import sbtversionpolicy.withsbtrelease.ReleaseVersion
import sbtrelease._
import ReleaseTransformations._

inThisBuild(List(
  organization := "org.organization",
  homepage := Some(url("https://github.com/organization/project")),
  licenses := List("Apache-2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0")),
  developers := List(
    Developer(
      "julienrf",
      "Julien Richard-Foy",
      "julien@organization.org",
      url("https://github.com/julienrf")
    )
  ),
  scalaVersion := "3.0.1"
))

val module =
  project
    .settings(
      name := "sbt-release-unconstrained-test"
    )

val root = project.in(file("."))
  .aggregate(module)
  .settings(
    // Tell sbt-release to set the release version based on the level of compatibility with the previous release
    releaseVersion := ReleaseVersion.fromAggregatedAssessedCompatibilityWithLatestRelease().value,
    // Custom release process for testing purpose only: the artifacts are locally published,
    // and we don’t push to the remote repository.
    releaseProcess := Seq[ReleaseStep](
      checkSnapshotDependencies,
      inquireVersions,
      runClean,
      runTest,
      setReleaseVersion,
      commitReleaseVersion,
      tagRelease,
      releaseStepTask(module / publishLocal), // Publish locally for our tests only, in practice you will publish to Sonatype
      setNextVersion,
      commitNextVersion,
      // pushChanges // Disable pushing the changes to the remote repository for our tests only
    )
  )

TaskKey[Unit]("checkTag_1_0_0") := {
  import scala.sys.process._
  assert("git describe --tags".lineStream.exists(_.contains("v1.0.0")))
}

TaskKey[Unit]("checkTag_1_1_0") := {
  import scala.sys.process._
  assert("git describe --tags".lineStream.exists(_.contains("v1.1.0")))
}
