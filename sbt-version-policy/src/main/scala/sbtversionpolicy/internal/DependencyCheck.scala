package sbtversionpolicy.internal

import coursier.version.{ModuleMatchers, VersionCompatibility}
import sbt.Compile
import sbt.librarymanagement.{ConfigurationReport, CrossVersion, ModuleID}
import sbt.util.Logger
import sbt.librarymanagement.{DependencyResolution, ScalaModuleInfo, UnresolvedWarningConfiguration, UpdateConfiguration}
import sbtversionpolicy.DependencyCheckReport

object DependencyCheck {

  def modulesOf(
    report: ConfigurationReport,
    scalaVersion: String,
    scalaBinaryVersion: String,
    log: Logger
  ): Map[(String, String), String] =
    report
      .modules
      .filter(!_.evicted)
      .map(_.module)
      .map { mod =>
        val name = CrossVersion(mod.crossVersion, scalaVersion, scalaBinaryVersion)
          .fold(mod.name)(_(mod.name))
        // TODO Check mod.explicitArtifacts too?
        (mod.organization, name) -> mod.revision
      }
      .groupBy(_._1)
      .map {
        case (orgName @ (org, name), grouped) =>
          val versions = grouped.map(_._2).distinct
          if (versions.lengthCompare(1) > 0)
            log.warn(s"Found several versions for $org:$name: ${versions.mkString(", ")}")
          (orgName, versions.head)
      }

  def report(
    currentModules: Map[(String, String), String],
    previousModuleId: ModuleID,
    reconciliations: Seq[(ModuleMatchers, VersionCompatibility)],
    defaultReconciliation: VersionCompatibility,
    sv: String,
    sbv: String,
    depRes: DependencyResolution,
    scalaModuleInf: Option[ScalaModuleInfo],
    updateConfig: UpdateConfiguration,
    warningConfig: UnresolvedWarningConfiguration,
    log: Logger
  ): DependencyCheckReport = {

    val previousModuleId0 = previousModuleId
      .withName(CrossVersion(previousModuleId.crossVersion, sv, sbv).fold(previousModuleId.name)(_(previousModuleId.name)))
      .withCrossVersion(CrossVersion.disabled)
      .withExplicitArtifacts(Vector.empty)

    val mod = depRes.moduleDescriptor(
      ModuleID("dummy-org", "dummy-name", "1.0"),
      Vector(previousModuleId0),
      scalaModuleInf
    )

    val previousReport = depRes.update(mod, updateConfig, warningConfig, log)
      .fold(thing => throw thing.resolveException, identity)
    val previousCompileReport = previousReport.configuration(Compile).getOrElse {
      sys.error(s"Compile configuration not found in previous update report $previousReport")
    }
    val previous = DependencyCheck.modulesOf(previousCompileReport, sv, sbv, log)
      .filterKeys {
        case (org, name) =>
          org != previousModuleId0.organization || name != previousModuleId0.name
      }
      .toMap

    DependencyCheckReport(
      currentModules,
      previous,
      reconciliations,
      defaultReconciliation
    )
  }
}
