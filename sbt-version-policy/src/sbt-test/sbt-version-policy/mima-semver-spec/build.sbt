ThisBuild / organization := "com.example"
ThisBuild / scalaVersion := "2.13.2"
ThisBuild / versionScheme := Some("semver-spec")

val `v0-1-0` =
  project.settings(
    name := "semver-spec-test",
    version := "0.1.0",
    versionPolicyIntention := Compatibility.None
  )

val `v0-1-1` =
  project.settings(
    name := "semver-spec-test",
    version := "0.1.1",
    versionPolicyIntention := Compatibility.BinaryAndSourceCompatible
  )

val `v0-1-2` =
  project.settings(
    name := "semver-spec-test",
    version := "0.1.2",
    versionPolicyIntention := Compatibility.BinaryCompatible
  )

val `v0-1-3` =
  project.settings(
    name := "semver-spec-test",
    version := "0.1.3",
    versionPolicyIntention := Compatibility.None
  )

val `v0-2-0` =
  project.settings(
    name := "semver-spec-test",
    version := "0.2.0",
    versionPolicyIntention := Compatibility.None
  )

val `v1-0-0` =
  project.settings(
    name := "semver-spec-test",
    version := "1.0.0",
    versionPolicyIntention := Compatibility.None
  )

val `v1-0-1` =
  project.settings(
    name := "semver-spec-test",
    version := "1.0.1",
    versionPolicyIntention := Compatibility.BinaryAndSourceCompatible
  )

val `v1-0-2` =
  project.settings(
    name := "semver-spec-test",
    version := "1.0.2",
    versionPolicyIntention := Compatibility.BinaryAndSourceCompatible
  )

val `v1-1-0` =
  project.settings(
    name := "semver-spec-test",
    version := "1.1.0",
    versionPolicyIntention := Compatibility.BinaryCompatible
  )

val `v1-2-0` =
  project.settings(
    name := "semver-spec-test",
    version := "1.2.0",
    versionPolicyIntention := Compatibility.BinaryCompatible
  )

val `v2-0-0` =
  project.settings(
    name := "semver-spec-test",
    version := "2.0.0",
    versionPolicyIntention := Compatibility.None
  )
