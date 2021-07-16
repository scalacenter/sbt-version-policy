package sbtversionpolicy

import com.typesafe.tools.mima.plugin.MimaPlugin
import sbt._

object SbtVersionPolicyPlugin extends AutoPlugin {

  override def trigger = allRequirements
  override def requires = MimaPlugin

  private def mimaIgnoreVersion(version: String): Seq[Def.Setting[_]] =
    Def.settings(
      MimaPlugin.autoImport.mimaPreviousArtifacts := {
        val value = MimaPlugin.autoImport.mimaPreviousArtifacts.value
        value.filter(_.revision != version)
      }
    )

  object autoImport extends SbtVersionPolicyKeys {
    val VersionCompatibility = coursier.version.VersionCompatibility
    val Compatibility = sbtversionpolicy.Compatibility
    type Compatibility = sbtversionpolicy.Compatibility
    def versionPolicyIgnoreVersion(version: String): Seq[Def.Setting[_]] =
      SbtVersionPolicyPlugin.mimaIgnoreVersion(version)
  }

  override def globalSettings =
    SbtVersionPolicySettings.reconciliationGlobalSettings ++
      SbtVersionPolicySettings.schemesGlobalSettings

  override def projectSettings =
    SbtVersionPolicySettings.updateSettings ++
      SbtVersionPolicySettings.reconciliationSettings ++
      SbtVersionPolicySettings.previousArtifactsSettings ++
      SbtVersionPolicySettings.findIssuesSettings ++
      SbtVersionPolicySettings.skipSettings

}
