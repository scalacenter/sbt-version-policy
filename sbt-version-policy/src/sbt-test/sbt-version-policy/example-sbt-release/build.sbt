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

val module = project

val root = project.in(file("."))
  .aggregate(module)
  .settings(
    publish / skip := true,
    // Configure releaseVersion to bump the patch, minor, or major version number according
    // to the compatibility intention set by versionPolicyIntention.
    releaseVersion := setReleaseVersionFunction(versionPolicyIntention.value),
    // Custom release process: run `versionCheck` after we have set the release version, and
    // reset compatibility intention to `Compatibility.BinaryAndSourceCompatible` after the release.
    // There are some other modifications for testing: the artifacts are locally published,
    // and we don’t push to the remote repository.
    releaseProcess := Seq[ReleaseStep](
      checkSnapshotDependencies,
      inquireVersions,
      runClean,
      runTest,
      setReleaseVersion,
      releaseStepCommand("versionCheck"), // Run task `versionCheck` after the release version is set
      commitReleaseVersion,
      tagRelease,
      releaseStepTask(module / publishLocal), // Publish locally for our tests only, in practice you will publish to Sonatype
      setNextVersion,
      commitNextVersion,
      releaseStepTask(setAndCommitNextCompatibilityIntention), // Reset compatibility intention to `Compatibility.BinaryAndSourceCompatible`
      // pushChanges // Disable pushing the changes to the remote repository for our tests only
    )
  )

def setReleaseVersionFunction(compatibilityIntention: Compatibility): String => String = {
  val maybeBump = compatibilityIntention match {
    case Compatibility.None                      => Some(Version.Bump.Major)
    case Compatibility.BinaryCompatible          => Some(Version.Bump.Minor)
    case Compatibility.BinaryAndSourceCompatible => None // No need to bump the patch version, because it has already been bumped when sbt-release set the next release version
  }
  { (currentVersion: String) =>
    val versionWithoutQualifier =
      Version(currentVersion)
        .getOrElse(versionFormatError(currentVersion))
        .withoutQualifier
    (maybeBump match {
      case Some(bump) => versionWithoutQualifier.bump(bump)
      case None       => versionWithoutQualifier
    }).string
  }
}

lazy val setAndCommitNextCompatibilityIntention = taskKey[Unit]("Set versionPolicyIntention to Compatibility.BinaryAndSourceCompatible, and commit the change")
ThisBuild / setAndCommitNextCompatibilityIntention := {
  val log = streams.value.log
  val intention = (ThisBuild / versionPolicyIntention).value
  if (intention == Compatibility.BinaryAndSourceCompatible) {
    log.info("Not changing compatibility intention because it is already set to BinaryAndSourceCompatible")
  } else {
    log.info("Reset compatibility intention to BinaryAndSourceCompatible")
    IO.write(
      new File("compatibility.sbt"),
      "ThisBuild / versionPolicyIntention := Compatibility.BinaryAndSourceCompatible\n"
    )
    val gitAddExitValue =
      sys.process.Process("git add compatibility.sbt").run(log).exitValue()
    assert(gitAddExitValue == 0, s"Command failed with exit status $gitAddExitValue")
    val gitCommitExitValue =
      sys.process
        .Process(Seq("git", "commit", "-m", "Reset compatibility intention"))
        .run(log)
        .exitValue()
    assert(gitCommitExitValue == 0, s"Command failed with exist status $gitCommitExitValue")
  }
}

TaskKey[Unit]("checkTag_1_0_0") := {
  import scala.sys.process._
  assert("git describe --tags".lineStream.exists(_.contains("v1.0.0")))
}

TaskKey[Unit]("checkTag_1_0_1") := {
  import scala.sys.process._
  assert("git describe --tags".lineStream.exists(_.contains("v1.0.1")))
}
