ThisBuild / organization := "com.example"
ThisBuild / scalaVersion := "2.13.2"
ThisBuild / versionPolicyPreviousVersionRepositories := CoursierDefaultRepositories

val `v0-1-0` =
  project.settings(
    name := "version-test",
    version := "0.1.0",
    versionPolicyIntention := Compatibility.None
  )

// Patch increment and BinaryCompatible => OK
val `v0-1-1` =
  project.settings(
    name := "version-test",
    version := "0.1.1",
    versionPolicyIntention := Compatibility.BinaryCompatible
  )

// Patch increment but no compatibility => Error
val `v0-1-2` =
  project.settings(
    name := "version-test",
    version := "0.1.2",
    versionPolicyIntention := Compatibility.None
  )

// Minor increment and no compatibility => OK
val `v0-2-0` =
  project.settings(
    name := "version-test",
    version := "0.2.0",
    versionPolicyIntention := Compatibility.None
  )

// Major increment and no compatibility => OK
val `v1-0-0` =
  project.settings(
    name := "version-test",
    version := "1.0.0",
    versionPolicyIntention := Compatibility.None
  )

// Patch increment and binary and source compatibility => OK
val `v1-0-1` =
  project.settings(
    name := "version-test",
    version := "1.0.1",
    versionPolicyIntention := Compatibility.BinaryAndSourceCompatible
  )

// Patch increment and binary compatibility => Error
val `v1-0-2` =
  project.settings(
    name := "version-test",
    version := "1.0.2",
    versionPolicyIntention := Compatibility.BinaryCompatible
  )

// Patch increment and no compatibility => Error
val `v1-0-3` =
  project.settings(
    name := "version-test",
    version := "1.0.3",
    versionPolicyIntention := Compatibility.None
  )

// Minor increment and binary compatibility => OK
val `v1-1-0` =
  project.settings(
    name := "version-test",
    version := "1.1.0",
    versionPolicyIntention := Compatibility.BinaryCompatible
  )

// Minor increment and no compatibility => Error
val `v1-2-0` =
  project.settings(
    name := "version-test",
    version := "1.2.0",
    versionPolicyIntention := Compatibility.None
  )

// Major increment and binary compatibility => OK
val `v2-0-0` =
  project.settings(
    name := "version-test",
    version := "2.0.0",
    versionPolicyIntention := Compatibility.BinaryCompatible
  )
