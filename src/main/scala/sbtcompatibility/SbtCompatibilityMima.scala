package sbtcompatibility

import com.typesafe.tools.mima.plugin.MimaPlugin
import coursier.version.Previous
import sbt.{AutoPlugin, Def, Keys, settingKey}

import scala.collection.JavaConverters._
import coursier.version.VersionCompatibility
import coursier.version.Version
import sbt.librarymanagement.CrossVersion
import sbt.librarymanagement.ModuleID

object SbtCompatibilityMima extends AutoPlugin {

  override def trigger = allRequirements
  override def requires = MimaPlugin

  object autoImport {
    val previousVersions = settingKey[Seq[String]]("")
    val firstVersion = settingKey[Option[String]]("")
  }

  import autoImport._

  private def moduleName(crossVersion: CrossVersion, sv: String, sbv: String, baseName: String): String =
    CrossVersion(crossVersion, sv, sbv).fold(baseName)(_(baseName))
  private def moduleName(m: ModuleID, sv: String, sbv: String): String =
    moduleName(m.crossVersion, sv, sbv, m.name)

  private lazy val previousVersionsFromRepo = Def.setting {

    val projId = Keys.projectID.value
    val sv = Keys.scalaVersion.value
    val sbv = Keys.scalaBinaryVersion.value
    val name = moduleName(projId, sv, sbv)

    val ivyProps = sbtcompatibility.internal.Resolvers.defaultIvyProperties(Keys.ivyPaths.value.ivyHome)
    val repos = Keys.resolvers.value.flatMap { res =>
      val repoOpt = sbtcompatibility.internal.Resolvers.repository(res, ivyProps, s => System.err.println(s))
      if (repoOpt.isEmpty)
        System.err.println(s"Warning: ignoring repository ${res.name} to get previous version")
      repoOpt.toSeq
    }
    // Can't reference Keys.fullResolvers, which is a task.
    // So we're using the usual default repositories from coursier here…
    val fullRepos = coursierapi.Repository.defaults().asScala ++ repos
    val res = coursierapi.Versions.create()
      .withRepositories(fullRepos: _*)
      .withModule(coursierapi.Module.of(projId.organization, name))
      .versions()
    res.getMergedListings().getAvailable().asScala.toSeq
  }

  private lazy val previousVersionOpt: Def.Initialize[Option[String]] = Def.settingDyn {
    val ver = sbt.Keys.version.value
    Previous.previousStableVersion(ver) match {
      case Some(previousVersion) => Def.setting(Some(previousVersion))
      case None =>
        // FIXME Make that VersionCompatibility configurable
        val mini = Version(VersionCompatibility.SemVer.minimumCompatibleVersion(ver))
        Def.setting {
          val current = Version(ver)
          val previousVersions = previousVersionsFromRepo.value.map(v => Version(v))
          val compatible = previousVersions.filter(v => mini.compareTo(v) <= 0 && v.compareTo(current) < 0)
          if (compatible.isEmpty)
            None
          else
            Some(compatible.max.repr)
        }
    }
  }

  override def projectSettings = Def.settings(
    previousVersions := {
      val ver = Keys.version.value
      val firstOpt = firstVersion.value
      previousVersionOpt.value match {
        case Some(v) => Seq(v)
        case None =>
          if (firstOpt.nonEmpty)
            sys.error(s"""Cannot compute previous version from $ver. To fix that error, set set previousVersions or unset firstVersion.""")
          Nil
      }
    },
    firstVersion := None,
    MimaPlugin.autoImport.mimaPreviousArtifacts := {
      val projId = Keys.projectID.value.withExplicitArtifacts(Vector.empty)
      val previousVersions0 = previousVersions.value

      previousVersions0.toSet.map { version =>
        projId
          .withExtraAttributes {
            projId.extraAttributes
              .filter(!_._1.stripPrefix("e:").startsWith("info."))
          }
          .withRevision(version)
      }
    }
  )

}
