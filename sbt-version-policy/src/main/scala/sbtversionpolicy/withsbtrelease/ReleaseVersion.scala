package sbtversionpolicy.withsbtrelease

import sbtversionpolicy.Compatibility
import sbtversionpolicy.SbtVersionPolicyPlugin.aggregatedAssessedCompatibilityWithLatestRelease
import sbtversionpolicy.SbtVersionPolicyPlugin.autoImport.versionPolicyAssessCompatibility
import sbt.*

/** Convenient methods to integrate with the plugin sbt-release */
object ReleaseVersion {

  /**
   * @return a [release version function](https://github.com/sbt/sbt-release?tab=readme-ov-file#custom-versioning)
   *         that bumps the patch, minor, or major version number depending on the provided
   *         compatibility level.
   * @param qualifier Optional qualifier to append to the version (e.g. `"-RC1"`). Empty by default.
   */
  def fromCompatibility(compatibility: Compatibility, qualifier: String = ""): String => String = {
    val maybeBump =
      compatibility match {
        case Compatibility.None => Some(Version.Bump.Major)
        case Compatibility.BinaryCompatible => Some(Version.Bump.Minor)
        case Compatibility.BinaryAndSourceCompatible => None // No need to bump the patch version, because it has already been bumped when sbt-release set the next release version
      }
    { (currentVersion: String) =>
      val versionWithoutQualifier =
        Version(currentVersion)
          .getOrElse(Version.formatError(currentVersion))
          .withoutQualifier
      val bumpedVersion =
        (maybeBump match {
          case Some(bump) => versionWithoutQualifier.bump(bump)
          case None => versionWithoutQualifier
        }).string
      s"${bumpedVersion}${qualifier}"
    }
  }

  /**
   * Task returning a [release version function](https://github.com/sbt/sbt-release?tab=readme-ov-file#custom-versioning)
   * based on the assessed compatibility level of the project.
   *
   * Use it in your `.sbt` build definition as follows:
   *
   * {{{
   *   import sbtversionpolicy.withsbtrelease.ReleaseVersion
   *
   *   releaseVersion := ReleaseVersion.fromAssessedCompatibilityWithLatestRelease.value
   * }}}
   *
   * sbt-release uses the `releaseVersion` function to set the version before publishing a release (at step
   * `setReleaseVersion`). It reads the current `version` (usually defined in a file `version.sbt`, and looking
   * like `"1.2.3-SNAPSHOT"`), and applies the function to it.
   *
   * @param qualifier Optional qualifier to append to the version (e.g. `"-RC1"`). Empty by default.
   */
  def fromAssessedCompatibilityWithLatestRelease(qualifier: String = ""): Def.Initialize[Task[String => String]] =
    Def.task {
      val compatibilityResults = versionPolicyAssessCompatibility.value
      val log = Keys.streams.value.log
      val compatibilityWithLatestRelease =
        compatibilityResults.headOption
          .getOrElse(throw new MessageOnlyException("Unable to assess the compatibility level of this project. Is 'versionPolicyPreviousVersions' defined?"))
      val (_, compatibility) = compatibilityWithLatestRelease
      log.debug(s"Compatibility level is ${compatibility}")
      fromCompatibility(compatibility, qualifier)
    }

  /**
   * Task returning a [release version function](https://github.com/sbt/sbt-release?tab=readme-ov-file#custom-versioning)
   * based on the assessed compatibility level of the project (ie, the highest level of compatibility
   * satisfied by all the sub-projects aggregated by this project).
   *
   * Use it in the root project of your `.sbt` build definition as follows:
   *
   * {{{
   *   import sbtversionpolicy.withsbtrelease.ReleaseVersion
   *
   *   val `my-project` =
   *     project
   *       .in(file("."))
   *       .aggregate(mySubproject1, mySubproject2)
   *       .settings(
   *         releaseVersion := ReleaseVersion.fromAggregatedAssessedCompatibilityWithLatestRelease.value
   *       )
   * }}}
   *
   * sbt-release uses the `releaseVersion` function to set the version before publishing a release (at step
   * `setReleaseVersion`). It reads the current `version` (usually defined in a file `version.sbt`, and looking
   * like `"1.2.3-SNAPSHOT"`), and applies the function to it.
   */
  def fromAggregatedAssessedCompatibilityWithLatestRelease(qualifier: String = ""): Def.Initialize[Task[String => String]] =
    Def.task {
      val log = Keys.streams.value.log
      val compatibility = aggregatedAssessedCompatibilityWithLatestRelease.value
      log.debug(s"Aggregated compatibility level is ${compatibility}")
      fromCompatibility(compatibility, qualifier)
    }

}
