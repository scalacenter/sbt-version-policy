package sbtversionpolicy

import com.typesafe.tools.mima.plugin.{MimaPlugin, SbtMima}
import coursier.version.{ModuleMatcher, ModuleMatchers, Version, VersionCompatibility}
import sbt._
import sbt.Keys._
import sbt.librarymanagement.CrossVersion
import lmcoursier.CoursierDependencyResolution
import lmcoursier.definitions.Reconciliation
import sbtversionpolicy.internal.{DependencyCheck, MimaIssues}
import sbtversionpolicy.SbtVersionPolicyMima.autoImport._

import scala.util.Try

object SbtVersionPolicySettings {

  private val keys: SbtVersionPolicyKeys = new SbtVersionPolicyKeys {}
  import keys._
  import keys.versionPolicyInternal._

  def updateSettings = Def.settings(
    versionPolicyCsrConfiguration := csrConfiguration.value
      // TODO Make that a method on CoursierConfiguration
      .withInterProjectDependencies(Vector.empty)
      .withFallbackDependencies(Vector.empty)
      .withSbtScalaOrganization(None)
      .withSbtScalaVersion(None)
      .withSbtScalaJars(Vector.empty)
      .withExcludeDependencies(Vector.empty)
      .withForceVersions(Vector.empty)
      .withReconciliation(Vector.empty),
    versionPolicyDependencyResolution := CoursierDependencyResolution(versionPolicyCsrConfiguration.value),
    versionPolicyUpdateConfiguration := updateConfiguration.value,
    versionPolicyUnresolvedWarningConfiguration := (update / unresolvedWarningConfiguration).value,
    versionPolicyScalaModuleInfo := scalaModuleInfo.value
  )

  // Trying to mimick the current default behavior of evicted in sbt, that is
  // - assume scala libraries follow PVP,
  // - assume Java libraries follow semver.
  private def defaultSchemes = Seq(
    ("*" % "*" % "pvp").cross(CrossVersion.full),
    "*" %% "*" % "pvp",
    "*" % "*" % "early-semver"
  )

  def reconciliationGlobalSettings = Def.settings(
    versionPolicyCheckDirection := Direction.backward,
    versionPolicyIgnoreSbtDefaultReconciliations := true,
    versionPolicyUseCsrConfigReconciliations := true,
    versionPolicyDefaultDependencySchemes := defaultSchemes,
    versionPolicyIgnored := Seq.empty,
    versionPolicyDefaultScheme := None
  )

  def reconciliationSettings = Def.settings(
    versionPolicyFallbackReconciliations := {
      val sv = scalaVersion.value
      val sbv = scalaBinaryVersion.value
      val defaultReconciliationOpt = versionPolicyDefaultScheme.value
      val (fallbackRules, fallbackMatchers) = {
        val rules: Seq[ModuleID] = versionPolicyDefaultDependencySchemes.value
        defaultReconciliationOpt match {
          case None => (rules, Nil)
          case Some(default) => (Nil, Seq(ModuleMatchers.all -> default))
        }
      }
      fallbackRules.map { mod =>
        val rec = VersionCompatibility(mod.revision) match {
          case Some(r) => r
          case None => sys.error(s"Unrecognized reconciliation '${mod.revision}' in $mod")
        }
        val name = CrossVersion(mod.crossVersion, sv, sbv).fold(mod.name)(_(mod.name))
        val matchers = ModuleMatchers.only(mod.organization, name)
        (matchers, rec)
      } ++ fallbackMatchers
    },
    versionPolicyDetailedReconciliations := {
      val sv = scalaVersion.value
      val sbv = scalaBinaryVersion.value
      versionPolicyDependencySchemes.value.map { mod =>
        val rec = VersionCompatibility(mod.revision) match {
          case Some(r) => r
          case None => sys.error(s"Unrecognized reconciliation '${mod.revision}' in $mod")
        }
        val name = CrossVersion(mod.crossVersion, sv, sbv).fold(mod.name)(_(mod.name))
        val matchers = ModuleMatchers.only(mod.organization, name)
        (matchers, rec)
      }
    }
  )

