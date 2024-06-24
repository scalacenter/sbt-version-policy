package sbtversionpolicy

import scala.collection.JavaConverters.*
import scala.util.control.NoStackTrace
import sbt.{AutoPlugin, Def, Keys, PluginTrigger, Plugins, settingKey, taskKey}
import sbt.KeyRanks.Invisible
import sbt.librarymanagement.{CrossVersion, ModuleID}
import coursier.version.{Previous, Version, VersionCompatibility}
import com.typesafe.tools.mima.plugin.MimaPlugin
import MimaPlugin.autoImport.mimaPreviousArtifacts

object SbtVersionPolicyMima extends AutoPlugin {

  override def trigger: PluginTrigger = allRequirements
  override def requires: Plugins = MimaPlugin && SbtVersionPolicyPlugin

  object autoImport {
    val versionPolicyPreviousVersions = settingKey[Either[Throwable, Seq[String]]]("Previous versions to check compatibility against.")
    val versionPolicyFirstVersion = settingKey[Option[String]]("First version this module was or will be published for.")
    val versionPolicyPreviousVersionRepositories = settingKey[PreviousVersionRepositories]("Repositories from which to look for previous versions of the artifact. The value can be either `CoursierDefaultRepositories` or `SbtResolvers(Set(resolverNames))`")

    sealed trait PreviousVersionRepositories
    case object CoursierDefaultRepositories extends PreviousVersionRepositories
    case class SbtResolvers(resolverNames: Set[String]) extends PreviousVersionRepositories

    private[sbtversionpolicy] val getVersionPolicyPreviousVersions =
      taskKey[Seq[String]]("Get previous versions or throw the error if it failed to resolve at load time").withRank(Invisible)
  }

  val versionPolicyInternal: SbtVersionPolicyInternalKeys =
    new SbtVersionPolicyInternalKeys {}

  import autoImport._
  import versionPolicyInternal._

  private def moduleName(crossVersion: CrossVersion, sv: String, sbv: String, baseName: String): String =
    CrossVersion(crossVersion, sv, sbv).fold(baseName)(_(baseName))

  private def moduleName(m: ModuleID, sv: String, sbv: String): String =
    moduleName(m.crossVersion, sv, sbv, m.name)

  private lazy val previousVersionsFromRepo = Def.setting[Either[Throwable, Seq[String]]] {

    val version = Keys.version.value
    val projId = Keys.projectID.value
    val sv = Keys.scalaVersion.value
    val sbv = Keys.scalaBinaryVersion.value
    val log = Keys.sLog.value
    val name = moduleName(projId, sv, sbv)

    val ivyProps = sbtversionpolicy.internal.Resolvers.defaultIvyProperties(Keys.ivyPaths.value.ivyHome)

    val repos = versionPolicyPreviousVersionRepositories.?.value match {
      case None =>
        sys.error(
          s"""Previous version cannot be calculated from the the current version $name:$version.
             |Please set `versionPolicyPreviousVersionRepositories` for the repositories from which metadata for previous versions can be downloaded.
             |Run `inspect versionPolicyPreviousVersionRepositories` for description of valid values.
             |""".stripMargin
        )

      case Some(CoursierDefaultRepositories) =>
        val repositories = coursierapi.Repository.defaults().asScala
        log.info(s"Getting previous versions of $name from coursier default repositories: ${repositories.mkString("\n", "\n", "")}")
        repositories

      case Some(SbtResolvers(resolverNames)) =>
        log.info(s"Getting previous versions of $name from resolvers: ${resolverNames.mkString("\n", "\n", "")}")
        val resolvers = Keys.resolvers.value.map(res => res.name -> res).toMap
        val previousVersionResolvers = resolverNames.collect(resolvers).toSeq
        val unrecognizedResolvers = resolverNames -- previousVersionResolvers.map(_.name)
        if (unrecognizedResolvers.nonEmpty) {
          sys.error(s"""Unrecognized resolver names: ${unrecognizedResolvers.mkString(", ")}""")
        }
        previousVersionResolvers.flatMap { res =>
          val repoOpt = sbtversionpolicy.internal.Resolvers.repository(res, ivyProps, s => System.err.println(s))
          if (repoOpt.isEmpty)
            System.err.println(s"Warning: ignoring repository ${res.name} to get previous version")
          repoOpt
        }
    }

    val start = System.nanoTime()

    val res = coursierapi.Versions.create()
      .withRepositories(repos: _*)
      .withModule(coursierapi.Module.of(projId.organization, name))
      .versions()

    log.debug(s"It took ${(System.nanoTime() - start) / 1e6f} ms to get previous versions of $name:")
    res.getListings.asScala.foreach {
      case e =>
        val repo = e.getKey
        val versions = e.getValue.toString
        log.debug(s"  $repo: $versions")
    }

    val errors = res.getErrors.asScala.collect {
      case e if !e.getValue.contains("not found:") =>
        e.getValue
    }
    if (errors.nonEmpty) {
      val msg = errors
        .map(err => s"  $err")
        .mkString("Failed to get artifact metadata from some repositories. Please fix the issues and reload.\n", "\n", "")
      log.error(msg)
      Left(new RuntimeException(msg) with NoStackTrace)
    } else {
      Right(res.getMergedListings().getAvailable().asScala)
    }
  }

