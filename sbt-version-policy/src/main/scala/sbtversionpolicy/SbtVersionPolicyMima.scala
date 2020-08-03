package sbtversionpolicy

import com.typesafe.tools.mima.plugin.MimaPlugin
import coursier.version.{ Previous, Version, VersionCompatibility }
import sbt.{AutoPlugin, Def, Keys, settingKey}
import sbt.librarymanagement.{ CrossVersion, ModuleID }
import scala.collection.JavaConverters._
import MimaPlugin.autoImport.mimaPreviousArtifacts

object SbtVersionPolicyMima extends AutoPlugin {

  override def trigger = allRequirements
  override def requires = MimaPlugin

  object autoImport {
    val versionPolicyPreviousVersions = settingKey[Seq[String]]("Previous versions to check compatibility against.")
    val versionPolicyFirstVersion = settingKey[Option[String]]("First version this module was or will be published for.")
    val versionScheme = settingKey[Option[String]]("Version scheme used for this build: early-semver, pvp, semver-spec")     
  }
  val versionPolicyInternal: SbtVersionPolicyInternalKeys =
    new SbtVersionPolicyInternalKeys {}

  import autoImport._
  import versionPolicyInternal._

  private def moduleName(crossVersion: CrossVersion, sv: String, sbv: String, baseName: String): String =
    CrossVersion(crossVersion, sv, sbv).fold(baseName)(_(baseName))
  private def moduleName(m: ModuleID, sv: String, sbv: String): String =
    moduleName(m.crossVersion, sv, sbv, m.name)

  private lazy val previousVersionsFromRepo = Def.setting {

    val projId = Keys.projectID.value
    val sv = Keys.scalaVersion.value
    val sbv = Keys.scalaBinaryVersion.value
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
    val res = coursierapi.Versions.create()
      .withRepositories(fullRepos: _*)
      .withModule(coursierapi.Module.of(projId.organization, name))
      .versions()
    res.getMergedListings().getAvailable().asScala.toSeq
  }

  private lazy val previousVersionOpt: Def.Initialize[Option[String]] = Def.settingDyn {
    val ver = sbt.Keys.version.value
    val compat = versionPolicyVersionCompatibility.value
    Previous.previousStableVersion(ver) match {
      case Some(previousVersion) => Def.setting(Some(previousVersion))
      case None =>
        val mini = Version(compat.minimumCompatibleVersion(ver))
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

  override def globalSettings = Def.settings(
    versionPolicyFirstVersion := None,
  )

  override def projectSettings = Def.settings(
    versionPolicyVersionCompatibility := {
      val schemeOpt = versionScheme.?.value.getOrElse(None)
      schemeOpt match {
        case Some("semver") =>
          sys.error(s"""versionScheme 'semver' is ambiguous.
                       |Based on the Semantic Versioning 2.0.0, 0.y.z updates are all initial development and thus
                       |0.6.0 and 0.6.1 would NOT maintain any compatibility, but in Scala ecosystem it is
                       |common to start adopting binary compatibility even in 0.y.z releases.
                       |
                       |Specify 'early-semver' for the early variant.
                       |Specify 'semver-spec' for the Spec-correct SemVer.""".stripMargin)
        case Some("early-semver") => VersionCompatibility.SemVer
        case Some("semver-spec")  => VersionCompatibility.SemVerSpec
        case Some("pvp")          => VersionCompatibility.PackVer
        case Some(x)              => sys.error(s"unknown versionScheme '$x'")
        case None                 => VersionCompatibility.SemVer
      }
    },
    versionPolicyPreviousVersions := {
      val ver = Keys.version.value
      val firstOpt = versionPolicyFirstVersion.value
      previousVersionOpt.value match {
        case Some(v) =>
          val firstVersionCheck = firstOpt.forall(first => Version(first).compareTo(Version(v)) <= 0)
          if (firstVersionCheck) Seq(v)
          else Nil
        case None =>
          if (firstOpt.nonEmpty)
            sys.error(s"""Cannot compute previous version from $ver. To fix that error, set previousVersions or unset addedIn.""")
          Nil
      }
    },

    mimaPreviousArtifacts := {
      val projId = Keys.projectID.value.withExplicitArtifacts(Vector.empty)
      val previousVersions0 = versionPolicyPreviousVersions.value

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
