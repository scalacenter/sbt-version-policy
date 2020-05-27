package sbtcompatibility

import com.typesafe.tools.mima.plugin.MimaPlugin
import sbt.{AutoPlugin, Def, Keys}
import sbtcompatibility.version.Version

object SbtCompatibilityMima extends AutoPlugin {

  override def trigger = allRequirements
  override def requires = MimaPlugin

  override def projectSettings = Def.settings(
    MimaPlugin.autoImport.mimaPreviousArtifacts := {
      val projId = Keys.projectID.value
      val versions = Version.latestCompatibleWith(sbt.Keys.version.value).toSet
      versions.map(version => projId.withRevision(version))
    }
  )

}
