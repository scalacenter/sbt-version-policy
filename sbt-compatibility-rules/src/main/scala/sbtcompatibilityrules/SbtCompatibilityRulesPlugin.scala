package sbtcompatibilityrules

import sbt._

object SbtCompatibilityRulesPlugin extends AutoPlugin {

  override def trigger = allRequirements

  object autoImport {
    val compatibilityDependencyRules = taskKey[Seq[ModuleID]]("""Compatibility rules for the library dependencies (e.g. "org.scala-lang" % "scala-compiler" % "strict")""")
  }
  import autoImport._

  override def globalSettings = Def.settings(
    compatibilityDependencyRules := Seq.empty
  )
}
