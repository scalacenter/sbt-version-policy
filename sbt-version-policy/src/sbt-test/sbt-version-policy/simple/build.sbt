lazy val a = project
  .settings(
    shared,
    name := "simple-test",
    libraryDependencies ++= Seq(
      "com.chuusai" %% "shapeless" % "2.3.2"
    ),
    version := "0.1.0"
  )

lazy val a1 = project
  .dependsOn(a)
  .settings(
    shared,
    name := "simple-test-foo",
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

lazy val b1 = project
  .dependsOn(b)
  .settings(
    shared,
    name := "simple-test-foo",
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
    versionPolicyDefaultReconciliation := Some(VersionCompatibility.Strict),
    checkFails,
    checkMimaPreviousArtifactsSet,
    version := "0.1.1"
  )

lazy val e = project
  .settings(
    shared,
    name := "simple-test",
    libraryDependencies ++= Seq(
      "io.get-coursier" %% "coursier" % "2.0.0-RC6-18" % Provided
    ),
    versionPolicyIgnored ++= Seq(
      "com.chuusai" %% "shapeless",
      "org.typelevel" %% "macro-compat"
    ),
    version := "0.1.1"
  )

lazy val f = project
  .settings(
    shared,
    name := "simple-test",
    libraryDependencies ++= Seq(
      "com.chuusai" %% "shapeless" % "2.3.3"
    ),
    versionPolicyDependencyRules += "com.chuusai" %% "shapeless" % "strict",
    version := "0.1.1"
  )

lazy val g = project
  .settings(
    shared,
    name := "simple-test",
    libraryDependencies ++= Seq(
      "com.chuusai" %% "shapeless" % "2.3.3"
    ),
    versionPolicyDependencyRules += "com.chuusai" %% "shapeless" % "pvp",
    version := "0.1.1"
  )

inThisBuild(List(
  scalaVersion := "2.12.11",
  organization := "io.github.alexarchambault.sbtversionpolicy.test",
))

lazy val check = taskKey[Unit]("")

lazy val shared = Def.settings(
  check := {}
)

lazy val checkFails = Def.settings(
  check := {
    check.value
    val direction = versionPolicyCheckDirection.value
    val reports = versionPolicyFindDependencyIssues.value
    val failed = reports.exists(!_._2.validated(direction))
    assert(failed, s"Expected a failed report in $reports")
  }
)

lazy val checkMimaPreviousArtifactsSet = Def.settings(
  check := {
    check.value
    val previousArtifacts = mimaPreviousArtifacts.value
    val versions = previousArtifacts.map(_.revision)
    assert(versions.nonEmpty, "No MiMa previous artifact found")
  }
)
