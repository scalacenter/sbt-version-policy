package sbtversionpolicy.internal

import java.io.File

import org.apache.ivy.plugins.resolver.IBiblioResolver
import sbt.librarymanagement.{Configuration => _, MavenRepository => _, _}
import sbt.util.Logger

import scala.collection.JavaConverters._

// originally adapted from https://github.com/alexarchambault/sbt-coursier-export/blob/4e8f95cade3a07029c09809722cfae64d0066a7a/src/main/scala/sbtcoursierexport/Resolvers.scala

object Resolvers {

  def defaultIvyProperties(ivyHomeOpt: Option[File]): Map[String, String] = {

    val ivyHome = Option(System.getProperty("ivy.home"))
      .orElse(ivyHomeOpt.map(_.getAbsoluteFile.toURI.getPath))
      .getOrElse(new File(System.getProperty("user.home")).toURI.getPath + ".ivy2")

    val sbtIvyHome = sys.props.getOrElse(
      "sbt.ivy.home",
      ivyHome
    )

    Map(
      "ivy.home" -> ivyHome,
      "sbt.ivy.home" -> sbtIvyHome
    ) ++ sys.props
  }


  private def mavenCompatibleBaseOpt(patterns: Patterns): Option[String] =
    if (patterns.isMavenCompatible) {
      val baseIvyPattern = patterns.ivyPatterns.head.takeWhile(c => c != '[' && c != '(')
      val baseArtifactPattern = patterns.ivyPatterns.head.takeWhile(c => c != '[' && c != '(')

      if (baseIvyPattern == baseArtifactPattern)
        Some(baseIvyPattern)
      else
        None
    } else
      None

  private def mavenRepository(root: String): String =
    if (root.endsWith("/")) root else root + "/"

  // this handles whitespace in path
  private def pathToUriString(path: String): String = {
    "file://" + path.replaceAllLiterally(" ", "%20")
  }

  private def substituteProperties(properties: Map[String, String], input: String): String =
    properties.foldLeft(input) {
      case (inputO, (k, v)) =>
        inputO.replace(s"$${$k}", v)
    }

  def repository(
    resolver: Resolver,
    ivyProperties: Map[String, String],
    warn: String => Unit
  ): Option[coursierapi.Repository] =
    resolver match {
      case r: sbt.librarymanagement.MavenRepository =>
        Some(coursierapi.MavenRepository.of(r.root))

      case r: FileRepository
        if r.patterns.ivyPatterns.lengthCompare(1) == 0 &&
          r.patterns.artifactPatterns.lengthCompare(1) == 0 =>

        mavenCompatibleBaseOpt(r.patterns) match {
          case None =>
            Some(coursierapi.IvyRepository.of(substituteProperties(ivyProperties, pathToUriString(r.patterns.artifactPatterns.head)), substituteProperties(ivyProperties, pathToUriString(r.patterns.ivyPatterns.head))))
          case Some(mavenCompatibleBase) =>
            Some(coursierapi.MavenRepository.of(pathToUriString(mavenCompatibleBase)))
        }

      case r: URLRepository if patternMatchGuard(r.patterns) =>
        Some(coursierapi.MavenRepository.of(parseMavenCompatResolver(ivyProperties, r.patterns)))

      case raw: RawRepository if raw.name == "inter-project" => // sbt.RawRepository.equals just compares names anyway
        None

      // Pattern Match resolver-type-specific RawRepositories
      case IBiblioRepository(p) =>
        Some(coursierapi.MavenRepository.of(parseMavenCompatResolver(ivyProperties, p)))

      case other =>
        warn(s"Unrecognized repository ${other.name}, ignoring it")
        None
    }

  private object IBiblioRepository {

    private def stringVector(v: java.util.List[_]): Vector[String] =
      Option(v).map(_.asScala.toVector).getOrElse(Vector.empty).collect {
        case s: String => s
      }

    private def patterns(resolver: IBiblioResolver): Patterns = Patterns(
      ivyPatterns = stringVector(resolver.getIvyPatterns),
      artifactPatterns = stringVector(resolver.getArtifactPatterns),
      isMavenCompatible = resolver.isM2compatible,
      descriptorOptional = !resolver.isUseMavenMetadata,
      skipConsistencyCheck = !resolver.isCheckconsistency
    )

    def unapply(r: Resolver): Option[Patterns] =
      r match {
        case raw: RawRepository =>
          raw.resolver match {
            case b: IBiblioResolver =>
              Some(patterns(b))
                .filter(patternMatchGuard)
            case _ =>
              None
          }
        case _ =>
          None
      }
  }

  private def patternMatchGuard(patterns: Patterns): Boolean =
    patterns.ivyPatterns.lengthCompare(1) == 0 &&
      patterns.artifactPatterns.lengthCompare(1) == 0

  private def parseMavenCompatResolver(
    ivyProperties: Map[String, String],
    patterns: Patterns
  ): String =
    mavenCompatibleBaseOpt(patterns) match {
      case None =>
        s"ivy:${substituteProperties(ivyProperties, patterns.artifactPatterns.head)}|${substituteProperties(ivyProperties, patterns.ivyPatterns.head)}"
      case Some(mavenCompatibleBase) =>
        mavenRepository(mavenCompatibleBase)
    }
}
