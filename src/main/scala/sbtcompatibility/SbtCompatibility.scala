package sbtcompatibility

import com.typesafe.tools.mima.plugin.MimaPlugin
import sbt._

object SbtCompatibility extends AutoPlugin {

  override def trigger = allRequirements
  override def requires = MimaPlugin

  object autoImport extends SbtCompatibilityKeys

  override def projectSettings =
    SbtCompatibilitySettings.updateSettings ++
      SbtCompatibilitySettings.reconciliationSettings ++
      SbtCompatibilitySettings.previousArtifactsSettings ++
      SbtCompatibilitySettings.findIssuesSettings
}
