package sbtversionpolicy

import com.typesafe.tools.mima.plugin.{MimaPlugin, SbtMima}
import coursier.version.{ModuleMatchers, Version, VersionCompatibility}
import sbt.{Def, *}
import sbt.Keys.*
import sbt.librarymanagement.CrossVersion
import lmcoursier.CoursierDependencyResolution
import sbtversionpolicy.internal.{DependencyCheck, DependencySchemes, MimaIssues}
import sbtversionpolicy.SbtVersionPolicyMima.autoImport.*

import scala.math.Ordering.Implicits._
import scala.util.Try

object SbtVersionPolicySettings {

  private val keys: SbtVersionPolicyKeys = new SbtVersionPolicyKeys {}
  import keys._
  import keys.versionPolicyInternal._

  def updateSettings = Def.settings(
    versionPolicyCsrConfiguration := csrConfiguration.value
      // TODO Make that a method on CoursierConfiguration
      .withInterProjectDependencies(Vector.empty)
      .withFallbackDependencies(Vector.empty)
      .withSbtScalaOrganization(None)
      .withSbtScalaVersion(None)
      .withSbtScalaJars(Vector.empty)
      .withExcludeDependencies(Vector.empty)
      .withForceVersions(Vector.empty)
      .withReconciliation(Vector.empty),
    versionPolicyDependencyResolution := CoursierDependencyResolution(versionPolicyCsrConfiguration.value),
    versionPolicyUpdateConfiguration := updateConfiguration.value,
    versionPolicyUnresolvedWarningConfiguration := (update / unresolvedWarningConfiguration).value,
    versionPolicyScalaModuleInfo := scalaModuleInfo.value
  )

  // Trying to mimick the current default behavior of evicted in sbt, that is
  // - assume scala libraries follow PVP,
  // - assume Java libraries follow semver.
  private def defaultSchemes = Seq(
    ("*" % "*" % "pvp").cross(CrossVersion.full),
    "*" %% "*" % "pvp",
    "*" % "*" % "early-semver"
  )

  def reconciliationGlobalSettings = Def.settings(
    versionPolicyCheckDirection := Direction.backward,
    versionPolicyIgnoreSbtDefaultReconciliations := true,
    versionPolicyUseCsrConfigReconciliations := true,
    versionPolicyDefaultDependencySchemes := defaultSchemes,
    versionPolicyIgnored := Seq.empty,
    versionPolicyDefaultScheme := None,
    versionPolicyIgnoredInternalDependencyVersions := None,
    versionPolicyModuleVersionExtractor := PartialFunction.empty
  )

  def reconciliationSettings = Def.settings(
    versionPolicyDependencySchemes ++= libraryDependencySchemes.value,
    versionPolicyFallbackReconciliations := {
      val sv = scalaVersion.value
      val sbv = scalaBinaryVersion.value
      val defaultReconciliationOpt = versionPolicyDefaultScheme.value
      val (fallbackRules, fallbackMatchers) = {
        val rules: Seq[ModuleID] = versionPolicyDefaultDependencySchemes.value
        defaultReconciliationOpt match {
          case None => (rules, Nil)
          case Some(default) => (Nil, Seq(ModuleMatchers.all -> default))
        }
      }
      fallbackRules.map { mod =>
        val rec = VersionCompatibility(mod.revision) match {
          case Some(r) => r
          case None => sys.error(s"Unrecognized reconciliation '${mod.revision}' in $mod")
        }
        val name = CrossVersion(mod.crossVersion, sv, sbv).fold(mod.name)(_(mod.name))
        val matchers = ModuleMatchers.only(mod.organization, name)
        (matchers, rec)
      } ++ fallbackMatchers
    },
    versionPolicyDetailedReconciliations := {
      val sv = scalaVersion.value
      val sbv = scalaBinaryVersion.value
      versionPolicyDependencySchemes.value.map { mod =>
        val rec = VersionCompatibility(mod.revision) match {
          case Some(r) => r
          case None => sys.error(s"Unrecognized reconciliation '${mod.revision}' in $mod")
        }
        val name = CrossVersion(mod.crossVersion, sv, sbv).fold(mod.name)(_(mod.name))
        val matchers = ModuleMatchers.only(mod.organization, name)
        (matchers, rec)
      }
    }
  )

