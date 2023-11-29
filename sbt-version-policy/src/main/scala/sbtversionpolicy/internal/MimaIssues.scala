package sbtversionpolicy.internal

import com.typesafe.tools.mima.MimaInternals
import com.typesafe.tools.mima.core.Problem
import com.typesafe.tools.mima.plugin.MimaPlugin.autoImport.*
import com.typesafe.tools.mima.plugin.MimaPlugin.binaryIssuesFinder
import sbt.{Def, Task}

private[sbtversionpolicy] object MimaIssues {

  val binaryIssuesIterator: Def.Initialize[Task[Iterator[(sbt.ModuleID, (List[Problem], List[Problem]))]]] = Def.task {
    val binaryIssueFilters = mimaBackwardIssueFilters.value
    val sourceIssueFilters = mimaForwardIssueFilters.value
    val issueFilters       = mimaBinaryIssueFilters.value
    val previousClassfiles = mimaPreviousClassfiles.value

    binaryIssuesFinder.value.runMima(previousClassfiles, "both")
      .map { case (previousModule, (binaryIssues, sourceIssues)) =>
        val moduleRevision = previousModule.revision
        val filteredBinaryIssues = binaryIssues.filter(MimaInternals.isProblemReported(moduleRevision, issueFilters, binaryIssueFilters))
        val filteredSourceIssues = sourceIssues.filter(MimaInternals.isProblemReported(moduleRevision, issueFilters, sourceIssueFilters))
        previousModule -> (filteredBinaryIssues, filteredSourceIssues)
      }
  }

}
