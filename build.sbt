
inThisBuild(List(
  organization := "io.github.alexarchambault.sbt",
  homepage := Some(url("https://github.com/alexarchambault/sbt-compatibility")),
  licenses := List("Apache-2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0")),
  developers := List(
    Developer(
      "alexarchambault",
      "Alexandre Archambault",
      "",
      url("https://github.com/alexarchambault")
    )
  )
))

sbtPlugin := true
enablePlugins(ScriptedPlugin)
scriptedLaunchOpts += "-Dplugin.version=" + version.value
scriptedBufferLog := false

name := "sbt-compatibility"

sonatypeProfileName := "io.github.alexarchambault"

addSbtPlugin("com.typesafe" % "sbt-mima-plugin" % "0.7.0")
libraryDependencies ++= Seq(
  "io.github.alexarchambault" %% "data-class" % "0.2.3" % Provided,
  compilerPlugin("org.scalamacros" % "paradise" % "2.1.1" cross CrossVersion.full)
)

libraryDependencies += "com.lihaoyi" %% "utest" % "0.7.4" % Test
testFrameworks += new TestFramework("utest.runner.Framework")
