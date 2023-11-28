package sbtversionpolicy

/** Incompatibilities can be binary incompatibilities or
 * source incompatibilities
 */
sealed trait IncompatibilityType

object IncompatibilityType {

  case object BinaryIncompatibility extends IncompatibilityType
  case object SourceIncompatibility extends IncompatibilityType

}
