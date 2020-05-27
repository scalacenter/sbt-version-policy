package sbtcompatibility

import com.typesafe.tools.mima.plugin.MimaPlugin
import sbt.{Compile, Def}
import sbt.Keys._
import lmcoursier.CoursierDependencyResolution
import lmcoursier.definitions.Reconciliation
import sbtcompatibility.internal.DependencyCheck
import sbtcompatibility.version.Version

import scala.util.Try

object SbtCompatibilitySettings {

  private val keys: SbtCompatibilityKeys = new SbtCompatibilityKeys {}
  import keys._

  def updateSettings = Def.settings(
    compatibilityCsrConfiguration := csrConfiguration.value
      .withInterProjectDependencies(Vector.empty)
      .withSbtScalaOrganization(None)
      .withSbtScalaVersion(None)
      .withSbtScalaJars(Vector.empty)
      .withExcludeDependencies(Vector.empty),
    compatibilityDependencyResolution := CoursierDependencyResolution(compatibilityCsrConfiguration.value),
    compatibilityUpdateConfiguration := updateConfiguration.value,
    compatibilityUnresolvedWarningConfiguration := unresolvedWarningConfiguration.in(update).value,
    compatibilityScalaModuleInfo := scalaModuleInfo.value
  )

  def reconciliationSettings = Def.settings(
    compatibilityCheckDirection := Direction.backward,
    compatibilityIgnoreSbtDefaultReconciliations := true,
    compatibilityUseCsrConfigReconciliations := true,
    compatibilityReconciliations := Seq.empty,
    compatibilityDefaultReconciliation := Reconciliation.SemVer
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
        val csrConfig = compatibilityCsrConfiguration.value
        val useCsrConfigReconciliations = compatibilityUseCsrConfigReconciliations.value
        val ignoreSbtDefaultReconciliations = compatibilityIgnoreSbtDefaultReconciliations.value

        val fromCsrConfig =
          if (useCsrConfigReconciliations) {
            if (ignoreSbtDefaultReconciliations)
              csrConfig.reconciliation.filter {
                val default = sbt.coursierint.LMCoursier.relaxedForAllModules.toSet
                rule => !default(rule)
              }
            else
              csrConfig.reconciliation
          } else
            Nil

        val ours = compatibilityReconciliations.value

        ours ++ fromCsrConfig
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
      val direction = compatibilityCheckDirection.value
      val reports = compatibilityFindDependencyIssues.value

      var anyError = false
      for ((previousModule, report) <- reports) {
        val errors = report.errors(direction)
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
