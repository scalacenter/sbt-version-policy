lazy val a = project
  .settings(
    shared,
    name := "simple-test",
    libraryDependencies ++= Seq(
      "com.chuusai" %% "shapeless" % "2.3.2"
    ),
    version := "0.1.0"
  )

lazy val b = project
  .settings(
    shared,
    name := "simple-test",
    libraryDependencies ++= Seq(
      "com.chuusai" %% "shapeless" % "2.3.3"
    ),
    version := "0.1.1"
  )

lazy val c = project
  .settings(
    shared,
    name := "simple-test",
    libraryDependencies ++= Seq(
      "com.chuusai" %% "shapeless" % "2.3.3"
    ),
    csrConfiguration := {
      import lmcoursier.definitions._
      csrConfiguration.value.withReconciliation(Vector(
        ModuleMatchers.only(Module(Organization("com.chuusai"), ModuleName("shapeless_2.12"), Map())) -> Reconciliation.Strict
      ))
    },
    checkFails,
    version := "0.1.1"
  )

lazy val d = project
  .settings(
    shared,
    name := "simple-test",
    libraryDependencies ++= Seq(
      "com.chuusai" %% "shapeless" % "2.3.3"
    ),
    compatibilityDefaultReconciliation := lmcoursier.definitions.Reconciliation.Strict,
    checkFails,
    version := "0.1.1"
  )

inThisBuild(List(
  scalaVersion := "2.12.11",
  organization := "io.github.alexarchambault.sbtcompatibility.test",
))

lazy val shared = Def.settings(
  compatibilityPreviousArtifacts := compatibilityAutoPreviousArtifacts.value
)

lazy val check = taskKey[Unit]("")

lazy val checkFails = Def.settings(
  check := {
    val direction = compatibilityCheckDirection.value
    val reports = compatibilityFindDependencyIssues.value
    val failed = reports.exists(!_._2.validated(direction))
    assert(failed, s"Expected a failed report in $reports")
  }
)