  def previousArtifactsSettings = Def.settings(
    versionPolicyPreviousArtifactsFromMima := {
      import Ordering.Implicits._
      MimaPlugin.autoImport.mimaPreviousArtifacts.value
        .toVector
        .map { mod =>
          val splitVersion = mod.revision.split('.').map(s => Try(s.toInt).getOrElse(-1)).toSeq
          (splitVersion, mod)
        }
        .sortBy(_._1)
        .map(_._2)
    },

    versionPolicyPreviousArtifacts := versionPolicyPreviousArtifactsFromMima.value
  )

  def findIssuesSettings = Def.settings(
    versionPolicyFindDependencyIssues := {
      val log = streams.value.log
      val sv = scalaVersion.value
      val sbv = scalaBinaryVersion.value

      val compileReport = update.value.configuration(Compile).getOrElse {
        sys.error("Compile configuration not found in update report")
      }

      val compatibilityIntention =
        versionPolicyIntention.?.value
          .getOrElse(throw new MessageOnlyException("Please set the key versionPolicyIntention to declare the compatibility you want to check"))
      val depRes = versionPolicyDependencyResolution.value
      val scalaModuleInf = versionPolicyScalaModuleInfo.value
      val updateConfig = versionPolicyUpdateConfiguration.value
      val warningConfig = versionPolicyUnresolvedWarningConfiguration.value
      val excludedModules = ignoredModulesOfCurrentBuild.value
      val extractVersion = versionPolicyModuleVersionExtractor.value

      val currentDependencies = DependencyCheck.modulesOf(compileReport, excludedModules, sv, sbv, extractVersion, log)

      val reconciliations =
        DependencySchemes(
          csrConfiguration.value,
          compileReport,
          versionPolicyUseCsrConfigReconciliations.value,
          versionPolicyIgnoreSbtDefaultReconciliations.value,
          currentDependencies,
          versionPolicyDetailedReconciliations.value,
          versionPolicyFallbackReconciliations.value,
          log
        )

      val previousModuleIds = versionPolicyPreviousArtifacts.value

      // Skip dependency check if no compatibility is intended
      if (compatibilityIntention == Compatibility.None) Nil else {

        previousModuleIds.map { previousModuleId =>

          val report0 = DependencyCheck.report(
            compatibilityIntention,
            excludedModules,
            currentDependencies,
            previousModuleId,
            reconciliations,
            VersionCompatibility.Strict,
            sv,
            sbv,
            depRes,
            scalaModuleInf,
            updateConfig,
            warningConfig,
            extractVersion,
            log
          )

          (previousModuleId, report0)
        }
      }
    },
    versionPolicyReportDependencyIssues := {
      val log = streams.value.log
      val sv = scalaVersion.value
      val sbv = scalaBinaryVersion.value
      val direction = versionPolicyCheckDirection.value
      val reports = versionPolicyFindDependencyIssues.value
      val intention =
        versionPolicyIntention.?.value
          .getOrElse(throw new MessageOnlyException("Please set the key versionPolicyIntention to declare the compatibility you want to check"))
      val currentModule = projectID.value
      val formattedPreviousVersions = formatVersions(versionPolicyPreviousVersions.value)

      val ignored = versionPolicyIgnored.value
        .map { orgName =>
          val mod = orgName % "foo"
          val name = CrossVersion(mod.crossVersion, sv, sbv).fold(mod.name)(_(mod.name))
          (mod.organization, name)
        }
        .toSet

      var anyError = false
      for ((previousModule, report) <- reports) {
        val (warnings, errors) = report.errors(direction, ignored)
        if (errors.nonEmpty) {
          anyError = true
          log.error(s"Incompatibilities with dependencies of ${nameAndRevision(previousModule)}")
          for (error <- errors)
            log.error("  " + error)
        }
      }

      if (anyError)
        throw new MessageOnlyException(s"Dependencies of module ${nameAndRevision(currentModule)} break the intended compatibility guarantees 'Compatibility.${intention}' (see messages above). You have to relax your compatibility intention by changing the value of versionPolicyIntention.")
      else {
        if (intention == Compatibility.None) {
          log.info(s"Not checking dependencies compatibility of module ${nameAndRevision(currentModule)} because versionPolicyIntention is set to 'Compatibility.None'")
        } else {
          log.info(s"Module ${nameAndRevision(currentModule)} has no dependency issues with ${formattedPreviousVersions} (versionPolicyIntention is set to 'Compatibility.${intention}')")
        }
      }
    },
    versionCheck := Def.ifS((versionCheck / skip).toTask)(Def.task {
      ()
    })(Def.task {
      val intention =
        versionPolicyIntention.?.value
          .getOrElse(throw new MessageOnlyException("Please set the key versionPolicyIntention to declare the compatibility guarantees of this release"))
      val versionValue = version.value
      val versionNumber = VersionNumber(versionValue)
      val s = streams.value
      val moduleName = projectID.value.name

      if (Compatibility.isValidVersion(intention, versionNumber)) {
        s.log.info(s"Module ${moduleName} has a valid version number: $versionValue (versionPolicyIntention is set to 'Compatibility.${intention}')")
      } else {
        val detail =
          if (intention == Compatibility.None) {
            val matchingIntention =
              if (versionNumber._3.contains(0)) Compatibility.BinaryCompatible else Compatibility.BinaryAndSourceCompatible
            "If you want to publish a binary incompatible release, you must increment the major version number (or the minor version number, if the major version is 0). " +
            s"Otherwise, to release version ${versionValue}, you need to restrict the versionPolicyIntention to ${matchingIntention} and run versionPolicyCheck again."
          } else {
            "If you want to publish a source incompatible release, you must increment the minor version number. " +
            s"Otherwise, to release version ${versionValue}, you need to restrict the versionPolicyIntention to ${Compatibility.BinaryAndSourceCompatible} and run versionPolicyCheck again."
          }
        throw new MessageOnlyException(s"Module ${moduleName} has a declared version number ${versionValue} that does not conform to its declared versionPolicyIntention of ${intention}. $detail")
      }
    }).value,
    versionPolicyCheck := Def.ifS((versionPolicyCheck / skip).toTask)(Def.task {
      ()
    })(Def.task {
      val ignored1 = versionPolicyMimaCheck.value
      val ignored2 = versionPolicyReportDependencyIssues.value
    }).value,
    versionPolicyForwardCompatibilityCheck := {
      import MimaPlugin.autoImport._
      val it = MimaIssues.forwardBinaryIssuesIterator.value
      it.foreach {
        case (moduleId, problems) =>
          SbtMima.reportModuleErrors(
            moduleId,
            problems._1,
            problems._2,
            true,
            mimaBinaryIssueFilters.value,
            mimaBackwardIssueFilters.value,
            mimaForwardIssueFilters.value,
            Keys.streams.value.log,
            name.value,
          )
      }
    },
    versionPolicyVersionCompatResult := {
      val ver = version.value
      val prevs = versionPolicyPreviousVersions.value
      if (prevs.nonEmpty) {
        val maxPrev = prevs.map(Version(_)).max.repr
        val compat = versionPolicyVersionCompatibility.value
        Compatibility(maxPrev, ver, compat)
      }
      else Compatibility.None
    },
    versionAssessMimaCompatibility := {
      /*
      When being used for an Intention check, we want:
      - the compatibility level attained
      - the exception we got trying to attain that compatability level, if any (which would be from the level *above* the attained level...)

      We want to say "project failed the binary compatability check with exception waawaah"

      log.error(s"Module ${nameAndRevision(currentModule)} is not {intended} compatible with ${formattedPreviousVersions}. You have to relax your compatibility intention by changing the value of versionPolicyIntention.")
      throw new MessageOnlyException(error.directCause.map(_.toString).getOrElse("versionPolicyForwardCompatibilityCheck failed"))

      result.attainsCompatability = BinaryCompatible
      result.failedToAttain = Some((SourceAndBinaryCompatible, exception))

       */
      lastPopulatedValueOf(Compatibility.Levels.toList.flatMap { level =>
        level.checkThatMustPassForCompatabilityLevel.map(_.result.map(_.toEither.toOption.map(_ => level)))
      }).map(_.getOrElse(Compatibility.None)).value
    },
    versionPolicyMimaCheck := Def.taskDyn {
      val intendedCompatibility =
        versionPolicyIntention.?.value
          .getOrElse(throw new MessageOnlyException("Please set the key versionPolicyIntention to declare the compatibility you want to check"))
      val log = streams.value.log
      val currentModule = nameAndRevision(projectID.value)
      val formattedPreviousVersions = formatVersions(versionPolicyPreviousVersions.value)
      val actualCompat = versionAssessMimaCompatibility.value
      val actualCompat2 = versionAssessMimaCompatibility.value
      val actualCompat3 = versionAssessMimaCompatibility.value

      println(s"actualCompat=$actualCompat")

      Def.task {
        log.info(if (intendedCompatibility == Compatibility.None) {
          s"Not checking compatibility of module $currentModule because versionPolicyIntention is set to 'Compatibility.None'"
        } else {
          s"Module $currentModule is ${actualCompat.shortDescription} compatible with $formattedPreviousVersions"
        })
      }
    }.value
  )

