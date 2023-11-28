ThisBuild / organization := "com.example"
ThisBuild / scalaVersion := "2.13.2"
ThisBuild / versionScheme := Some("semver-spec")

val checkTasks = Seq(
  TaskKey[Unit]("checkAssessedCompatibilityIsBinaryAndSourceCompatible") := {
    val (_, compatibility) = versionPolicyAssessCompatibility.value.head
    assert(compatibility == Compatibility.BinaryAndSourceCompatible, s"Unexpected assessed compatibility: ${compatibility}")
  },
  TaskKey[Unit]("checkAssessedCompatibilityIsBinaryCompatible") := {
    val (_, compatibility) = versionPolicyAssessCompatibility.value.head
    assert(compatibility == Compatibility.BinaryCompatible, s"Unexpected assessed compatibility: ${compatibility}")
  },
  TaskKey[Unit]("checkAssessedCompatibilityIsNone") := {
    val (_, compatibility) = versionPolicyAssessCompatibility.value.head
    assert(compatibility == Compatibility.None, s"Unexpected assessed compatibility: ${compatibility}")
  }
)

val `v1-0-0` =
  project.settings(
    name := "assess-compatibility-test",
    version := "1.0.0",
    libraryDependencies += "org.typelevel" %% "cats-core" % "2.6.0",
    checkTasks,
  )

// binary and source compatible change in the code
val `v1-0-1` =
  project.settings(
    name := "assess-compatibility-test",
    version := "1.0.1",
    libraryDependencies += "org.typelevel" %% "cats-core" % "2.6.0",
    checkTasks,
  )

// No changes in the code, patch bump of library dependency
val `v1-0-2` =
  project.settings(
    name := "assess-compatibility-test",
    version := "1.0.2",
    libraryDependencies += "org.typelevel" %% "cats-core" % "2.6.1",
    checkTasks,
  )

// Source incompatible change in the code
val `v1-1-0` =
  project.settings(
    name := "assess-compatibility-test",
    version := "1.1.0",
    libraryDependencies += "org.typelevel" %% "cats-core" % "2.6.1",
    checkTasks,
  )

// No changes in the code, minor bump of library dependency
val `v1-2-0` =
  project.settings(
    name := "assess-compatibility-test",
    version := "1.2.0",
    libraryDependencies += "org.typelevel" %% "cats-core" % "2.9.0",
    checkTasks,
  )

// Binary incompatible change in the code
val `v2-0-0` =
  project.settings(
    name := "assess-compatibility-test",
    version := "2.0.0",
    libraryDependencies += "org.typelevel" %% "cats-core" % "2.9.0",
    checkTasks,
  )

// No changes in the code, breaking change in the dependencies
val `v3-0-0` =
  project.settings(
    name := "assess-compatibility-test",
    version := "3.0.0",
    // no library dependency anymore
    checkTasks,
  )
