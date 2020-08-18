package sbtversionpolicy

import com.typesafe.tools.mima.plugin.{MimaPlugin, SbtMima}
import coursier.version.{ModuleMatcher, ModuleMatchers, Version, VersionCompatibility}
import sbt._
import sbt.Keys._
import sbt.librarymanagement.CrossVersion
import lmcoursier.CoursierDependencyResolution
import lmcoursier.definitions.Reconciliation
import sbtversionpolicy.internal.{DependencyCheck, MimaIssues}
import sbtversionpolicyrules.SbtVersionPolicyRulesPlugin
import SbtVersionPolicyRulesPlugin.autoImport.versionPolicyDependencyRules
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
    versionPolicyUnresolvedWarningConfiguration := unresolvedWarningConfiguration.in(update).value,
    versionPolicyScalaModuleInfo := scalaModuleInfo.value
  )

  // Trying to mimick the current default behavior of evicted in sbt, that is
  // - assume scala libraries follow PVP,
  // - assume Java libraries follow semver.
  private def defaultRules = Seq(
    ("*" % "*" % "pvp").cross(CrossVersion.full),
    "*" %% "*" % "pvp",
    "*" % "*" % "semver"
  )

  def reconciliationGlobalSettings = Def.settings(
    versionPolicyCheckDirection := Direction.backward,
    versionPolicyIgnoreSbtDefaultReconciliations := true,
    versionPolicyUseCsrConfigReconciliations := true,
    versionPolicyDefaultRules := defaultRules,
    versionPolicyIgnored := Seq.empty,
    versionPolicyDefaultReconciliation := None
  )

  def reconciliationSettings = Def.settings(
    versionPolicyFallbackReconciliations := {
      val sv = scalaVersion.value
      val sbv = scalaBinaryVersion.value
      val defaultReconciliationOpt = versionPolicyDefaultReconciliation.value
      val (fallbackRules, fallbackMatchers) = {
        val rules: Seq[ModuleID] = versionPolicyDefaultRules.value
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
      versionPolicyDependencyRules.value.map { mod =>
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
    versionPolicyAutoPreviousArtifacts := {
      val projId = projectID.value
      versionPolicyPreviousVersions.value.map { version =>
        projId.withRevision(version)
      }
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
              case Reconciliation.SemVer  => VersionCompatibility.SemVer
              case Reconciliation.Strict  => VersionCompatibility.Strict
            }
            (matcher, compatibility)
        }

        val ours = versionPolicyDetailedReconciliations.value
        val fallback = versionPolicyFallbackReconciliations.value

        ours ++ fromCsrConfig0 ++ fallback
      }

      val currentModules = DependencyCheck.modulesOf(compileReport, sv, sbv, log)

      val previousModuleIds = versionPolicyPreviousArtifacts.value

      previousModuleIds.map { previousModuleId =>

        val report0 = DependencyCheck.report(
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
    },
    versionPolicyReportDependencyIssues := {
      val log = streams.value.log
      val sv = scalaVersion.value
      val sbv = scalaBinaryVersion.value
      val direction = versionPolicyCheckDirection.value
      val reports = versionPolicyFindDependencyIssues.value

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
          log.error(s"Incompatibilities with $previousModule")
          for (error <- errors)
            log.error("  " + error)
        }
      }

      if (anyError)
        throw new Exception("Compatibility check failed (see messages above)")
    },
    versionPolicyCheck := {
      versionPolicyMimaCheck.value
      versionPolicyReportDependencyIssues.value
    },
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
            new MimaIssues.SbtLogger(Keys.streams.value.log),
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
        VersionCompatResult(maxPrev, ver, compat)
      }
      else VersionCompatResult.None
    },
    versionPolicyMimaCheck := (Def.taskDyn {
      import VersionCompatResult._
      val r = versionPolicyVersionCompatResult.value
      r match {
        case BinaryCompatible => MimaPlugin.autoImport.mimaReportBinaryIssues
        case BinaryAndSourceCompatible =>
          Def.task {
            versionPolicyForwardCompatibilityCheck.value
            MimaPlugin.autoImport.mimaReportBinaryIssues.value
          }
        case _ => Def.task { () } // skip mima for major upgrade + dev
      }
    }).value
  )

}
