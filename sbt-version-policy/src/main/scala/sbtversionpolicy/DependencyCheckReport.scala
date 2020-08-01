package sbtversionpolicy

import coursier.version.{ModuleMatchers, VersionCompatibility}
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


  def apply(
    currentModules: Map[(String, String), String],
    previousModules: Map[(String, String), String],
    reconciliations: Seq[(ModuleMatchers, VersionCompatibility)],
    defaultReconciliation: VersionCompatibility
  ): DependencyCheckReport = {

    val backward = moduleStatuses(currentModules, previousModules, reconciliations, defaultReconciliation)
    val forward = moduleStatuses(currentModules, previousModules, reconciliations, defaultReconciliation)

    DependencyCheckReport(backward, forward)
  }

  def moduleStatuses(
    currentModules: Map[(String, String), String],
    previousModules: Map[(String, String), String],
    reconciliations: Seq[(ModuleMatchers, VersionCompatibility)],
    defaultReconciliation: VersionCompatibility
  ): Map[(String, String), ModuleStatus] =
    for ((orgName @ (org, name), ver) <- previousModules) yield {

      val status = currentModules.get(orgName) match {
        case None => Missing(ver)
        case Some(`ver`) => SameVersion(ver)
        case Some(currentVersion) =>
          val reconciliation = reconciliations
            .collectFirst {
              case (matcher, rec) if matcher.matches(org, name) =>
                rec
            }
            .getOrElse(defaultReconciliation)
          if (reconciliation.isCompatible(ver, currentVersion))
            CompatibleVersion(currentVersion, ver, reconciliation)
          else
            IncompatibleVersion(currentVersion, ver, reconciliation)
      }

      orgName -> status
    }
}
