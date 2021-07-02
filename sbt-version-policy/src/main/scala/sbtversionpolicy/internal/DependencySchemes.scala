package sbtversionpolicy.internal

import coursier.version.{ModuleMatcher, ModuleMatchers, VersionCompatibility}
import lmcoursier.CoursierConfiguration
import lmcoursier.definitions.Reconciliation
import sbt.librarymanagement.{ConfigurationReport, ModuleReport}
import sbt.util.Logger

private[sbtversionpolicy] object DependencySchemes {

  def apply(
    coursierConfig: CoursierConfiguration,
    compileReport: ConfigurationReport,
    useCsrConfigReconciliations: Boolean,
    ignoreSbtDefaultReconciliations: Boolean,
    dependencies: Map[(String, String), String],
    userDefinedSchemes: Seq[(ModuleMatchers, VersionCompatibility)],
    fallbackSchemes: Seq[(ModuleMatchers, VersionCompatibility)],
    log: Logger
  ): Seq[(ModuleMatchers, VersionCompatibility)] = {
    val fromCsrConfig =
      if (useCsrConfigReconciliations) {
        if (ignoreSbtDefaultReconciliations)
          coursierConfig.reconciliation
            .filter {
              val default = sbt.coursierint.LMCoursier.relaxedForAllModules.toSet
              rule => !default(rule)
            }
        else
          coursierConfig.reconciliation
      } else
        Nil

    val fromCsrConfig0 = fromCsrConfig.map {
      case (m, r) =>
        val matcher = ModuleMatchers(
          m.exclude.map(m => ModuleMatcher(m.organization.value, m.name.value, m.attributes)),
          m.include.map(m => ModuleMatcher(m.organization.value, m.name.value, m.attributes)),
          includeByDefault = m.includeByDefault
        )
        val compatibility = r match {
          case Reconciliation.Default => VersionCompatibility.Default
          case Reconciliation.Relaxed => VersionCompatibility.Always
          case Reconciliation.SemVer  => VersionCompatibility.EarlySemVer
          case Reconciliation.Strict  => VersionCompatibility.Strict
        }
        (matcher, compatibility)
    }

    val dependenciesSchemes =
      for {
        moduleReport <- compileReport.modules
        scheme <- findVersionScheme(moduleReport)
      } yield {
        val module = moduleReport.module
        for ((_, userDefinedScheme) <- userDefinedSchemes.find(_._1.matches(module.organization, module.name))) {
          if (userDefinedScheme == scheme) {
            log.warn(s"Custom versioning scheme '${userDefinedScheme.name}' for ${module.organization}:${module.name} is the same as the versioning scheme declared in the module itself. Consider removing it from the setting libraryDependencySchemes.")
          } else {
            log.warn(s"Using custom versioning scheme '${userDefinedScheme.name}' although the module ${module.organization}:${module.name} declares the versioning scheme '${scheme.name}'.")
          }
        }
        val matchers = ModuleMatchers.only(module.organization, module.name)
        (matchers, scheme)
      }

    // Order is significant!
    userDefinedSchemes ++ dependenciesSchemes ++ fromCsrConfig0 ++ fallbackSchemes
  }

  private def findVersionScheme(moduleReport: ModuleReport): Option[VersionCompatibility] =
    moduleReport
      .extraAttributes
      .get("info.versionScheme")
      .flatMap(VersionCompatibility(_))

}
