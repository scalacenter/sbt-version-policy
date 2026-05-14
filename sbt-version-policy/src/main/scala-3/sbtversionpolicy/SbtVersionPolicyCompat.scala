package sbtversionpolicy

import sbt._
import java.io.File

private[sbtversionpolicy] object SbtVersionPolicyCompat {
  val ivyHome: Def.Initialize[Option[File]] =
    Def.setting(Keys.ivyPaths.value.ivyHome.map(new File(_)))
}
