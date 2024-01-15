package sbtversionpolicy

import sbt._

object SbtVersionPolicyDynverPlugin extends AutoPlugin with SbtVersionPolicyKeys {

  override def trigger = allRequirements
  
  override def requires = SbtVersionPolicyPlugin

  override def buildSettings: Seq[Setting[_]] = Seq(
    versionPolicyIgnoredInternalDependencyVersions := Some("^\\d+\\.\\d+\\.\\d+\\+\\d+".r)
  )

}
