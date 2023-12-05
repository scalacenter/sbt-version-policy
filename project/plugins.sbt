addSbtPlugin("com.github.sbt" % "sbt-ci-release" % "1.5.12")

libraryDependencies += "org.scala-sbt" %% "scripted-plugin" % sbtVersion.value

addSbtPlugin("ch.epfl.scala" % "sbt-version-policy" % "3.1.0")

addSbtPlugin("io.get-coursier" % "sbt-shading" % "2.1.1")
