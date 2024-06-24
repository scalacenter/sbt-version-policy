ThisBuild / versionPolicyPreviousVersionRepositories := CoursierDefaultRepositories

// A project with two modules (“a” and “b”) and a root module that aggregates them

val v1_a =
  project
    .settings(
      name := "aggregated-test-a",
      version := "1.0.0",
    )

val v1_b =
  project
    .settings(
      name := "aggregated-test-b",
      version := "1.0.0",
    )

val v1_root =
  project
    .settings(
      name := "aggregated-test-root",
      publish / skip := true,
    )
    .aggregate(v1_a, v1_b)


// First round of evolutions
// No changes in v2_a
val v2_a =
  project
    .settings(
      name := "aggregated-test-a",
      version := "1.0.0+n",
    )

// Small changes that don’t break the binary and source compatibility
val v2_b =
  project
    .settings(
      name := "aggregated-test-b",
      version := "1.0.0+n",
    )

val v2_root =
  project
    .settings(
      name := "aggregated-test-root",
      publish / skip := true,
      TaskKey[Unit]("check") := {
        val compatibility = SbtVersionPolicyPlugin.aggregatedAssessedCompatibilityWithLatestRelease.value
        assert(compatibility == Compatibility.BinaryAndSourceCompatible)
      }
    )
    .aggregate(v2_a, v2_b)


// Second round of evolutions
// Introduction of a public member
val v3_a =
  project
    .settings(
      name := "aggregated-test-a",
      version := "1.0.0+n",
    )

// No changes
val v3_b =
  project
    .settings(
      name := "aggregated-test-b",
      version := "1.0.0+n",
    )

val v3_root =
  project
    .settings(
      name := "aggregated-test-root",
      publish / skip := true,
      TaskKey[Unit]("check") := {
        val compatibility = SbtVersionPolicyPlugin.aggregatedAssessedCompatibilityWithLatestRelease.value
        assert(compatibility == Compatibility.BinaryCompatible)
      }
    )
    .aggregate(v3_a, v3_b)

// Third round of evolutions
// Introduction of a public member
val v4_a =
  project
    .settings(
      name := "aggregated-test-a",
      version := "1.0.0+n",
    )

// Removal of a public member
val v4_b =
  project
    .settings(
      name := "aggregated-test-b",
      version := "1.0.0+n",
    )

val v4_root =
  project
    .settings(
      name := "aggregated-test-root",
      publish / skip := true,
      TaskKey[Unit]("check") := {
        val compatibility = SbtVersionPolicyPlugin.aggregatedAssessedCompatibilityWithLatestRelease.value
        assert(compatibility == Compatibility.None)
      }
    )
    .aggregate(v4_a, v4_b)


