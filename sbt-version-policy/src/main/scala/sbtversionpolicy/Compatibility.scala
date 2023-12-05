package sbtversionpolicy

import com.typesafe.tools.mima.core.Problem
import coursier.version.{Version, VersionCompatibility}
import sbt.VersionNumber

/** Compatibility level between two version values.
 */
sealed trait Compatibility

object Compatibility {

  /** There is NO source compatibility or binary compatibility.
   */
  case object None extends Compatibility

  /** Binary compatibility only.
   */
  case object BinaryCompatible extends Compatibility

  /** Binary and source compatibility.
   */
  case object BinaryAndSourceCompatible extends Compatibility

  def apply(value1: String, value2: String, scheme: VersionCompatibility): Compatibility = {
    def get(idx: Int, items: Vector[Version.Item]) =
      if (items.size > idx) items(idx)
      else Version.Number(0)
    val v1 = Version(value1)
    val v2 = Version(value2)
    // flip the order so parameter ordering doesn't matter
    val (p, n) =
      if (v1 <= v2) (v1, v2)
      else (v2, v1)
    val pNums = p.items.takeWhile(_.isNumber)
    val p_1 = get(0, pNums)
    val p_1Zero = p_1.compareToEmpty == 0
    val p_2 = get(1, pNums)
    val p_3 = get(2, pNums)
    val nNums = n.items.takeWhile(_.isNumber)
    val n_1 = get(0, nNums)
    val n_2 = get(1, nNums)
    val n_3 = get(2, nNums)

    p_1 match {
      case _ if value1 == value2 => BinaryAndSourceCompatible
      // if the x changes in x.y.z, it's a major change in any scheme
      case _ if p_1 != n_1       => None
      // handle y changes in (non-zero).y.z cases
      case _ if !p_1Zero && p_2 != n_2 =>
        if (scheme == VersionCompatibility.PackVer) None
        else BinaryCompatible
      // handle z chages in (non-zero).y.z cases
      case _ if !p_1Zero && p_3 != n_3 => BinaryAndSourceCompatible
      case _ if !p_1Zero               => None // handle tag changes
      // in SemVerSpec all 0.y.z changes are initial development
      case _ if p_1Zero && scheme == VersionCompatibility.SemVerSpec => None
      case _ if p_1Zero && p_2 != n_2  => None
      case _ if p_1Zero && p_3 != n_3  => BinaryCompatible
      case _ => None // handle tag changes
    }
  }

  def fromIssues(dependencyIssues: DependencyCheckReport, apiIssues: Seq[(IncompatibilityType, Problem)]): Compatibility = {
    if (
      dependencyIssues.validated(IncompatibilityType.SourceIncompatibility) &&
        apiIssues.isEmpty
    ) {
      Compatibility.BinaryAndSourceCompatible
    } else if (
      dependencyIssues.validated(IncompatibilityType.BinaryIncompatibility) &&
        !apiIssues.exists(_._1 == IncompatibilityType.BinaryIncompatibility)
    ) {
      Compatibility.BinaryCompatible
    } else {
      Compatibility.None
    }
  }

  /**
   * Validates that the given new `version` matches the claimed `compatibility` level.
   * @return Some validation error, or None if the version is valid.
   */
  def isValidVersion(compatibility: Compatibility, version: String): Boolean =
    isValidVersion(compatibility, VersionNumber(version))

  /**
   * Validates that the given new `version` matches the claimed `compatibility` level.
   * @return Some validation error, or None if the version is valid.
   */
  private[sbtversionpolicy] def isValidVersion(compatibility: Compatibility, versionNumber: VersionNumber): Boolean = {
    val major = versionNumber._1
    val minor = versionNumber._2
    val patch = versionNumber._3
    compatibility match {
      case Compatibility.None =>
        // No compatibility is guaranteed: the major version must be incremented (or the minor version, if major is 0)
        if (major.contains(0)) patch.contains(0) // minor version bump
        else minor.contains(0) && patch.contains(0) // major version bump
      case Compatibility.BinaryCompatible =>
        // No source compatibility is guaranteed, the minor version must be incremented (or the patch version, if major is 0)
        if (major.contains(0)) true // always OK
        else patch.contains(0) // minor version bump
      case Compatibility.BinaryAndSourceCompatible =>
        // OK, the version can be set to whatever
        true
    }
  }

  // Ordering from the least compatible to the most compatible
  implicit val ordering: Ordering[Compatibility] =
    Ordering.by {
      case Compatibility.None                      => 0
      case Compatibility.BinaryCompatible          => 1
      case Compatibility.BinaryAndSourceCompatible => 3
    }

}