  def previousArtifactsSettings = Def.settings(
    versionPolicyPreviousArtifactsFromMima := {
      import Ordering.Implicits._
      MimaPlugin.autoImport.mimaPreviousArtifacts.value
        .toVector
        .map { mod =>
          val splitVersion = mod.revision.split('.').map(s => Try(s.toInt).getOrElse(-1)).toSeq
          (splitVersion, mod)
        }
        .sortBy(_._1)
        .map(_._2)
    },

    versionPolicyPreviousArtifacts := versionPolicyPreviousArtifactsFromMima.value
  )

  def findIssuesSettings = Def.settings(
    versionPolicyFindDependencyIssues := {

      val log = streams.value.log
      val sv = scalaVersion.value
      val sbv = scalaBinaryVersion.value

      val compileReport = update.value.configuration(Compile).getOrElse {
        sys.error("Compile configuration not found in update report")
      }

      val compatibilityIntention =
        versionPolicyIntention.?.value
          .getOrElse(throw new MessageOnlyException("Please set the key versionPolicyIntention to declare the compatibility you want to check"))
      val depRes = versionPolicyDependencyResolution.value
      val scalaModuleInf = versionPolicyScalaModuleInfo.value
      val updateConfig = versionPolicyUpdateConfiguration.value
      val warningConfig = versionPolicyUnresolvedWarningConfiguration.value

      val reconciliations = {
        val csrConfig = csrConfiguration.value
        val useCsrConfigReconciliations = versionPolicyUseCsrConfigReconciliations.value
        val ignoreSbtDefaultReconciliations = versionPolicyIgnoreSbtDefaultReconciliations.value

        val fromCsrConfig =
          if (useCsrConfigReconciliations) {
            if (ignoreSbtDefaultReconciliations)
              csrConfig.reconciliation
                .filter {
                  val default = sbt.coursierint.LMCoursier.relaxedForAllModules.toSet
                  rule => !default(rule)
                }
            else
              csrConfig.reconciliation
          } else
            Nil

        val fromCsrConfig0 = fromCsrConfig.map {
          case (m, r) =>
            val matcher = ModuleMatchers(
              m.exclude.map(m => ModuleMatcher(m.organization.value, m.name.value, m.attributes)),
              m.include.map(m => ModuleMatcher(m.organization.value, m.name.value, m.attributes)),
              includeByDefault = m.includeByDefault
            )
            val compatibility = r match {
              case Reconciliation.Default => VersionCompatibility.Default
              case Reconciliation.Relaxed => VersionCompatibility.Always
              case Reconciliation.SemVer  => VersionCompatibility.EarlySemVer
              case Reconciliation.Strict  => VersionCompatibility.Strict
            }
            (matcher, compatibility)
        }

        val ours = versionPolicyDetailedReconciliations.value
        val fallback = versionPolicyFallbackReconciliations.value

        ours ++ fromCsrConfig0 ++ fallback
      }

      val previousModuleIds = versionPolicyPreviousArtifacts.value

      // Skip dependency check if no compatibility is intended
      if (compatibilityIntention == Compatibility.None) Nil else {

        val currentModules = DependencyCheck.modulesOf(compileReport, sv, sbv, log)

        previousModuleIds.map { previousModuleId =>

          val report0 = DependencyCheck.report(
            compatibilityIntention,
            currentModules,
            previousModuleId,
            reconciliations,
            VersionCompatibility.Strict,
            sv,
            sbv,
            depRes,
            scalaModuleInf,
            updateConfig,
            warningConfig,
            log
          )

          (previousModuleId, report0)
        }
      }
    },
    versionPolicyReportDependencyIssues := {
      val log = streams.value.log
      val sv = scalaVersion.value
      val sbv = scalaBinaryVersion.value
      val direction = versionPolicyCheckDirection.value
      val reports = versionPolicyFindDependencyIssues.value
      val intention =
        versionPolicyIntention.?.value
          .getOrElse(throw new MessageOnlyException("Please set the key versionPolicyIntention to declare the compatibility you want to check"))

      if (reports.isEmpty)
        log.warn(s"No dependency check reports found (empty versionPolicyPreviousArtifacts?).")

      val ignored = versionPolicyIgnored.value
        .map { orgName =>
          val mod = orgName % "foo"
          val name = CrossVersion(mod.crossVersion, sv, sbv).fold(mod.name)(_(mod.name))
          (mod.organization, name)
        }
        .toSet

      var anyError = false
      for ((previousModule, report) <- reports) {
        val (warnings, errors) = report.errors(direction, ignored)
        if (errors.nonEmpty) {
          anyError = true
          log.error(s"Incompatibilities with $previousModule with respect to compatibility intention ${intention}")
          for (error <- errors)
            log.error("  " + error)
        }
      }

      if (anyError)
        throw new Exception("Compatibility check failed (see messages above)")
    },
    versionCheck := Def.ifS((versionCheck / skip).toTask)(Def.task {
      ()
    })(Def.task {
      val intention =
        versionPolicyIntention.?.value
          .getOrElse(throw new MessageOnlyException("Please set the key versionPolicyIntention to declare the compatibility guarantees of this release"))
      val versionValue = version.value
      val s = streams.value
      val projectId = thisProject.value.id

      if (Compatibility.isValidVersion(intention, versionValue)) {
        s.log.info(s"$projectId/$versionValue is a valid version number with respect to the compatibility guarantees '$intention'")
      } else {
        val detail =
          if (intention == Compatibility.None) "You must increment the major version number (or the minor version number, if major version is 0) to publish a binary incompatible release."
          else "You must increment the minor version number to publish a source incompatible release."
        throw new MessageOnlyException(s"$projectId/$versionValue is not a valid version number. $detail")
      }
    }).value,
    versionPolicyCheck := Def.ifS((versionPolicyCheck / skip).toTask)(Def.task {
      ()
    })(Def.task {
      val ignored1 = versionPolicyMimaCheck.value
      val ignored2 = versionPolicyReportDependencyIssues.value
    }).value,
    versionPolicyForwardCompatibilityCheck := {
      import MimaPlugin.autoImport._
      val it = MimaIssues.forwardBinaryIssuesIterator.value
      it.foreach {
        case (moduleId, problems) =>
          SbtMima.reportModuleErrors(
            moduleId,
            problems._1,
            problems._2,
            true,
            mimaBinaryIssueFilters.value,
            mimaBackwardIssueFilters.value,
            mimaForwardIssueFilters.value,
            Keys.streams.value.log,
            name.value,
          )
      }
    },
    versionPolicyVersionCompatResult := {
      val ver = version.value
      val prevs = versionPolicyPreviousVersions.value
      if (prevs.nonEmpty) {
        val maxPrev = prevs.map(Version(_)).max.repr
        val compat = versionPolicyVersionCompatibility.value
        Compatibility(maxPrev, ver, compat)
      }
      else Compatibility.None
    },
    versionPolicyMimaCheck := (Def.taskDyn {
      import Compatibility._
      val compatibility =
        versionPolicyIntention.?.value
          .getOrElse(throw new MessageOnlyException("Please set the key versionPolicyIntention to declare the compatibility you want to check"))
      compatibility match {
        case BinaryCompatible => MimaPlugin.autoImport.mimaReportBinaryIssues
        case BinaryAndSourceCompatible =>
          Def.task {
            val ignored1 = versionPolicyForwardCompatibilityCheck.value
            val ignored2 = MimaPlugin.autoImport.mimaReportBinaryIssues.value
          }
        case None => Def.task { () } // skip mima if no compatibility is intented
      }
    }).value
  )

  def skipSettings = Seq(
    versionCheck / skip := (publish / skip).value,
    versionPolicyCheck / skip := (publish / skip).value
  )

  def schemesGlobalSettings = Seq(
    versionPolicyDependencySchemes := Seq.empty,
    versionScheme := Some("early-semver")
  )

}
