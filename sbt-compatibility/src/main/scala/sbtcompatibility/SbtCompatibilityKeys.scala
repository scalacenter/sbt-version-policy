package sbtcompatibility

import coursier.version.VersionCompatibility
import sbt._
import sbt.librarymanagement.DependencyBuilders.OrganizationArtifactName
import sbtcompatibilityrules.SbtCompatibilityRulesPlugin

trait SbtCompatibilityKeys {
  final val compatibilityPreviousArtifacts      = taskKey[Seq[ModuleID]]("")
  final val compatibilityReportDependencyIssues = taskKey[Unit]("")
  final val compatibilityCheck                  = taskKey[Unit]("Runs both compatibilityReportDependencyIssues and mimaReportBinaryIssues")

  @deprecated("Use compatibilityRules instead", "0.0.8")
  final def compatibilityReconciliations         = SbtCompatibilityRulesPlugin.autoImport.compatibilityRules
  final val compatibilityIgnored                 = taskKey[Seq[OrganizationArtifactName]]("")
  final val compatibilityCheckDirection          = taskKey[Direction]("")

  final val compatibilityFindDependencyIssues = taskKey[Seq[(ModuleID, DependencyCheckReport)]]("")

  final val compatibilityDefaultRules          = taskKey[Seq[ModuleID]]("")
  final val compatibilityDefaultReconciliation = taskKey[Option[VersionCompatibility]]("")

  final val compatibilityInternal: SbtCompatibilityInternalKeys =
    new SbtCompatibilityInternalKeys {}
}
