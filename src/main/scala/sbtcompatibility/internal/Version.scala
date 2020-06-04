package sbtcompatibility.internal

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
          // assume versions like '0.0.0' aren't published
          .filter(_.exists(c => c.isDigit && c != '0'))
      else
        previousStableVersion(stripped)
    } else
      previousStableVersion(version)

}
