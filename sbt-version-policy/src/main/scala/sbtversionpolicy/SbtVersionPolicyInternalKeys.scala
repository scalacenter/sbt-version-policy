package sbtversionpolicy

import coursier.version.{ModuleMatchers, VersionCompatibility}
import lmcoursier.CoursierConfiguration
import sbt._
import sbt.librarymanagement.{DependencyResolution, ScalaModuleInfo, UpdateConfiguration, UnresolvedWarningConfiguration}

trait SbtVersionPolicyInternalKeys {
  @transient
  final val versionPolicyCsrConfiguration               = taskKey[CoursierConfiguration]("CoursierConfiguration instance to use to fetch previous versions dependencies")
  @transient
  final val versionPolicyDependencyResolution           = taskKey[DependencyResolution]("DependencyResolution instance to use to fetch previous versions dependencies")
  @transient
  final val versionPolicyUpdateConfiguration            = taskKey[UpdateConfiguration]("")
  @transient
  final val versionPolicyUnresolvedWarningConfiguration = taskKey[UnresolvedWarningConfiguration]("")
  @transient
  final val versionPolicyScalaModuleInfo                = taskKey[Option[ScalaModuleInfo]]("")

  final val versionPolicyIgnoreSbtDefaultReconciliations = settingKey[Boolean]("")
  final val versionPolicyUseCsrConfigReconciliations     = settingKey[Boolean]("")

  @transient
  final val versionPolicyPreviousArtifactsFromMima = taskKey[Seq[ModuleID]]("")

  @transient
  final val versionPolicyDetailedReconciliations = taskKey[Seq[(ModuleMatchers, VersionCompatibility)]]("")
  @transient
  final val versionPolicyFallbackReconciliations = taskKey[Seq[(ModuleMatchers, VersionCompatibility)]]("")

  final val versionPolicyVersionCompatibility = settingKey[VersionCompatibility]("VersionCompatibility used to determine compatibility.")
  final val versionPolicyVersionCompatResult  = taskKey[Compatibility]("Calculated level of compatibility required according to the current project version and the versioning scheme.")

  @transient
  final def versionPolicyCollectCompatibilityReports = TaskKey[CompatibilityReport]("versionPolicyCollectCompatibilityReports", "Collect compatibility reports for the export task.")
}
