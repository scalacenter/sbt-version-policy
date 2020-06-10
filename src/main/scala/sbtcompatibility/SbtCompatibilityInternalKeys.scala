package sbtcompatibility

import coursier.version.{ModuleMatchers, VersionCompatibility}
import lmcoursier.CoursierConfiguration
import sbt._
import sbt.librarymanagement.{DependencyResolution, ScalaModuleInfo, UpdateConfiguration, UnresolvedWarningConfiguration}

trait SbtCompatibilityInternalKeys {
  final val compatibilityCsrConfiguration               = taskKey[CoursierConfiguration]("CoursierConfiguration instance to use to fetch previous versions dependencies")
  final val compatibilityDependencyResolution           = taskKey[DependencyResolution]("DependencyResolution instance to use to fetch previous versions dependencies")
  final val compatibilityUpdateConfiguration            = taskKey[UpdateConfiguration]("")
  final val compatibilityUnresolvedWarningConfiguration = taskKey[UnresolvedWarningConfiguration]("")
  final val compatibilityScalaModuleInfo                = taskKey[Option[ScalaModuleInfo]]("")

  final val compatibilityIgnoreSbtDefaultReconciliations = taskKey[Boolean]("")
  final val compatibilityUseCsrConfigReconciliations     = taskKey[Boolean]("")

  final val compatibilityAutoPreviousArtifacts     = taskKey[Seq[ModuleID]]("")
  final val compatibilityPreviousArtifactsFromMima = taskKey[Seq[ModuleID]]("")
  final val compatibilityPreviousVersions          = taskKey[Seq[String]]("")

  final val compatibilityDetailedReconciliations = taskKey[Seq[(ModuleMatchers, VersionCompatibility)]]("")
  final val compatibilityFallbackReconciliations = taskKey[Seq[(ModuleMatchers, VersionCompatibility)]]("")
}
