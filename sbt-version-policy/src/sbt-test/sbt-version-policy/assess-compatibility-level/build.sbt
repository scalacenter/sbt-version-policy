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

val `v1_0_0` =
  project.settings(
    name := "assess-compatibility-test",
    version := "1.0.0",
    libraryDependencies += "org.typelevel" %% "cats-core" % "2.6.0",
    checkTasks,
  )

// binary and source compatible change in the code
val `v1_0_1` =
  project.settings(
    name := "assess-compatibility-test",
    version := "1.0.0+n", // we don’t set the version yet, it will be set by the scripted test
    libraryDependencies += "org.typelevel" %% "cats-core" % "2.6.0",
    checkTasks,
  )

// No changes in the code, patch bump of library dependency
val `v1_0_2` =
  project.settings(
    name := "assess-compatibility-test",
    version := "1.0.1+n",
    libraryDependencies += "org.typelevel" %% "cats-core" % "2.6.1",
    checkTasks,
  )

// Source incompatible change in the code
val `v1_1_0` =
  project.settings(
    name := "assess-compatibility-test",
    version := "1.0.2+n",
    libraryDependencies += "org.typelevel" %% "cats-core" % "2.6.1",
    checkTasks,
  )

// No changes in the code, minor bump of library dependency
val `v1_2_0` =
  project.settings(
    name := "assess-compatibility-test",
    version := "1.1.0+n",
    libraryDependencies += "org.typelevel" %% "cats-core" % "2.7.0",
    checkTasks,
  )

// Binary incompatible change in the code
val `v2_0_0` =
  project.settings(
    name := "assess-compatibility-test",
    version := "1.2.0+n",
    libraryDependencies += "org.typelevel" %% "cats-core" % "2.7.0",
    checkTasks,
  )

// No changes in the code, breaking change in the dependencies
val `v3_0_0` =
  project.settings(
    name := "assess-compatibility-test",
    version := "2.0.0+n",
    // no library dependency anymore
    checkTasks,
  )
