package sbtversionpolicyrules

import sbt._

object SbtVersionPolicyRulesPlugin extends AutoPlugin {

  override def trigger = allRequirements

  object autoImport {
    val versionPolicyDependencyRules = settingKey[Seq[ModuleID]]("""Version policy rules for the library dependencies (e.g. "org.scala-lang" % "scala-compiler" % "strict")""")
  }
  import autoImport._

  override def globalSettings = Def.settings(
    versionPolicyDependencyRules := Seq.empty
  )
}
