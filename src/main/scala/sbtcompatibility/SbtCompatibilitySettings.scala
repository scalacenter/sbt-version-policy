package sbtcompatibility

import com.typesafe.tools.mima.plugin.MimaPlugin
import coursier.version.{ModuleMatcher, ModuleMatchers, VersionCompatibility}
import sbt.{Compile, Def}
import sbt.Keys._
import sbt.librarymanagement.CrossVersion
import lmcoursier.CoursierDependencyResolution
import lmcoursier.definitions.Reconciliation
import sbtcompatibility.internal.{DependencyCheck, Version}

import scala.util.Try

object SbtCompatibilitySettings {

  private val keys: SbtCompatibilityKeys = new SbtCompatibilityKeys {}
  import keys._

  def updateSettings = Def.settings(
    compatibilityCsrConfiguration := csrConfiguration.value
      // TODO Make that a method on CoursierConfiguration
      .withInterProjectDependencies(Vector.empty)
      .withFallbackDependencies(Vector.empty)
      .withSbtScalaOrganization(None)
      .withSbtScalaVersion(None)
      .withSbtScalaJars(Vector.empty)
      .withExcludeDependencies(Vector.empty)
      .withForceVersions(Vector.empty)
      .withReconciliation(Vector.empty),
    compatibilityDependencyResolution := CoursierDependencyResolution(compatibilityCsrConfiguration.value),
    compatibilityUpdateConfiguration := updateConfiguration.value,
    compatibilityUnresolvedWarningConfiguration := unresolvedWarningConfiguration.in(update).value,
    compatibilityScalaModuleInfo := scalaModuleInfo.value
  )

  def reconciliationBuildSettings = Def.settings(
    compatibilityCheckDirection := Direction.backward,
    compatibilityIgnoreSbtDefaultReconciliations := true,
    compatibilityUseCsrConfigReconciliations := true,
    compatibilityRules := Seq.empty,
    compatibilityIgnored := Seq.empty,
    compatibilityDefaultReconciliation := VersionCompatibility.PackVer
  )

  def reconciliationSettings = Def.settings(
    compatibilityDetailedReconciliations := {
      val sv = scalaVersion.value
      val sbv = scalaBinaryVersion.value
      compatibilityRules.value.map { mod =>
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
    compatibilityPreviousArtifactsFromMima := {
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
    compatibilityAutoPreviousArtifacts := {
      val projId = projectID.value
      compatibilityPreviousVersions.value.map { version =>
        projId.withRevision(version)
      }
    },
    compatibilityPreviousVersions := Version.latestCompatibleWith(sbt.Keys.version.value).toSeq,

    compatibilityPreviousArtifacts := compatibilityPreviousArtifactsFromMima.value
  )

  def findIssuesSettings = Def.settings(
    compatibilityFindDependencyIssues := {

      val log = streams.value.log
      val sv = scalaVersion.value
      val sbv = scalaBinaryVersion.value

      val compileReport = update.value.configuration(Compile).getOrElse {
        sys.error("Compile configuration not found in update report")
      }

      val depRes = compatibilityDependencyResolution.value
      val scalaModuleInf = compatibilityScalaModuleInfo.value
      val updateConfig = compatibilityUpdateConfiguration.value
      val warningConfig = compatibilityUnresolvedWarningConfiguration.value

      val reconciliations = {
        val csrConfig = csrConfiguration.value
        val useCsrConfigReconciliations = compatibilityUseCsrConfigReconciliations.value
        val ignoreSbtDefaultReconciliations = compatibilityIgnoreSbtDefaultReconciliations.value

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

        val ours = compatibilityDetailedReconciliations.value

        ours ++ fromCsrConfig0
      }
      val defaultReconciliation = compatibilityDefaultReconciliation.value

      val currentModules = DependencyCheck.modulesOf(compileReport, sv, sbv, log)

      val previousModuleIds = compatibilityPreviousArtifacts.value

      previousModuleIds.map { previousModuleId =>

        val report0 = DependencyCheck.report(
          currentModules,
          previousModuleId,
          reconciliations,
          defaultReconciliation,
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
    compatibilityReportDependencyIssues := {
      val log = streams.value.log
      val sv = scalaVersion.value
      val sbv = scalaBinaryVersion.value
      val direction = compatibilityCheckDirection.value
      val reports = compatibilityFindDependencyIssues.value

      if (reports.isEmpty)
        log.warn(s"No dependency check reports found (empty compatibilityPreviousArtifacts?).")

      val ignored = compatibilityIgnored.value
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
    compatibilityCheck := {
      MimaPlugin.autoImport.mimaReportBinaryIssues.value
      compatibilityReportDependencyIssues.value
    }
  )

}
