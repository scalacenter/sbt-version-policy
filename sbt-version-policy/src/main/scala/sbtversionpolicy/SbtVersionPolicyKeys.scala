package sbtversionpolicy

import com.typesafe.tools.mima.core.Problem
import coursier.version.VersionCompatibility
import sbt.*
import sbt.librarymanagement.DependencyBuilders.OrganizationArtifactName

import scala.util.matching.Regex

trait SbtVersionPolicyKeys {
  final val versionPolicyIntention              = settingKey[Compatibility]("Compatibility intentions for the next release.")
  final val versionPolicyPreviousArtifacts      = taskKey[Seq[ModuleID]]("Previous released artifacts used to test compatibility.")
  final val versionPolicyReportDependencyIssues = taskKey[Unit]("Check for removed or updated dependencies in an incompatible way.")
  final val versionPolicyCheck                  = taskKey[Unit]("Runs both versionPolicyReportDependencyIssues and versionPolicyMimaCheck")
  final val versionPolicyMimaCheck              = taskKey[Unit]("Runs Mima to check backward or forward compatibility depending on the intended change defined via versionPolicyIntention.")
  final val versionPolicyFindDependencyIssues   = taskKey[Seq[(ModuleID, DependencyCheckReport)]]("Compatibility issues in the library dependencies.")
  final val versionPolicyFindMimaIssues         = taskKey[Seq[(ModuleID, Seq[(IncompatibilityType, Problem)])]]("Binary or source compatibility issues over the previously released artifacts.")
  final val versionPolicyFindIssues             = taskKey[Seq[(ModuleID, (DependencyCheckReport, Seq[(IncompatibilityType, Problem)]))]]("Find both dependency issues and Mima issues.")
  final val versionPolicyAssessCompatibility    = taskKey[Seq[(ModuleID, Compatibility)]]("Assess the compatibility level of the project compared to its previous releases.")
  final def versionPolicyExportCompatibilityReport = TaskKey[Unit]("versionPolicyExportCompatibilityReport", "Export the compatibility report into a JSON file.")
  final def versionPolicyCompatibilityReportPath = SettingKey[File]("versionPolicyCompatibilityReportPath", s"Path of the compatibility report (used by ${versionPolicyExportCompatibilityReport.key.label}).")
  final val versionCheck                        = taskKey[Unit]("Checks that the version is consistent with the intended compatibility level defined via versionPolicyIntention")

  final val versionPolicyIgnored        = settingKey[Seq[OrganizationArtifactName]]("Exclude these dependencies from versionPolicyReportDependencyIssues.")
  final val versionPolicyIgnoredInternalDependencyVersions = settingKey[Option[Regex]]("Exclude dependencies to projects of the current build whose version matches this regular expression.")

  final val versionPolicyModuleVersionExtractor = settingKey[PartialFunction[ModuleID, String]]("Parse version number from ModuleId. Defaults to `_.revision`")

  final val versionPolicyDefaultScheme            = settingKey[Option[VersionCompatibility]]("Fallback reconciliation used to evaluate version policy issues in the library dependency.")
  final val versionPolicyDefaultDependencySchemes = settingKey[Seq[ModuleID]]("Fallback rules used to evaluate version policy issues in the library dependency.")

  final val versionPolicyInternal: SbtVersionPolicyInternalKeys =
    new SbtVersionPolicyInternalKeys {}
}
