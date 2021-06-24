package sbtversionpolicy

import coursier.version.{ModuleMatchers, Version, VersionCompatibility}
import dataclass.data
import lmcoursier.definitions.{ModuleMatchers => _, _}

@data class DependencyCheckReport(
  backwardStatuses: Map[(String, String), DependencyCheckReport.ModuleStatus],
  forwardStatuses: Map[(String, String), DependencyCheckReport.ModuleStatus]
) {
  def validated(direction: Direction): Boolean =
    (!direction.backward || backwardStatuses.forall(_._2.validated)) &&
      (!direction.forward || forwardStatuses.forall(_._2.validated))

  def errors(direction: Direction, ignored: Set[(String, String)] = Set.empty): (Seq[String], Seq[String]) = {

    val backwardElems =
      if (direction.backward) backwardStatuses else Map()
    val forwardElems =
      if (direction.forward) forwardStatuses else Map()

    val baseErrors = (backwardElems.iterator.map((_, true)) ++ forwardElems.iterator.map((_, false)))
      .filter(!_._1._2.validated)
      .toVector
      .sortBy(_._1._1)

    def message(org: String, name: String, backward: Boolean, status: DependencyCheckReport.ModuleStatus): String = {
      val direction = if (backward) "backward" else "forward"
      s"$org:$name: ${status.message}"
    }

    val actualErrors = baseErrors.collect {
      case ((orgName @ (org, name), status), backward) if !ignored(orgName) =>
        message(org, name, backward, status)
    }

    val warnings = baseErrors.collect {
      case ((orgName @ (org, name), status), backward) if ignored(orgName) =>
        message(org, name, backward, status)
    }

    (warnings, actualErrors)
  }
}

object DependencyCheckReport {

  sealed abstract class ModuleStatus(val validated: Boolean) extends Product with Serializable {
    def message: String
  }
  @data class SameVersion(version: String) extends ModuleStatus(true) {
    def message = s"found same version $version"
  }
  @data class CompatibleVersion(version: String, previousVersion: String, reconciliation: VersionCompatibility) extends ModuleStatus(true) {
    def message = s"compatible version change from $previousVersion to $version (compatibility: ${reconciliation.name})"
  }
  @data class IncompatibleVersion(version: String, previousVersion: String, reconciliation: VersionCompatibility) extends ModuleStatus(false) {
    def message = s"incompatible version change from $previousVersion to $version (compatibility: ${reconciliation.name})"
  }
  @data class Missing(version: String) extends ModuleStatus(false) {
    def message = "missing dependency"
  }

  @deprecated("This method is internal.", "1.1.0")
  def apply(
    currentModules: Map[(String, String), String],
    previousModules: Map[(String, String), String],
    reconciliations: Seq[(ModuleMatchers, VersionCompatibility)],
    defaultReconciliation: VersionCompatibility
  ): DependencyCheckReport =
    apply(
      Compatibility.BinaryCompatible,
      currentModules,
      previousModules,
      reconciliations,
      defaultReconciliation
    )

  private[sbtversionpolicy] def apply(
    compatibilityIntention: Compatibility,
    currentModules: Map[(String, String), String],
    previousModules: Map[(String, String), String],
    reconciliations: Seq[(ModuleMatchers, VersionCompatibility)],
    defaultReconciliation: VersionCompatibility
  ): DependencyCheckReport = {

    // FIXME These two lines compute the same result. What is the reason for having two directions?
    val backward = moduleStatuses(compatibilityIntention, currentModules, previousModules, reconciliations, defaultReconciliation)
    val forward = moduleStatuses(compatibilityIntention, currentModules, previousModules, reconciliations, defaultReconciliation)

    DependencyCheckReport(backward, forward)
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
        extractSemVerNumbers(currentVersion).zip(extractSemVerNumbers(previousVersion)).headOption match {
          case Some(((currentMajor, currentMinor, currentPatch), (previousMajor, previousMinor, previousPatch))) =>
            currentMajor == previousMajor && {
              if (currentMajor == 0)
                currentMinor == previousMinor && currentPatch == previousPatch
              else
                currentMinor == previousMinor && currentPatch >= previousPatch
            }
          case None => currentVersion == previousVersion
        }
    }

  private def extractSemVerNumbers(versionString: String): Option[(Int, Int, Int)] = {
    val version = Version(versionString)
    if (version.items.size == 3 && version.items.forall(_.isInstanceOf[Version.Number])) {
      val Seq(major, minor, patch) = version.items.collect { case num: Version.Number => num.value }
      Some((major, minor, patch))
    } else None // Not a normalized version number (e.g., 1.0.0-RC1)
  }

}
