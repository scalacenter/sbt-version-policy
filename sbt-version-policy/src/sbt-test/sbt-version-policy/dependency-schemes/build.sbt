ThisBuild / scalaVersion := "2.13.6"
ThisBuild / organization := "ch.epfl.scala"

val a1 = project
  .settings(
    name := "dependency-schemes-test-a",
    version := "1.0.0"
  )

val a2 = project
  .settings(
    name := "dependency-schemes-test-a",
    version := "1.1.0"
  )

val b1 = project
  .settings(
    name := "dependency-schemes-test-b",
    version := "1.0.0"
  )
  .dependsOn(a1)

val b2 = project
  .settings(
    name := "dependency-schemes-test-b",
    version := "1.1.0",
    versionPolicyIntention := Compatibility.BinaryCompatible
  )
  .dependsOn(a2)
