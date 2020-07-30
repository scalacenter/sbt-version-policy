package sbtcompatibility

import coursier.version.VersionCompatibility
import sbt._
import sbt.librarymanagement.DependencyBuilders.OrganizationArtifactName
import sbtcompatibilityrules.SbtCompatibilityRulesPlugin

trait SbtCompatibilityKeys {
  final val compatibilityPreviousArtifacts      = taskKey[Seq[ModuleID]]("")
  final val compatibilityReportDependencyIssues = taskKey[Unit]("Check for removed or updated dependencies in an incompatible way.")
  final val compatibilityCheck                  = taskKey[Unit]("Runs both compatibilityReportDependencyIssues and mimaReportBinaryIssues")
  final val forwardCompatibilityCheck           = taskKey[Unit]("Report forward binary compatible issues from Mima.")

  final val compatibilityIgnored                 = taskKey[Seq[OrganizationArtifactName]]("Exclude these dependencies from compatibilityReportDependencyIssues.")
  final val compatibilityCheckDirection          = taskKey[Direction]("Direction to check the compatibibility. Default: Direction.backward.")

  final val compatibilityFindDependencyIssues = taskKey[Seq[(ModuleID, DependencyCheckReport)]]("Compatibility issues in the library dependencies.")

  final val compatibilityDefaultRules          = taskKey[Seq[ModuleID]]("Fallback rules used to evalute compatibility issues in the library dependency.")
  final val compatibilityDefaultReconciliation = taskKey[Option[VersionCompatibility]]("Fallback reconciliation used to evalute compatibility issues in the library dependency.")

  final val compatibilityInternal: SbtCompatibilityInternalKeys =
    new SbtCompatibilityInternalKeys {}
}
