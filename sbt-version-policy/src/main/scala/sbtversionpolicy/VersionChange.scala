package sbtversionpolicy

import coursier.version.{ Version, VersionCompatibility }

sealed trait VersionChange
object VersionChange {

  /** Major upgrade represents change in a "major" version,
   * for example `x` in `x.y.z` in SemVer where `x > 0`.
   *
   * There is NO source compatibility or binary compatibility
   * guaratee between a major upgrade.
   */
  case object MajorUpgrade extends VersionChange

  /** Minor upgrade represents change in a "minor" version,
   * for example `y` in `x.y.z` in SemVer where `x > 0`.
   *
   * Binary compatibility MUST be guranteed between a minor upgrade.
   */
  case object MinorUpgrade extends VersionChange


  /** A patch represents change in a "patch" version,
   * for example `z` in `x.y.z` in SemVer where `x > 0`.
   *
   * Binary compatibility MUST be guranteed between a patch.
   * Source compatibility [[https://docs.scala-lang.org/overviews/contributors/index.html SHOULD]]
   * be guaranteed between a patch.
   */
  case object PatchUpgrade extends VersionChange

  /** Prerelease represents version changes during development period.
   * This could include 0.y.z in SemVer or changes between release candidates.
   *
   * There is NO source compatibility or binary compatibility
   * guaratee between a prerelease change.
   */
  case object Prerelease extends VersionChange

  /** Same versions.
   */
  case object Same extends VersionChange

  def apply(value1: String, value2: String, scheme: VersionCompatibility): VersionChange = {
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
      case _ if value1 == value2 => Same
      // if the x changes in x.y.z, it's a major change in any scheme
      case _ if p_1 != n_1       => MajorUpgrade
      // handle y changes in (non-zero).y.z cases
      case _ if !p_1Zero && p_2 != n_2 =>
        if (scheme == VersionCompatibility.PackVer) MajorUpgrade
        else MinorUpgrade
      // handle z chages in (non-zero).y.z cases
      case _ if !p_1Zero && p_3 != n_3 => PatchUpgrade
      case _ if !p_1Zero               => Prerelease // handle tag changes
      // in SemVerSpec all 0.y.z changes are initial development
      case _ if p_1Zero && scheme == VersionCompatibility.SemVerSpec => Prerelease
      case _ if p_1Zero && p_2 != n_2  => MajorUpgrade
      case _ if p_1Zero && p_3 != n_3  => MinorUpgrade
      case _ => Prerelease // handle tag changes
    }
  }
}
