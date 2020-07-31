package sbtcompatibility

import coursier.version.VersionCompatibility
import sbt._
import sbt.librarymanagement.DependencyBuilders.OrganizationArtifactName
import sbtcompatibilityrules.SbtCompatibilityRulesPlugin

trait SbtCompatibilityKeys {
  final val compatibilityPreviousArtifacts      = taskKey[Seq[ModuleID]]("")
  final val compatibilityReportDependencyIssues = taskKey[Unit]("")
  final val compatibilityCheck                  = taskKey[Unit]("Runs both compatibilityReportDependencyIssues and mimaReportBinaryIssues")
  final val forwardCompatibilityCheck           = taskKey[Unit]("")

  final val compatibilityIgnored                 = taskKey[Seq[OrganizationArtifactName]]("")
  final val compatibilityCheckDirection          = taskKey[Direction]("")

  final val compatibilityFindDependencyIssues = taskKey[Seq[(ModuleID, DependencyCheckReport)]]("")

  final val compatibilityDefaultRules          = taskKey[Seq[ModuleID]]("")
  final val compatibilityDefaultReconciliation = taskKey[Option[VersionCompatibility]]("")

  final val compatibilityInternal: SbtCompatibilityInternalKeys =
    new SbtCompatibilityInternalKeys {}
}
