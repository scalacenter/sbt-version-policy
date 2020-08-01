lazy val v1 = project
  .settings(
    check := {},
    name := "defaults-test",
    libraryDependencies ++= Seq(
      "org.scala-lang.modules" %% "scala-xml" % "1.0.6",
      "com.fasterxml.jackson.core" % "jackson-databind" % "2.10.4"
    ),
    version := "0.1.0"
  )

lazy val bumpScala = project
  .settings(
    name := "defaults-test",
    libraryDependencies ++= Seq(
      "org.scala-lang.modules" %% "scala-xml" % "1.2.0",
      "com.fasterxml.jackson.core" % "jackson-databind" % "2.10.4"
    ),
    version := "0.1.1",
    checkFails
  )

lazy val bumpJava = project
  .settings(
    name := "defaults-test",
    libraryDependencies ++= Seq(
      "org.scala-lang.modules" %% "scala-xml" % "1.0.6",
      "com.fasterxml.jackson.core" % "jackson-databind" % "2.11.0"
    ),
    version := "0.1.1"
  )

lazy val bumpScalaAndAddRule = project
  .settings(
    name := "defaults-test",
    libraryDependencies ++= Seq(
      "org.scala-lang.modules" %% "scala-xml" % "1.2.0",
      "com.fasterxml.jackson.core" % "jackson-databind" % "2.10.4"
    ),
    version := "0.1.1",
    versionPolicyDependencyRules += "org.scala-lang.modules" %% "*" % "semver"
  )

inThisBuild(List(
  scalaVersion := "2.12.11",
  organization := "io.github.alexarchambault.sbtversionpolicy.test",
))

lazy val check = taskKey[Unit]("")

lazy val checkFails = Def.settings(
  check := {
    val direction = versionPolicyCheckDirection.value
    val reports = versionPolicyFindDependencyIssues.value
    val failed = reports.exists(!_._2.validated(direction))
    assert(failed, s"Expected a failed report in $reports")
  }
)

