lazy val a = project
  .settings(
    name := "simple-test",
    libraryDependencies ++= Seq(
      "org.scala-lang.modules" %% "scala-xml" % "1.0.6"
    ),
    version := "0.1.0"
  )

lazy val b = project
  .settings(
    name := "simple-test",
    libraryDependencies ++= Seq(
      "org.scala-lang.modules" %% "scala-xml" % "1.2.0"
    ),
    version := "0.1.1"
  )

inThisBuild(List(
  scalaVersion := "2.12.11",
  organization := "io.github.alexarchambault.sbtversionpolicy.test2",
  versionPolicyDependencyRules += "org.scala-lang.modules" %% "scala-xml" % "early-semver"
))

