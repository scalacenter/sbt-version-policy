ThisBuild / scalaVersion := "2.13.6"
ThisBuild / organization := "ch.epfl.scala"

// Baseline version with a Runtime-only dependency that has transitive deps
// not on the Compile classpath (e.g. okhttp, okio, kotlin-stdlib).
val a = project
  .settings(
    name := "runtime-dependency-test",
    version := "1.0.0",
    libraryDependencies += "com.squareup.okhttp3" % "okhttp" % "4.12.0" % Runtime,
  )

// New version with the same Runtime-only dependency.
// versionPolicyReportDependencyIssues should succeed because the dependency
// has not changed
val b = project
  .settings(
    name := "runtime-dependency-test",
    version := "1.1.0",
    versionPolicyIntention := Compatibility.BinaryCompatible,
    libraryDependencies += "com.squareup.okhttp3" % "okhttp" % "4.12.0" % Runtime,
  )
