package sbtversionpolicy.internal

import com.typesafe.tools.mima.plugin.MimaPlugin.autoImport._
import sbt.Def
import sbt.Keys._
import com.typesafe.tools.mima.plugin.SbtMima

object MimaIssues {

  import com.typesafe.tools.mima.core.util.log.Logging
  import sbt.Logger

  // adapted from https://github.com/lightbend/mima/blob/fde02955c4908a6423b12edf044799a868b51706/sbtplugin/src/main/scala/com/typesafe/tools/mima/plugin/SbtLogger.scala#L8-L12
  final class SbtLogger(log: Logger) extends Logging {
    def info(msg: String): Unit = log.verbose(msg)
    def debugLog(msg: String): Unit   = log.debug(msg)
    def warn(msg: String): Unit    = log.warn(msg)
    def error(msg: String): Unit   = log.error(msg)
  }

  // adapted from https://github.com/lightbend/mima/blob/fde02955c4908a6423b12edf044799a868b51706/sbtplugin/src/main/scala/com/typesafe/tools/mima/plugin/MimaPlugin.scala#L82-L99
  def forwardBinaryIssuesIterator = Def.task {
    val log = streams.value.log
    val previousClassfiles = mimaPreviousClassfiles.value
    val currentClassfiles = mimaCurrentClassfiles.value
    val cp = fullClasspath.in(mimaFindBinaryIssues).value

    if (previousClassfiles.isEmpty)
      log.info(s"${name.value}: mimaPreviousArtifacts is empty, not analyzing binary compatibility.")

    previousClassfiles
      .iterator
      .map {
        case (moduleId, prevClassfiles) =>
          moduleId -> SbtMima.runMima(
            prevClassfiles,
            currentClassfiles,
            cp,
            "forward",
            new SbtLogger(log)
          )
      }
      .filter {
        case (_, (problems, problems0)) =>
          problems.nonEmpty || problems0.nonEmpty
      }
  }

}
