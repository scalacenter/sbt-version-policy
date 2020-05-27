sbtPlugin := true
enablePlugins(ScriptedPlugin)
scriptedLaunchOpts += "-Dplugin.version=" + version.value
scriptedBufferLog := false

organization := "io.github.alexarchambault.sbt"
name := "sbt-compatibility"

addSbtPlugin("com.typesafe" % "sbt-mima-plugin" % "0.7.0")
libraryDependencies ++= Seq(
  "io.github.alexarchambault" %% "data-class" % "0.2.3" % Provided,
  compilerPlugin("org.scalamacros" % "paradise" % "2.1.1" cross CrossVersion.full)
)

libraryDependencies += "com.lihaoyi" %% "utest" % "0.7.4" % Test
testFrameworks += new TestFramework("utest.runner.Framework")
