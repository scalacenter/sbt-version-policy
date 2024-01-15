package com.typesafe.tools.mima

import com.typesafe.tools.mima.core.{Problem, ProblemFilter, ProblemReporting}

// Access the internals of Mima and use them internally. NOT INTENDED for users.
// See https://github.com/lightbend/mima/pull/793
object MimaInternals {
  def isProblemReported(
    version: String,
    filters: Seq[ProblemFilter],
    versionedFilters: Map[String, Seq[ProblemFilter]]
  )(problem: Problem): Boolean =
    ProblemReporting.isReported(version, filters, versionedFilters)(problem)

}