  private def lastPopulatedValueOf[B](tasks: List[Def.Initialize[Task[Option[B]]]]): Def.Initialize[Task[Option[B]]] = {
    tasks match {
      case Nil => Def.task(None)
      case x :: xs =>
        Def.task {
          val tv = x.value
          if (tv.isDefined) lastPopulatedValueOf(xs).value.orElse(tv)
          else None
        }
    }
  }

  def skipSettings = Seq(
    versionCheck / skip := (publish / skip).value,
    versionPolicyCheck / skip := (publish / skip).value
  )

  def schemesGlobalSettings = Seq(
    versionPolicyDependencySchemes := Seq.empty,
    versionScheme := Some("early-semver")
  )

  /** All the modules (as pairs of organization name and artifact name) defined
    * by the current build definition, and whose version number matches the regex
    * defined by the key `versionPolicyIgnoredInternalDependencyVersions`.
    */
  private val ignoredModulesOfCurrentBuild: Def.Initialize[Set[(String, String)]] = Def.settingDyn {
    val allProjectRefs = loadedBuild.value.allProjectRefs
    versionPolicyIgnoredInternalDependencyVersions.value match {
      case Some(versionRegex) =>
        Def.uniform {
          allProjectRefs.map { case (projectRef, _) =>
            Def.setting {
              val projectOrganization = (projectRef / organization).value
              val projectName = (projectRef / moduleName).value
              val projectVersion = (projectRef / version).value
              val projectCrossVersion = (projectRef / crossVersion).value
              val projectScalaVersion = (projectRef / scalaVersion).value
              val projectScalaBinaryVersion = (projectRef / scalaBinaryVersion).value
              if (versionRegex.findFirstMatchIn(projectVersion).isDefined) {
                val nameWithBinarySuffix =
                  CrossVersion(projectCrossVersion, projectScalaVersion, projectScalaBinaryVersion)
                    .fold(projectName)(_ (projectName))
                val module = projectOrganization -> nameWithBinarySuffix
                Some(module)
              } else {
                // Don’t include the module if its version does not match the regex
                None
              }
            }
          }
        }(_.flatten.toSet)
      case None =>
        // versionPolicyIgnoredInternalDependencyVersions is unset
        Def.setting(Set.empty)
    }
  }

  private def nameAndRevision(moduleID: ModuleID): String = s"${moduleID.name}:${moduleID.revision}"

  private def formatVersions(previousVersions: Seq[String]): String =
    if (previousVersions.size == 1) s"version ${previousVersions.head}"
    else s"versions ${previousVersions.mkString(", ")}"

}
