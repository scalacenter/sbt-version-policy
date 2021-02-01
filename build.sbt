
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
  )
))

lazy val root = (project in file("."))
  .aggregate(`sbt-version-policy`, `sbt-version-policy-dummy`)
  .settings(
    name := "sbt-version-policy root",
    publish / skip := true,
  )

lazy val `sbt-version-policy` = project
  .enablePlugins(SbtPlugin)
  .settings(
    scriptedLaunchOpts += "-Dplugin.version=" + version.value,
    scriptedBufferLog := false,
    addSbtPlugin("com.typesafe" % "sbt-mima-plugin" % "0.8.1"),
    libraryDependencies ++= Seq(
      "io.github.alexarchambault" %% "data-class" % "0.2.5" % Provided,
      compilerPlugin("org.scalamacros" % "paradise" % "2.1.1" cross CrossVersion.full)
    ),
    libraryDependencies ++= Seq(
      "io.get-coursier" % "interface" % "1.0.0",
      "io.get-coursier" %% "versions" % "0.3.0",
      "com.eed3si9n.verify" %% "verify" % "0.2.0" % Test,
    ),
    testFrameworks += new TestFramework("verify.runner.Framework"),
  )

lazy val `sbt-version-policy-dummy` = project
  .in(file("sbt-version-policy/target/dummy"))
