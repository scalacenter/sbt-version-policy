ThisBuild / organization := "com.example"
ThisBuild / scalaVersion := "3.0.0-RC3"
ThisBuild / versionPolicyPreviousVersionRepositories := CoursierDefaultRepositories

val `v1-0-0` =
  project.settings(
    name := "scala-3-test",
    version := "1.0.0",
    versionPolicyIntention := Compatibility.None
  )

val `v1-0-1` =
  project.settings(
    name := "scala-3-test",
    version := "1.0.1",
    versionPolicyIntention := Compatibility.BinaryAndSourceCompatible
  )
