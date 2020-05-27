package sbtcompatibility.internal

import dataclass._
import lmcoursier.definitions.Module
import lmcoursier.definitions.Organization
import lmcoursier.definitions.ModuleName

// FIXME Cut-n-pasted from https://github.com/coursier/coursier/blob/f0b10fb1744e5bdf94bf17857dfb3cb19fda2e5b/modules/coursier/shared/src/main/scala/coursier/util/ModuleMatchers.scala

@data class ModuleMatchers(
  exclude: Set[ModuleMatcher],
  include: Set[ModuleMatcher] = Set(),
  @since
  includeByDefault: Boolean = true
) {

  // If modules are included by default:
  // Those matched by anything in exclude are excluded, but for those also matched by something in include.
  // If modules are excluded by default:
  // Those matched by anything in include are included, but for those also matched by something in exclude.

  def matches(module: Module): Boolean =
    if (includeByDefault)
      !exclude.exists(_.matches(module)) || include.exists(_.matches(module))
    else
      include.exists(_.matches(module)) && !exclude.exists(_.matches(module))

  def +(other: ModuleMatchers): ModuleMatchers =
    ModuleMatchers(
      exclude ++ other.exclude,
      include ++ other.include
    )

}

object ModuleMatchers {
  def apply(matchers: lmcoursier.definitions.ModuleMatchers): ModuleMatchers =
    ModuleMatchers(
      matchers.exclude.map(ModuleMatcher(_)),
      matchers.include.map(ModuleMatcher(_)),
      matchers.includeByDefault
    )

}
