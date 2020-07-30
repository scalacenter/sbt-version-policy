package sbtcompatibility

import com.typesafe.tools.mima.plugin.MimaPlugin
import sbt._

object SbtCompatibilityPlugin extends AutoPlugin {

  override def trigger = allRequirements
  override def requires = MimaPlugin

  private def mimaIgnoreVersion(version: String): Seq[Def.Setting[_]] =
    Def.settings(
      MimaPlugin.autoImport.mimaPreviousArtifacts := {
        val value = MimaPlugin.autoImport.mimaPreviousArtifacts.value
        value.filter(_.revision != version)
      }
    )

  object autoImport extends SbtCompatibilityKeys {
    val VersionCompatibility = coursier.version.VersionCompatibility
    def compatibilityIgnoreVersion(version: String): Seq[Def.Setting[_]] =
      SbtCompatibilityPlugin.mimaIgnoreVersion(version)
  }

  override def globalSettings =
    SbtCompatibilitySettings.reconciliationGlobalSettings

  override def projectSettings =
    SbtCompatibilitySettings.updateSettings ++
      SbtCompatibilitySettings.reconciliationSettings ++
      SbtCompatibilitySettings.previousArtifactsSettings ++
      SbtCompatibilitySettings.findIssuesSettings
}
