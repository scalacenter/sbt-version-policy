package sbtversionpolicy

import com.typesafe.tools.mima.core.Problem
import com.typesafe.tools.mima.plugin.MimaPlugin
import coursier.version.{ModuleMatchers, VersionCompatibility}
import sbt.*
import sbt.Keys.*
import sbt.librarymanagement.CrossVersion
import lmcoursier.CoursierDependencyResolution
import sbtversionpolicy.internal.{DependencyCheck, DependencySchemes, MimaIssues}
import sbtversionpolicy.SbtVersionPolicyMima.autoImport.*

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
    versionPolicyIgnoreSbtDefaultReconciliations := true,
    versionPolicyUseCsrConfigReconciliations := true,
    versionPolicyDefaultDependencySchemes := defaultSchemes,
    versionPolicyIgnored := Seq.empty,
    versionPolicyDefaultScheme := None,
    versionPolicyIgnoredInternalDependencyVersions := None,
    versionPolicyModuleVersionExtractor := PartialFunction.empty
  )

  def reconciliationSettings = Def.settings(
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
      libraryDependencySchemes.value.map { mod =>
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
          .getOrElse(Compatibility.BinaryAndSourceCompatible) // If not defined, report all the possible incompatibilities
      val depRes = versionPolicyDependencyResolution.value
      val scalaModuleInf = versionPolicyScalaModuleInfo.value
      val updateConfig = versionPolicyUpdateConfiguration.value
      val warningConfig = versionPolicyUnresolvedWarningConfiguration.value
      val excludedModules = ignoredModulesOfCurrentBuild.value
      val extractVersion = versionPolicyModuleVersionExtractor.value

      log.debug(s"Computing module dependencies excluding ${excludedModules}")
      val currentDependencies = DependencyCheck.modulesOf(compileReport, excludedModules, sv, sbv, extractVersion, log)
      log.debug(s"Computed dependencies: ${currentDependencies.keySet}")

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
      val reports = versionPolicyFindDependencyIssues.value
      val intention =
        versionPolicyIntention.?.value
          .getOrElse(throw new MessageOnlyException("Please set the key versionPolicyIntention to declare the compatibility you want to check"))
      val currentModule = projectID.value
      val formattedPreviousVersions = formatVersions(versionPolicyPreviousVersions.value)

      if (intention == Compatibility.None) {
        log.info(s"Not checking dependencies compatibility of module ${nameAndRevision(currentModule)} because versionPolicyIntention is set to 'Compatibility.None'")
      } else {

        val ignored = versionPolicyIgnored.value
          .map { orgName =>
            val mod = orgName % "foo"
            val name = CrossVersion(mod.crossVersion, sv, sbv).fold(mod.name)(_(mod.name))
            (mod.organization, name)
          }
          .toSet

        val incompatibilityType =
          if (intention == Compatibility.BinaryCompatible) IncompatibilityType.BinaryIncompatibility
          else IncompatibilityType.SourceIncompatibility

        var anyError = false
        for ((previousModule, report) <- reports) {
          val (warnings, errors) = report.errors(incompatibilityType, ignored)
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

    // For every previous module, returns a list of problems paired with the type of incompatibility
    versionPolicyFindMimaIssues := Def.taskDyn[Seq[(ModuleID, Seq[(IncompatibilityType, Problem)])]] {
      val compatibility =
        versionPolicyIntention.?.value.getOrElse(Compatibility.BinaryAndSourceCompatible)
      compatibility match {
        case Compatibility.None =>
          Def.task { Nil }
        case Compatibility.BinaryCompatible | Compatibility.BinaryAndSourceCompatible =>
          Def.task {
            MimaIssues.binaryIssuesIterator.value.map { case (previousModule, (binaryIncompatibilities, sourceIncompatibilities)) =>
              def annotatedBinaryIncompatibilities = binaryIncompatibilities.map(IncompatibilityType.BinaryIncompatibility -> _)
              def annotatedSourceIncompatibilities = sourceIncompatibilities.map(IncompatibilityType.SourceIncompatibility -> _)
              val incompatibilities =
                if (compatibility == Compatibility.BinaryCompatible) annotatedBinaryIncompatibilities
                else annotatedBinaryIncompatibilities ++ annotatedSourceIncompatibilities
              previousModule -> incompatibilities
            }.toSeq
          }
      }
    }.value,

    versionPolicyMimaCheck := Def.taskDyn {
      import Compatibility.*
      val compatibility =
        versionPolicyIntention.?.value
          .getOrElse(throw new MessageOnlyException("Please set the key versionPolicyIntention to declare the compatibility you want to check"))
      val log = streams.value.log
      val currentModule = projectID.value
      val formattedModule = nameAndRevision(currentModule)

      compatibility match {
        case BinaryCompatible | BinaryAndSourceCompatible =>
          Def.task {
            val issues = versionPolicyFindMimaIssues.value
            val formattedCompatibility = if (compatibility == BinaryCompatible) "binary" else "binary and source"
            var hadErrors = false
            for ((previousModule, problems) <- issues) {
              val formattedPreviousModule = nameAndRevision(previousModule)
              if (problems.isEmpty) {
                log.info(s"Module ${formattedModule} is ${formattedCompatibility} compatible with ${formattedPreviousModule}")
              } else {
                val formattedProblems =
                  problems.map { case (incompatibilityType, problem) =>
                    val affected = incompatibilityType match {
                      case IncompatibilityType.BinaryIncompatibility => "current"
                      case IncompatibilityType.SourceIncompatibility => "previous"
                    }
                    val howToFilter = problem.howToFilter.fold("")(hint => s"\n     filter with: ${hint}")
                    s"   * ${problem.description(affected)}${howToFilter}"
                  }.mkString("\n")
                log.error(
                  s"""Module ${formattedModule} is not ${formattedCompatibility} compatible with ${formattedPreviousModule}.
                     |You have to relax our compatibility intention by changing the value of versionPolicyIntention, or to fix the incompatibilities.
                     |We found the following incompatibilities:
                     |${formattedProblems}""".stripMargin)
                hadErrors = true
              }
            }
            if (hadErrors) {
              throw new MessageOnlyException("versionPolicyMimaCheck failed")
            }
          }
        case None => Def.task {
          // skip Mima if no compatibility is intended
          log.info(s"Not checking compatibility of module ${formattedModule} because versionPolicyIntention is set to 'Compatibility.None'")
        }
      }
    }.value,

    versionPolicyFindIssues := Def.ifS((versionPolicyFindIssues / skip).toTask)(Def.task {
      streams.value.log.debug("Not finding incompatibilities with previous releases because 'versionPolicyFindIssues / skip' is 'true'")
      Seq.empty[(ModuleID, (DependencyCheckReport, Seq[(IncompatibilityType, Problem)]))]
    })(
      Def.ifS[Seq[(ModuleID, (DependencyCheckReport, Seq[(IncompatibilityType, Problem)]))]](Def.task {
        versionPolicyPreviousVersions.value.isEmpty
      })(Def.task {
        throw new MessageOnlyException("Unable to find compatibility issues because versionPolicyPreviousVersions is empty.")
      })(Def.task {
        versionPolicyPreviousVersions.value
        val dependencyIssues = versionPolicyFindDependencyIssues.value
        val mimaIssues = versionPolicyFindMimaIssues.value
        assert(
          dependencyIssues.map(_._1.revision).toSet == mimaIssues.map(_._1.revision).toSet,
          "Dependency issues and Mima issues must be checked against the same previous releases"
        )
        for ((previousModule, dependencyReport) <- dependencyIssues) yield {
          val (_, problems) =
            mimaIssues
              .find { case (id, _) => previousModule.revision == id.revision }
              .get // See assertion above
          previousModule -> (dependencyReport, problems)
        }
      })
    ).value,

    versionPolicyAssessCompatibility := Def.ifS((versionPolicyAssessCompatibility / skip).toTask)(Def.task {
      streams.value.log.debug("Not assessing the compatibility with previous releases because 'versionPolicyAssessCompatibility / skip' is 'true'")
      Seq.empty[(ModuleID, Compatibility)]
    })(Def.task {
      // Results will be flawed if the `versionPolicyIntention` is set to `BinaryCompatible` or `None`
      // because `versionPolicyFindIssues` only reports the issues that violate the intended compatibility level
      if (versionPolicyIntention.?.value.exists(_ != Compatibility.BinaryAndSourceCompatible)) {
        throw new MessageOnlyException("versionPolicyIntention should not be set when you run versionPolicyAssessCompatibility.")
      }
      val issues = versionPolicyFindIssues.value
      issues.map { case (previousRelease, (dependencyIssues, mimaIssues)) =>
        previousRelease -> Compatibility.fromIssues(dependencyIssues, mimaIssues)
      }
    }).value,

    versionPolicyExportCompatibilityReport := {
      val log = streams.value.log
      val compatibilityReport = versionPolicyCollectCompatibilityReports.value
      val targetFile =
        versionPolicyCompatibilityReportPath.?.value
          .getOrElse(crossTarget.value / "compatibility-report.json")
      CompatibilityReport.write(targetFile, compatibilityReport, log)
    },

    versionPolicyCollectCompatibilityReports := Def.ifS(Def.task {
      (versionPolicyCollectCompatibilityReports / skip).value
    })(Def.task {
      CompatibilityReport(None, None)
    })(Def.taskDyn {
      val module = thisProjectRef.value
      val submodules = thisProject.value.aggregate
      val log = streams.value.log

      // Compatibility report of the current module
      val maybeModuleReport =
        Def.task {
          val issues = (module / versionPolicyFindIssues).value
          if (issues.size > 1) {
            log.warn(s"Ignoring compatibility reports with versions ${issues.drop(1).map(_._1.revision).mkString(", ")} for module ${issues.head._1.name}. Remove this warning by setting 'versionPolicyPreviousVersions' to a single previous version.")
          }
          issues.headOption.map {
            case (previousRelease, (dependencyIssues, apiIssues)) =>
              val compatibility = Compatibility.fromIssues(dependencyIssues, apiIssues)
              CompatibilityModuleReport(previousRelease, compatibility, dependencyIssues, apiIssues)
          }
        }

      // Compatibility reports of the aggregated modules (recursively computed)
      val maybeAggregatedReports = Def.ifS[Option[(Compatibility, Seq[CompatibilityReport])]]({
          Def.task { submodules.isEmpty }
        })(Def.task {
          None
        })(SbtVersionPolicyPlugin.aggregatedCompatibility(submodules, log) { submodule =>
          Def.task {
            (submodule / versionPolicyCollectCompatibilityReports).value
          }
        } { compatibilityReport =>
          compatibilityReport.moduleReport.map(_.compatibility)
        }.map(Some(_)))

      Def.task {
        CompatibilityReport(
          maybeModuleReport.value,
          maybeAggregatedReports.value
        )
      }
    }).value
  )

  def skipSettings = Seq(
    versionCheck / skip := (publish / skip).value,
    versionPolicyCheck / skip := (publish / skip).value,
    versionPolicyFindIssues / skip := (publish / skip).value,
    versionPolicyAssessCompatibility / skip := (publish / skip).value,
  )

  def schemesGlobalSettings = Seq(
    versionScheme := Some("early-semver")
  )

  val exportGlobalSettings: Seq[Def.Setting[?]] = Seq(
    // Default [aggregation behavior](https://www.scala-sbt.org/1.x/docs/Multi-Project.html#Aggregation)
    // is disabled for the “export” tasks because they handle
    // their aggregated projects by themselves
    versionPolicyExportCompatibilityReport / aggregate := false,
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
              val isSbtPlugin = (projectRef / sbtPlugin).value
              if (versionRegex.findFirstMatchIn(projectVersion).isDefined) {
                // Our goal is to compute the set of submodule names that should be excluded
                // from dependency checks.
                // For some reason, the compilation report returned by sbt adds a Scala binary
                // version suffix to the module names except for sbt plugins.
                val nameWithBinarySuffix =
                  if (isSbtPlugin) projectName
                  else CrossVersion(projectCrossVersion, projectScalaVersion, projectScalaBinaryVersion)
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
