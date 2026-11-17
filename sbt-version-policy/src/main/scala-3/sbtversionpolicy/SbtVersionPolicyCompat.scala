package sbtversionpolicy

import sbt._
import java.io.File
import sbt.librarymanagement.{Patterns, Resolver}

private[sbtversionpolicy] object SbtVersionPolicyCompat {
  val ivyHome: Def.Initialize[Option[File]] =
    Def.setting(Keys.ivyPaths.value.ivyHome.map(new File(_)))

  def ibiblioPatterns(resolver: Resolver): Option[Patterns] = None
}
