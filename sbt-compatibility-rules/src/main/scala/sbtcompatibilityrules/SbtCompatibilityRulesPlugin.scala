package sbtcompatibilityrules

import sbt._

object SbtCompatibilityRulesPlugin extends AutoPlugin {

  override def trigger = allRequirements

  object autoImport {
    val compatibilityRules = taskKey[Seq[ModuleID]]("")
  }
  import autoImport._

  override def buildSettings = Def.settings(
    compatibilityRules := Seq.empty
  )
}
