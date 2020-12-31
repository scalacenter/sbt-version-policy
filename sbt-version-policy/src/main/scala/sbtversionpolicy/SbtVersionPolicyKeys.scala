package sbtversionpolicy

import coursier.version.VersionCompatibility
import sbt._
import sbt.librarymanagement.DependencyBuilders.OrganizationArtifactName

trait SbtVersionPolicyKeys {
  final val versionPolicyPreviousArtifacts      = taskKey[Seq[ModuleID]]("")
  final val versionPolicyReportDependencyIssues = taskKey[Unit]("Check for removed or updated dependencies in an incompatible way.")
  final val versionPolicyCheck                  = taskKey[Unit]("Runs both versionPolicyReportDependencyIssues and versionPolicyMimaCheck")
  final val versionPolicyMimaCheck              = taskKey[Unit]("Runs Mima to check backward or forward compatibility depending on the version change.")
  final val versionPolicyForwardCompatibilityCheck = taskKey[Unit]("Report forward binary compatible issues from Mima.")
  final val versionPolicyFindDependencyIssues   = taskKey[Seq[(ModuleID, DependencyCheckReport)]]("Compatibility issues in the library dependencies.")

  final val versionPolicyIgnored                = settingKey[Seq[OrganizationArtifactName]]("Exclude these dependencies from versionPolicyReportDependencyIssues.")
  final val versionPolicyCheckDirection         = settingKey[Direction]("Direction to check the version compatibility. Default: Direction.backward.")
  final val versionPolicyDefaultRules           = settingKey[Seq[ModuleID]]("Fallback rules used to evaluate version policy issues in the library dependency.")
  final val versionPolicyDefaultReconciliation  = settingKey[Option[VersionCompatibility]]("Fallback reconciliation used to evaluate version policy issues in the library dependency.")

  // TODO Remove/deprecate when sbt 1.5.0 is out and use libraryDependencySchemes instead
  final val versionPolicyDependencyRules = settingKey[Seq[ModuleID]]("""Version policy rules for the library dependencies (e.g. "org.scala-lang" % "scala-compiler" % "strict")""")

  final val versionPolicyInternal: SbtVersionPolicyInternalKeys =
    new SbtVersionPolicyInternalKeys {}
}
