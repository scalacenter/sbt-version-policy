package sbtversionpolicy

import sbt.*
import com.typesafe.tools.mima.core.Problem
import upickle.core.LinkedHashMap

/**
 * @param moduleReport     Compatibility report for one module
 * @param submoduleReports Compatibility reports for the aggregated submodules
 */
case class CompatibilityReport(
  moduleReport: Option[CompatibilityModuleReport],
  submoduleReports: Option[(Compatibility, Seq[CompatibilityReport])]
)

/**
 * @param previousRelease  Module ID of the previous release of this module, against which the compatibility was assessed
 * @param compatibility    Assessed compatibility level based on both dependency issues and API issues
 * @param dependencyIssues Dependency issues found for this module
 * @param apiIssues        API issues (ie, Mima issue) found for this module
 */
case class CompatibilityModuleReport(
  previousRelease: ModuleID,
  compatibility: Compatibility,
  dependencyIssues: DependencyCheckReport,
  apiIssues: Seq[(IncompatibilityType, Problem)]
)

object CompatibilityReport {

  def write(
    targetFile: File,
    compatibilityReport: CompatibilityReport,
    log: Logger,
    compatibilityLabel: Compatibility => String = defaultCompatibilityLabels
  ): Unit = {
    IO.createDirectory(targetFile.getParentFile)
    IO.write(targetFile, ujson.write(toJson(compatibilityReport, compatibilityLabel), indent = 2))
    log.info(s"Wrote compatibility report in ${targetFile.absolutePath}")
  }

  // Human readable description of the compatibility levels
  val defaultCompatibilityLabels: Compatibility => String = {
    case Compatibility.None                      => "Incompatible"
    case Compatibility.BinaryCompatible          => "Binary compatible"
    case Compatibility.BinaryAndSourceCompatible => "Binary and source compatible"
  }

  private def toJson(report: CompatibilityReport, compatibilityLabel: Compatibility => String): ujson.Value = {
    val fields = LinkedHashMap[String, ujson.Value]()
    report.moduleReport.foreach { moduleReport =>
      fields ++= Seq(
        "module-name" -> moduleReport.previousRelease.name,
        "previous-version" -> moduleReport.previousRelease.revision,
        "compatibility" -> toJson(moduleReport.compatibility, compatibilityLabel),
        // TODO add issue details
        // "issues" -> ujson.Obj("dependencies" -> ujson.Arr(), "api" -> ujson.Obj())
      )
    }
    report.submoduleReports.foreach { case (aggregatedCompatibility, submoduleReports) =>
      fields += "aggregated" -> ujson.Obj(
        "compatibility" -> toJson(aggregatedCompatibility, compatibilityLabel),
        "modules" -> ujson.Arr(submoduleReports.map(toJson(_, compatibilityLabel))*)
      )
    }
    ujson.Obj(fields)
  }

  private def toJson(compatibility: Compatibility, compatibilityLabel: Compatibility => String): ujson.Value =
    ujson.Obj(
      "value" -> (compatibility match {
        case Compatibility.None                      => ujson.Str("incompatible")
        case Compatibility.BinaryCompatible          => ujson.Str("binary-compatible")
        case Compatibility.BinaryAndSourceCompatible => ujson.Str("binary-and-source-compatible")
      }),
      "label" -> ujson.Str(compatibilityLabel(compatibility))
    )

}
