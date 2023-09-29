package sbtversionpolicy

import scala.math.Ordered._

case class CompatAssessment(
  attainedCompat: Compatibility,
  failure: Option[Exception]
) {
  for {
    f <- failure
    failedCompat <- Compatibility.Levels.find(_ > attainedCompat)
  } yield (failedCompat, f)
}

object CompatAssessment {
  def apply(): CompatAssessment = {

  }
}