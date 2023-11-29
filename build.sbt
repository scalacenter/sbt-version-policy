import com.typesafe.tools.mima.core._

inThisBuild(List(
  organization := "ch.epfl.scala",
  homepage := Some(url("https://github.com/scalacenter/sbt-version-policy")),
  licenses := List("Apache-2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0")),
  developers := List(
    Developer(
      "alexarchambault",
      "Alexandre Archambault",
      "",
      url("https://github.com/alexarchambault")
    )
  ),
  versionPolicyIntention := Compatibility.None,
  libraryDependencySchemes += "com.typesafe" %% "mima-core" % "semver-spec"
))

lazy val root = (project in file("."))
  .aggregate(`sbt-version-policy`)
  .settings(
    name := "sbt-version-policy root",
    publish / skip := true,
  )

lazy val `sbt-version-policy` = project
  .enablePlugins(SbtPlugin)
  .settings(
    scriptedLaunchOpts += "-Dplugin.version=" + version.value,
    scriptedBufferLog := false,
    addSbtPlugin("com.typesafe" % "sbt-mima-plugin" % "1.1.3"),
    libraryDependencies ++= Seq(
      "io.get-coursier" % "interface" % "1.0.18",
      "io.get-coursier" %% "versions" % "0.3.1",
      "com.eed3si9n.verify" %% "verify" % "2.0.1" % Test,
    ),
    testFrameworks += new TestFramework("verify.runner.Framework"),
    mimaBinaryIssueFilters ++= Seq(
      // Add Mima filters here
    ),
  )