  private lazy val previousVersionOpt: Def.Initialize[Either[Throwable, Option[String]]] = Def.settingDyn {
    val ver = sbt.Keys.version.value
    val compat = versionPolicyVersionCompatibility.value
    Previous.previousStableVersion(ver) match {
      case Some(previousVersion) => Def.setting(Right(Some(previousVersion)))
      case None =>
        val mini = Version(compat.minimumCompatibleVersion(ver))
        Def.setting {
          val current = Version(ver)
          previousVersionsFromRepo.value.map(_.map(Version(_))).map {
            previousVersions =>
              val compatible = previousVersions.filter(v => mini.compareTo(v) <= 0 && v.compareTo(current) < 0)
              if (compatible.isEmpty)
                None
              else
                Some(compatible.max.repr)
          }
        }
    }
  }

  override def globalSettings = Def.settings(
    versionPolicyFirstVersion := None,
  )

  override def projectSettings = Def.settings(
    versionPolicyVersionCompatibility := {
      import VersionCompatibility.{Always, Default, Strict}
      val schemeOpt = Keys.versionScheme.?.value.getOrElse(None)
      schemeOpt match {
        case Some(x) =>
          val compatOpt = VersionCompatibility(x)
          compatOpt match {
            case Some(Always) | Some(Default) | Some(Strict) | None =>
              sys.error(s"unsupported version scheme: $x")
            case Some(compat) => compat
          }
        case None => VersionCompatibility.EarlySemVer
      }
    },
    versionPolicyPreviousVersions := {
      val firstOpt = versionPolicyFirstVersion.value
      previousVersionOpt.value.map {
        case Some(v) =>
          val firstVersionCheck = firstOpt.forall(first => Version(first).compareTo(Version(v)) <= 0)
          if (firstVersionCheck) Seq(v)
          else Nil
        case None =>
          Nil
      }
    },
    getVersionPolicyPreviousVersions := {
      versionPolicyPreviousVersions.value match {
        case Left(err) => throw err
        case Right(value) => value
      }
    },

    mimaPreviousArtifacts := {
      val projId = Keys.projectID.value.withExplicitArtifacts(Vector.empty)
      val previousVersions0 = versionPolicyPreviousVersions.value.getOrElse(Nil)

      previousVersions0.toSet.map { version =>
        projId
          .withExtraAttributes {
            projId.extraAttributes
              .filter(!_._1.stripPrefix("e:").startsWith("info."))
          }
          .withRevision(version)
      }
    },
  )
}
