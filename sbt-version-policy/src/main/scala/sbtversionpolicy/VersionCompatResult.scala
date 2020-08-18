package sbtversionpolicy

import coursier.version.{ Version, VersionCompatibility }

/** Represents the compatibility between two version values.
 */
sealed trait VersionCompatResult

object VersionCompatResult {

  /** There is NO source compatibility or binary compatibility.
   */
  case object None extends VersionCompatResult

  /** Binary compatibility MUST be guranteed between the upgrade.
   */
  case object BinaryCompatible extends VersionCompatResult

  /** Binary compatibility MUST be guranteed between a patch.
   * Source compatibility [[https://docs.scala-lang.org/overviews/contributors/index.html SHOULD]]
   * be guaranteed between a patch.
   */
  case object BinaryAndSourceCompatible extends VersionCompatResult

  def apply(value1: String, value2: String, scheme: VersionCompatibility): VersionCompatResult = {
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
}
