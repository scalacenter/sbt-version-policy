package sbtcompatibility.internal

import java.util.regex.Pattern

import dataclass.data
import lmcoursier.definitions.Module

import scala.annotation.tailrec
import scala.util.matching.Regex
import lmcoursier.definitions.Organization
import lmcoursier.definitions.ModuleName

// FIXME cut-n-pasted from https://github.com/coursier/coursier/blob/876a6604d0cd0c3783ed729f5399549f52a3a385/modules/coursier/shared/src/main/scala/coursier/util/ModuleMatcher.scala

@data class ModuleMatcher(matcher: Module) {

  import ModuleMatcher.blobToPattern

  lazy val orgPattern = blobToPattern(matcher.organization.value)
  lazy val namePattern = blobToPattern(matcher.name.value)
  lazy val attributesPattern = matcher
    .attributes
    .mapValues(blobToPattern(_))
    .iterator
    .toMap

  def matches(module: Module): Boolean =
    orgPattern.pattern.matcher(module.organization.value).matches() &&
      namePattern.pattern.matcher(module.name.value).matches() &&
      module.attributes.keySet == attributesPattern.keySet &&
      attributesPattern.forall {
        case (k, p) =>
          module.attributes.get(k).exists(p.pattern.matcher(_).matches())
      }

}

object ModuleMatcher {

  def apply(org: Organization, name: ModuleName, attributes: Map[String, String] = Map.empty): ModuleMatcher =
    ModuleMatcher(Module(org, name, attributes))

  def all: ModuleMatcher =
    ModuleMatcher(Module(Organization("*"), ModuleName("*"), Map.empty))

  @tailrec
  private def blobToPattern(s: String, b: StringBuilder = new StringBuilder): Regex =
    if (s.isEmpty)
      b.result().r
    else {
      val idx = s.indexOf('*')
      if (idx < 0) {
        b ++= Pattern.quote(s)
        b.result().r
      } else {
        if (idx > 0)
          b ++= Pattern.quote(s.substring(0, idx))
        b ++= ".*"
        blobToPattern(s.substring(idx + 1), b)
      }
    }

}
