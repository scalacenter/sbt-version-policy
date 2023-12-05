package sbtversionpolicy

import com.typesafe.tools.mima.plugin.MimaPlugin
import sbt._

object SbtVersionPolicyPlugin extends AutoPlugin {

  override def trigger = allRequirements
  override def requires = MimaPlugin

  private def mimaIgnoreVersion(version: String): Seq[Def.Setting[_]] =
    Def.settings(
      MimaPlugin.autoImport.mimaPreviousArtifacts := {
        val value = MimaPlugin.autoImport.mimaPreviousArtifacts.value
        value.filter(_.revision != version)
      }
    )

  object autoImport extends SbtVersionPolicyKeys {
    val VersionCompatibility = coursier.version.VersionCompatibility
    val Compatibility = sbtversionpolicy.Compatibility
    type Compatibility = sbtversionpolicy.Compatibility
    def versionPolicyIgnoreVersion(version: String): Seq[Def.Setting[_]] =
      SbtVersionPolicyPlugin.mimaIgnoreVersion(version)
  }

  override def globalSettings =
    SbtVersionPolicySettings.reconciliationGlobalSettings ++
      SbtVersionPolicySettings.schemesGlobalSettings ++
      SbtVersionPolicySettings.exportGlobalSettings

  override def projectSettings =
    SbtVersionPolicySettings.updateSettings ++
      SbtVersionPolicySettings.reconciliationSettings ++
      SbtVersionPolicySettings.previousArtifactsSettings ++
      SbtVersionPolicySettings.findIssuesSettings ++
      SbtVersionPolicySettings.skipSettings

  /**
   * Compute the highest compatibility level satisfied by all the projects aggregated by the
   * project this task is applied to.
   * This is useful to know the overall level of compatibility of a multi-module project.
   * On every aggregated project, it invokes `versionPolicyAssessCompatibility` and keeps
   * the first result only (ie, it assumes that that task assessed the compatibility with
   * the latest release only).
   */
  val aggregatedAssessedCompatibilityWithLatestRelease: Def.Initialize[Task[Compatibility]] =
    Def.taskDyn {
      import autoImport.versionPolicyAssessCompatibility
      val log = Keys.streams.value.log
      // Take all the projects aggregated by this project
      val aggregatedProjects = Keys.thisProject.value.aggregate

      aggregatedCompatibility(aggregatedProjects, log) { submodule =>
        Def.task {
          (submodule / versionPolicyAssessCompatibility).value
        }
      } { compatibilities =>
        // The most common case is to assess the compatibility with the latest release,
        // so we look at the first element only and discard the others
        compatibilities.headOption.map(_._2)
      }.map(_._1) // Discard submodules details
    }

  // Compute the highest compatibility level that is satisfied by all the aggregated projects
  private[sbtversionpolicy] def aggregatedCompatibility[A](
    submodules: Seq[ProjectRef],
    log: Logger
  )(
    f: ProjectRef => Def.Initialize[Task[A]]
  )(
    compatibility: A => Option[Compatibility]
  ): Def.Initialize[Task[(Compatibility, Seq[A])]] =
    submodules.foldLeft(
      Def.task {
        (Compatibility.BinaryAndSourceCompatible: Compatibility, Seq.newBuilder[A])
      }
    ) { case (highestCompatibilityAndResults, module) =>
      Def.task {
        val (highestCompatibility, results) = highestCompatibilityAndResults.value
        val result = f(module).value
        compatibility(result) match {
          case Some(compatibility) =>
            log.debug(s"Compatibility of aggregated project ${module.project} is ${compatibility}")
            (
              // Take the lowest of both
              Compatibility.ordering.min(highestCompatibility, compatibility),
              results += result
            )
          case None =>
            log.debug(s"Unable to assess the compatibility level of the aggregated project ${module.project}")
            (highestCompatibility, results)
        }
      }
    }.map { case (compatibility, builder) =>
      (compatibility, builder.result())
    }

}
