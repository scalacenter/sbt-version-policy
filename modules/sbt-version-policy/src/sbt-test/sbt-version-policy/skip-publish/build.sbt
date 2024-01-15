ThisBuild / organization := "com.example"
ThisBuild / scalaVersion := "2.13.2"

val v100 =
  project.in(file("v1-0-0")).settings(
    name := "library-test-skip",
    version := "1.0.0",
    versionPolicyIntention := Compatibility.None
  )

val v101 =
  project.in(file("v1-0-1")).settings(
    name := "library-test-skip",
    version := "1.0.1",
    versionPolicyIntention := Compatibility.BinaryCompatible
  )

val root =
  project.in(file("."))
    .settings(
      name := "library-test-skip-root",
    )
    .aggregate(v101)
