package sbtcompatibility

import coursier.version.{ModuleMatchers, VersionCompatibility}
import sbt._
import sbt.librarymanagement.DependencyBuilders.OrganizationArtifactName

trait SbtCompatibilityKeys {
  final val compatibilityPreviousArtifacts      = taskKey[Seq[ModuleID]]("")
  final val compatibilityReportDependencyIssues = taskKey[Unit]("")
  final val compatibilityCheck                  = taskKey[Unit]("Runs both compatibilityReportDependencyIssues and mimaReportBinaryIssues")

  final val compatibilityRules                   = taskKey[Seq[ModuleID]]("")
  @deprecated("Use compatibilityRules instead", "0.0.8")
  final def compatibilityReconciliations         = compatibilityRules
  final val compatibilityIgnored                 = taskKey[Seq[OrganizationArtifactName]]("")
  final val compatibilityDetailedReconciliations = taskKey[Seq[(ModuleMatchers, VersionCompatibility)]]("")
  final val compatibilityCheckDirection          = taskKey[Direction]("")

  final val compatibilityFindDependencyIssues = taskKey[Seq[(ModuleID, DependencyCheckReport)]]("")

  final val compatibilityDefaultReconciliation = taskKey[VersionCompatibility]("")

  final val compatibilityInternal: SbtCompatibilityInternalKeys =
    new SbtCompatibilityInternalKeys {}
}
