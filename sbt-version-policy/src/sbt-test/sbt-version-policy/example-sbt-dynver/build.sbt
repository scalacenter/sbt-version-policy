ThisBuild / scalaVersion := "2.13.6"
ThisBuild / organization := "com.example"
// Before the first release, set it to `None` because there is no previous release to compare to
ThisBuild / versionPolicyIntention := Compatibility.None

val a =
  project
    .settings(
      name := "dynver-test-a"
    )

val b =
  project
    .settings(
      name := "dynver-test-b",
    )
    .dependsOn(a)

val root =
  project.in(file("."))
    .aggregate(a, b)
    .settings(
      publish / skip := true
    )
