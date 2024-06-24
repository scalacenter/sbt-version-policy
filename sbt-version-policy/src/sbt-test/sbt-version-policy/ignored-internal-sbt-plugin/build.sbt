ThisBuild / scalaVersion := "2.12.19"
ThisBuild / organization := "com.example"
ThisBuild / versionPolicyIntention := Compatibility.BinaryAndSourceCompatible
ThisBuild / versionPolicyPreviousVersionRepositories := CoursierDefaultRepositories

val a_1 =
  project
    .settings(
      name := "ignored-internal-dependencies-a",
      version := "1.0.0"
    )

val b_1 =
  project
    .enablePlugins(SbtPlugin)
    .settings(
      name := "ignored-internal-dependencies-b",
      version := "1.0.0"
    )

val c_1 =
  project
    .enablePlugins(SbtPlugin)
    .settings(
      name := "ignored-internal-dependencies-c",
      version := "1.0.0"
    )
    .dependsOn(a_1, b_1)

val a_2 =
  project
    .settings(
      name := "ignored-internal-dependencies-a",
      version := "2.0.0"
    )

val b_2 =
  project
    .enablePlugins(SbtPlugin)
    .settings(
      name := "ignored-internal-dependencies-b",
      version := "2.0.0"
    )

val c_2 =
  project
    .enablePlugins(SbtPlugin)
    .settings(
      name := "ignored-internal-dependencies-c",
      version := "1.0.1"
    )
    .dependsOn(a_2, b_2)
