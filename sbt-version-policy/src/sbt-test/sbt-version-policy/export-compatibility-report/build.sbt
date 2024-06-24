ThisBuild / organization := "com.example"
ThisBuild / scalaVersion := "2.13.2"
ThisBuild / versionPolicyPreviousVersionRepositories := CoursierDefaultRepositories

val v1_a =
  project
    .settings(
      name := "export-compatibility-report-test-a",
      version := "1.0.0"
    )

val v1_b =
  project
    .settings(
      name := "export-compatibility-report-test-b",
      version := "1.0.0"
    )

val v1_c =
  project
    .settings(
      name := "export-compatibility-report-test-c",
      publish / skip := true
    )

val v1_root =
  project
    .settings(
      name := "export-compatibility-report-test-root",
      publish / skip := true
    )
    .aggregate(v1_a, v1_b, v1_c)


val v2_a =
  project
    .settings(
      name := "export-compatibility-report-test-a",
      version := "1.0.0+n"
    )

val v2_b =
  project
    .settings(
      name := "export-compatibility-report-test-b",
      version := "1.0.0+n"
    )

val v2_c =
  project
    .settings(
      name := "export-compatibility-report-test-c",
      publish / skip := true
    )

val v2_root =
  project
    .settings(
      name := "export-compatibility-report-test-root",
      publish / skip := true
    )
    .aggregate(v2_a, v2_b, v2_c)
