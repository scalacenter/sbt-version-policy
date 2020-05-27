package sbtcompatibility.version

import java.util.regex.Pattern

object Version {

  private val tagPattern = "-[A-Za-z]".r

  private def previousStableVersion(version: String): Option[String] = {

    val stripped = tagPattern.findFirstMatchIn(version)
      .fold(version)(m => version.take(m.start))

    for {
      idx <- Some(stripped.lastIndexOf('.'))
      if idx >= 0
      (base, lastPart) = stripped.splitAt(idx + 1)
      if lastPart.nonEmpty && lastPart.forall(_.isDigit)
      num = lastPart.toInt
      if num > 0
    } yield s"$base${num - 1}"
  }

  def latestCompatibleWith(version: String): Option[String] =
    if (version.contains('+')) {
      val stripped = version.takeWhile(_ != '+')
      if (tagPattern.findAllMatchIn(stripped).isEmpty)
        Some(stripped)
      else
        previousStableVersion(stripped)
    } else
      previousStableVersion(version)

  /** Given a version like "X.Y.Z", returns Some((X, Y)) */
  def prefix(version: String): Option[(Int, Int)] =
    if (!version.headOption.exists(_.isDigit) || version.exists(c => !c.isDigit && c != '.'))
      None
    else {
      val Array(majStr, minStr, _ @ _*) = version.split(Pattern.quote("."), 3)
      Some((majStr.toInt, minStr.toInt))
    }

}
