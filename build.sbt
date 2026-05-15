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
  versionPolicyIntention := Compatibility.BinaryCompatible,
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
    addSbtPlugin("com.typesafe" % "sbt-mima-plugin" % "1.1.5"),
    crossScalaVersions += "3.8.2",
    scalacOptions ++= {
      scalaBinaryVersion.value match {
        case "3" =>
          Nil
        case _ =>
          Seq(
            "-release:8",
          )
      }
    },
    pluginCrossBuild / sbtVersion := {
      scalaBinaryVersion.value match {
        case "2.12" =>
          sbtVersion.value
        case _ =>
          "2.0.0-RC12"
      }
    },
    libraryDependencies ++= {
      scalaBinaryVersion.value match {
        case "3" =>
          // https://github.com/scala/scala3/issues/18487$
          Seq("net.hamnaberg" %% "dataclass-annotation" % "0.3.0" % Provided)
        case _ =>
          Nil
      }
    },
    libraryDependencies ++= Seq(
      "io.get-coursier" % "interface" % "1.0.28",
      ("io.get-coursier" %% "versions" % "0.5.3").cross(CrossVersion.for3Use2_13),
      "com.lihaoyi" %% "ujson" % "3.1.4", // FIXME shade
      "com.eed3si9n.verify" %% "verify" % "1.0.0" % Test,
    ),
    testFrameworks += new TestFramework("verify.runner.Framework"),
    mimaBinaryIssueFilters ++= Seq(
      // Add Mima filters here
    ),
  )
