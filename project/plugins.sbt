addSbtPlugin("com.github.sbt" % "sbt-ci-release" % "1.5.11")

libraryDependencies += "org.scala-sbt" %% "scripted-plugin" % sbtVersion.value

addSbtPlugin("ch.epfl.scala" % "sbt-version-policy" % "2.0.1")
