package sbtversionpolicy

import sbt._
import java.io.File
import org.apache.ivy.plugins.resolver.IBiblioResolver
import sbt.librarymanagement.{Patterns, RawRepository, Resolver}
import scala.collection.JavaConverters._

private[sbtversionpolicy] object SbtVersionPolicyCompat {
  val ivyHome: Def.Initialize[Option[File]] =
    Def.setting(Keys.ivyPaths.value.ivyHome)

  def ibiblioPatterns(resolver: Resolver): Option[Patterns] = {
    def strings(values: java.util.List[_]): Vector[String] =
      Option(values).map(_.asScala.toVector).getOrElse(Vector.empty).collect {
        case value: String => value
      }

    resolver match {
      case raw: RawRepository => raw.resolver match {
        case value: IBiblioResolver => Some(Patterns(
          ivyPatterns = strings(value.getIvyPatterns),
          artifactPatterns = strings(value.getArtifactPatterns),
          isMavenCompatible = value.isM2compatible,
          descriptorOptional = !value.isUseMavenMetadata,
          skipConsistencyCheck = !value.isCheckconsistency
        ))
        case _ => None
      }
      case _ => None
    }
  }
}
