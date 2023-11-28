package sbtversionpolicy.internal

import coursier.version.{ModuleMatchers, VersionCompatibility}
import sbt.Compile
import sbt.librarymanagement.{ConfigurationReport, CrossVersion, ModuleID}
import sbt.util.Logger
import sbt.librarymanagement.{DependencyResolution, ScalaModuleInfo, UnresolvedWarningConfiguration, UpdateConfiguration}
import sbtversionpolicy.DependencyCheckReport

object DependencyCheck {

  private[sbtversionpolicy] def modulesOf(
    report: ConfigurationReport,
    excludedModules: Set[(String, String)],
    scalaVersion: String,
    scalaBinaryVersion: String,
    moduleToVersion: PartialFunction[ModuleID, String],
    log: Logger
  ): Map[(String, String), String] =
    report
      .modules
      .filter(!_.evicted)
      .map(_.module)
      .filter(module => !excludedModules.contains(module.organization -> module.name))
      .map { mod =>
        val name = CrossVersion(mod.crossVersion, scalaVersion, scalaBinaryVersion)
          .fold(mod.name)(_(mod.name))
        // TODO Check mod.explicitArtifacts too?
        (mod.organization, name) -> moduleToVersion.applyOrElse(mod, (m: ModuleID) => m.revision)
      }
      .groupBy(_._1)
      .map {
        case (orgName @ (org, name), grouped) =>
          val versions = grouped.map(_._2).distinct
          if (versions.lengthCompare(1) > 0)
            log.warn(s"Found several versions for $org:$name: ${versions.mkString(", ")}")
          (orgName, versions.head)
      }

  private[sbtversionpolicy] def report(
    excludedModules: Set[(String, String)],
    currentDependencies: Map[(String, String), String],
    previousModuleId: ModuleID,
    reconciliations: Seq[(ModuleMatchers, VersionCompatibility)],
    defaultReconciliation: VersionCompatibility,
    sv: String,
    sbv: String,
    depRes: DependencyResolution,
    scalaModuleInf: Option[ScalaModuleInfo],
    updateConfig: UpdateConfiguration,
    warningConfig: UnresolvedWarningConfiguration,
    moduleToVersion: PartialFunction[ModuleID, String],
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
    val previousDependencies = DependencyCheck.modulesOf(previousCompileReport, excludedModules, sv, sbv, moduleToVersion, log)
      .filterKeys {
        case (org, name) =>
          org != previousModuleId0.organization || name != previousModuleId0.name
      }

    DependencyCheckReport(
      currentDependencies,
      previousDependencies,
      reconciliations,
      defaultReconciliation
    )
  }
}
