ThisBuild / organization := "com.example"
ThisBuild / scalaVersion := "2.13.18"

// https://github.com/sbt/sbt/issues/8248
SettingKey[String]("outputPath") := thisProject.value.id

val `v1-0-0` =
  project.settings(
    name := "signature-test",
    version := "1.0.0",
    versionPolicyIntention := Compatibility.None
  )

val `v1-1-0` =
  project.settings(
    name := "signature-test",
    version := "1.1.0",
    versionPolicyIntention := Compatibility.BinaryCompatible
  )

val `v1-1-0-signatures` =
  project.settings(
    name := "signature-test",
    version := "1.1.0",
    versionPolicyIntention := Compatibility.BinaryCompatible,
    mimaReportSignatureProblems := true,
    Compile / unmanagedSourceDirectories += (`v1-1-0` / Compile / scalaSource).value
  )
