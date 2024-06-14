package sbtversionpolicy

import java.io.{File, FileWriter, PrintWriter}

import scala.collection.JavaConverters.*
import scala.io.Source
import scala.util.Using
import scala.util.control.NoStackTrace

import sbt.{AutoPlugin, Def, Keys, file, settingKey, taskKey}
import sbt.librarymanagement.{CrossVersion, ModuleID}
import sbt.librarymanagement.ivy.DirectCredentials

import coursier.version.{Previous, Version, VersionCompatibility}
import com.typesafe.tools.mima.plugin.MimaPlugin
import MimaPlugin.autoImport.mimaPreviousArtifacts

object SbtVersionPolicyMima extends AutoPlugin {

  override def trigger = allRequirements
  override def requires = MimaPlugin

  object autoImport {
    val versionPolicyPreviousVersions = settingKey[Either[Throwable, Seq[String]]]("Previous versions to check compatibility against.")
    val getVersionPolicyPreviousVersions = taskKey[Seq[String]]("Previous versions to check compatibility against.")
    val versionPolicyFirstVersion = settingKey[Option[String]]("First version this module was or will be published for.")
    val coursierCredentialsFile = settingKey[File]("The coursier credentials file containing credentials for resolvers used in the project.")
    val exportCredentialsToCoursier = taskKey[Unit]("Export credentials to a coursier credentials properties file")
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

    val projId = Keys.projectID.value
    val sv = Keys.scalaVersion.value
    val sbv = Keys.scalaBinaryVersion.value
    val log = Keys.sLog.value
    val name = moduleName(projId, sv, sbv)

    val ivyProps = sbtversionpolicy.internal.Resolvers.defaultIvyProperties(Keys.ivyPaths.value.ivyHome)
    val repos = Keys.resolvers.value.flatMap { res =>
      val repoOpt = sbtversionpolicy.internal.Resolvers.repository(res, ivyProps, s => System.err.println(s))
      if (repoOpt.isEmpty)
        System.err.println(s"Warning: ignoring repository ${res.name} to get previous version")
      repoOpt.toSeq
    }
    // Can't reference Keys.fullResolvers, which is a task.
    // So we're using the usual default repositories from coursier here…
    val fullRepos = coursierapi.Repository.defaults().asScala ++ repos

    val start = System.nanoTime()

    val res = coursierapi.Versions.create()
      .withRepositories(fullRepos: _*)
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

  override def buildSettings = Def.settings(
    coursierCredentialsFile := file {
      if (sys.props("os.name").toLowerCase.contains("mac"))
        s"${sys.props("user.home")}/Library/Application Support/Coursier/credentials.properties"
      else
        s"${sys.props("user.home")}/.config/coursier/credentials.properties"
    },
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

    exportCredentialsToCoursier := {
      val credentials = Keys.credentials.value
      val credentialsFile = coursierCredentialsFile.value
      val preamble = "# Created by SbtVersionPolicyMima plugin"
      val log = Keys.streams.value.log

      if (credentialsFile.exists()) {
        Using(Source.fromFile(credentialsFile)) {
          source =>
            if (!source.getLines().buffered.headOption.contains(preamble)) {
              val backup = File.createTempFile("credentials-backup-", ".properties", credentialsFile.getParentFile)
              log.info(s"Backup coursier credentials file to ${backup}")
              if (!credentialsFile.renameTo(backup)) {
                throw new RuntimeException("Failed to backup coursier credentials file") with NoStackTrace
              }
            }
        }.get
      }

      credentialsFile.getParentFile().mkdirs()

      Using(new PrintWriter(new FileWriter(credentialsFile))) {
        writer =>
          writer.println(preamble)
          credentials.zipWithIndex.collect {
            case (c: DirectCredentials, i) =>
              writer.println(s"$i.host=${c.host}")
              writer.println(s"$i.realm=${c.realm}")
              writer.println(s"$i.username=${c.userName}")
              writer.println(s"$i.password=${c.passwd}")
              writer.println()
          }
      }.get
    },
  )

}
