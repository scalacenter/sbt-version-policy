package sbtversionpolicy

import coursier.version.{ModuleMatchers, Version, VersionCompatibility}
import lmcoursier.definitions.{ModuleMatchers => *, *}

case class DependencyCheckReport(
  compatibilityReports: Map[IncompatibilityType, Map[(String, String), DependencyCheckReport.ModuleStatus]]
) {

  def validated(incompatibilityType: IncompatibilityType): Boolean =
    compatibilityReports(incompatibilityType).forall(_._2.validated)

  def errors(incompatibilityType: IncompatibilityType, ignored: Set[(String, String)] = Set.empty): (Seq[String], Seq[String]) = {

    val relevantErrors = compatibilityReports(incompatibilityType)

    val baseErrors = relevantErrors
      .filter(!_._2.validated)
      .toVector
      .sortBy(_._1)

    def message(org: String, name: String, status: DependencyCheckReport.ModuleStatus): String = {
      s"$org:$name: ${status.message}"
    }

    val actualErrors = baseErrors.collect {
      case (orgName @ (org, name), status) if !ignored(orgName) =>
        message(org, name, status)
    }

    val warnings = baseErrors.collect {
      case (orgName @ (org, name), status) if ignored(orgName) =>
        message(org, name, status)
    }

    (warnings, actualErrors)
  }
}

object DependencyCheckReport {

  sealed abstract class ModuleStatus(val validated: Boolean) extends Product with Serializable {
    def message: String
  }
  case class SameVersion(version: String) extends ModuleStatus(true) {
    def message = s"found same version $version"
  }
  case class CompatibleVersion(version: String, previousVersion: String, reconciliation: VersionCompatibility) extends ModuleStatus(true) {
    def message = s"compatible version change from $previousVersion to $version (compatibility: ${reconciliation.name})"
  }
  case class IncompatibleVersion(version: String, previousVersion: String, reconciliation: VersionCompatibility) extends ModuleStatus(false) {
    def message = s"incompatible version change from $previousVersion to $version (compatibility: ${reconciliation.name})"
  }
  case class Missing(version: String) extends ModuleStatus(false) {
    def message = "missing dependency"
  }

  private case class SemVerVersion(major: Int, minor: Int, patch: Int, suffix: Seq[Version.Item])

  private[sbtversionpolicy] def apply(
    currentModules: Map[(String, String), String],
    previousModules: Map[(String, String), String],
    reconciliations: Seq[(ModuleMatchers, VersionCompatibility)],
    defaultReconciliation: VersionCompatibility
  ): DependencyCheckReport = {

    def report(compatibility: Compatibility) =
      moduleStatuses(compatibility, currentModules, previousModules, reconciliations, defaultReconciliation)

    DependencyCheckReport(Map(
      IncompatibilityType.BinaryIncompatibility -> report(Compatibility.BinaryCompatible),
      IncompatibilityType.SourceIncompatibility -> report(Compatibility.BinaryAndSourceCompatible)
    ))
  }

  @deprecated("This method is internal.", "1.1.0")
  def moduleStatuses(
    currentModules: Map[(String, String), String],
    previousModules: Map[(String, String), String],
    reconciliations: Seq[(ModuleMatchers, VersionCompatibility)],
    defaultReconciliation: VersionCompatibility
  ): Map[(String, String), ModuleStatus] =
    moduleStatuses(
      Compatibility.BinaryCompatible,
      currentModules,
      previousModules,
      reconciliations,
      defaultReconciliation
    )

  private def moduleStatuses(
    compatibilityIntention: Compatibility,
    currentModules: Map[(String, String), String],
    previousModules: Map[(String, String), String],
    reconciliations: Seq[(ModuleMatchers, VersionCompatibility)],
    defaultReconciliation: VersionCompatibility
  ): Map[(String, String), ModuleStatus] =
    for ((orgAndName @ (org, name), previousVersion) <- previousModules) yield {

      val status = currentModules.get(orgAndName) match {
        case None => Missing(previousVersion)
        case Some(`previousVersion`) => SameVersion(previousVersion)
        case Some(currentVersion) =>
          val reconciliation = reconciliations
            .collectFirst {
              case (matcher, rec) if matcher.matches(org, name) =>
                rec
            }
            .getOrElse(defaultReconciliation)
          val isCompatible =
            if (compatibilityIntention == Compatibility.BinaryAndSourceCompatible) {
              isBinaryCompatible(currentVersion, previousVersion, reconciliation) &&
                isSourceCompatible(currentVersion, previousVersion, reconciliation)
            } else {
              isBinaryCompatible(currentVersion, previousVersion, reconciliation)
            }
          if (isCompatible)
            CompatibleVersion(currentVersion, previousVersion, reconciliation)
          else
            IncompatibleVersion(currentVersion, previousVersion, reconciliation)
      }

      orgAndName -> status
    }

  private def isBinaryCompatible(currentVersion: String, previousVersion: String, versionCompatibility: VersionCompatibility): Boolean =
    versionCompatibility.isCompatible(previousVersion, currentVersion)

  private[sbtversionpolicy] def isSourceCompatible(currentVersion: String, previousVersion: String, versionCompatibility: VersionCompatibility): Boolean =
    versionCompatibility match {
      case VersionCompatibility.Always =>
        true
      case VersionCompatibility.Strict | VersionCompatibility.Default | VersionCompatibility.PackVer =>
        // In PVP, any release can break source compatibility
        previousVersion == currentVersion
      case VersionCompatibility.SemVer | VersionCompatibility.EarlySemVer | VersionCompatibility.SemVerSpec =>
        // Early SemVer and SemVer Spec are equivalent regarding source compatibility
        (extractSemVerNumbers(currentVersion), extractSemVerNumbers(previousVersion)) match {
          case (Some(currentSemVer), Some(previousSemVer)) =>
            def sameMajor  = currentSemVer.major == previousSemVer.major
            def sameMinor  = currentSemVer.minor == previousSemVer.minor
            def samePatch  = currentSemVer.patch == previousSemVer.patch
            def sameSuffix = currentSemVer.suffix == previousSemVer.suffix

            if (currentSemVer.major == 0) {
              // Before 1.x.y release even patch changes could be source incompatible,
              // this includes changes between snapshots and release candidates              

              sameMajor && sameMinor && samePatch && sameSuffix
            } else {
              // 1.0.0-RC1 may be source incompatible to 1.0.0-RC2
              // but!
              // 1.0.1-RC2 must be source compatible both to 1.0.1-RC1 and 1.0.0 (w/o suffix!)
              def compatPatch = (samePatch && sameSuffix) || (previousSemVer.suffix.isEmpty || previousSemVer.patch > 0)
                
              sameMajor && sameMinor && compatPatch
            }
          case _ => false
        }
    }

  private def extractSemVerNumbers(versionString: String): Option[SemVerVersion] = {
    val version = Version(versionString)
    version.items match {
      case Vector(major: Version.Number, minor: Version.Number, patch: Version.Number, suffix*) =>
        Some(SemVerVersion(major.value, minor.value, patch.value, suffix))
      case _ => 
        None // Not a semantic version number (e.g., 1.0-RC1)
    }
  }

}
