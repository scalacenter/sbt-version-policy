package sbtversionpolicy

import coursier.version.{ModuleMatchers, VersionCompatibility}
import lmcoursier.CoursierConfiguration
import sbt._
import sbt.librarymanagement.{DependencyResolution, ScalaModuleInfo, UpdateConfiguration, UnresolvedWarningConfiguration}

trait SbtVersionPolicyInternalKeys {
  final val versionPolicyCsrConfiguration               = taskKey[CoursierConfiguration]("CoursierConfiguration instance to use to fetch previous versions dependencies")
  final val versionPolicyDependencyResolution           = taskKey[DependencyResolution]("DependencyResolution instance to use to fetch previous versions dependencies")
  final val versionPolicyUpdateConfiguration            = taskKey[UpdateConfiguration]("")
  final val versionPolicyUnresolvedWarningConfiguration = taskKey[UnresolvedWarningConfiguration]("")
  final val versionPolicyScalaModuleInfo                = taskKey[Option[ScalaModuleInfo]]("")

  final val versionPolicyIgnoreSbtDefaultReconciliations = settingKey[Boolean]("")
  final val versionPolicyUseCsrConfigReconciliations     = settingKey[Boolean]("")

  final val versionPolicyAutoPreviousArtifacts     = taskKey[Seq[ModuleID]]("")
  final val versionPolicyPreviousArtifactsFromMima = taskKey[Seq[ModuleID]]("")

  final val versionPolicyDetailedReconciliations = taskKey[Seq[(ModuleMatchers, VersionCompatibility)]]("")
  final val versionPolicyFallbackReconciliations = taskKey[Seq[(ModuleMatchers, VersionCompatibility)]]("")

  final val versionPolicyVersionCompatibility = settingKey[VersionCompatibility]("VersionCompatibility used to determine compatibility.")
  final val versionPolicyVersionCompatResult = taskKey[VersionCompatResult]("Calculated version change compared to the previous version.")
}
