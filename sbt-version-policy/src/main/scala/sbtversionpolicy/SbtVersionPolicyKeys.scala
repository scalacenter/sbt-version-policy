package sbtversionpolicy

import coursier.version.VersionCompatibility
import sbt._
import sbt.librarymanagement.DependencyBuilders.OrganizationArtifactName

trait SbtVersionPolicyKeys {
  final val versionPolicyIntention              = settingKey[Compatibility]("Compatibility intentions for the next release.")
  final val versionPolicyPreviousArtifacts      = taskKey[Seq[ModuleID]]("")
  final val versionPolicyReportDependencyIssues = taskKey[Unit]("Check for removed or updated dependencies in an incompatible way.")
  final val versionPolicyCheck                  = taskKey[Unit]("Runs both versionPolicyReportDependencyIssues and versionPolicyMimaCheck")
  final val versionPolicyMimaCheck              = taskKey[Unit]("Runs Mima to check backward or forward compatibility depending on the intended change defined via versionPolicyIntention.")
  final val versionPolicyForwardCompatibilityCheck = taskKey[Unit]("Report forward binary compatible issues from Mima.")
  final val versionPolicyFindDependencyIssues   = taskKey[Seq[(ModuleID, DependencyCheckReport)]]("Compatibility issues in the library dependencies.")
  final val versionCheck                        = taskKey[Unit]("Checks that the version is consistent with the intended compatibility level defined via versionPolicyIntention")

  final val versionPolicyIgnored        = settingKey[Seq[OrganizationArtifactName]]("Exclude these dependencies from versionPolicyReportDependencyIssues.")
  final val versionPolicyCheckDirection = settingKey[Direction]("Direction to check the version compatibility. Default: Direction.backward.")

  @deprecated("Use libraryDependencySchemes instead", "1.1.0")
  final val versionPolicyDependencySchemes        = settingKey[Seq[ModuleID]]("""Versioning schemes for the library dependencies (e.g. "org.scala-lang" % "scala-compiler" % "strict")""")
  final val versionPolicyDefaultScheme            = settingKey[Option[VersionCompatibility]]("Fallback reconciliation used to evaluate version policy issues in the library dependency.")
  final val versionPolicyDefaultDependencySchemes = settingKey[Seq[ModuleID]]("Fallback rules used to evaluate version policy issues in the library dependency.")

  final val versionPolicyInternal: SbtVersionPolicyInternalKeys =
    new SbtVersionPolicyInternalKeys {}
}
