lazy val a = project
  .settings(
    name := "simple-test",
    libraryDependencies ++= Seq(
      "com.chuusai" %% "shapeless" % "2.3.2"
    ),
    version := "0.1.1",
    apiURL := Some(new java.net.URL("https://www.google.fr/search?q=simple+test+library+scala+0.1.0")),
    check := {
      assert(projectID.value.extraAttributes.nonEmpty)
      val prev = mimaPreviousArtifacts.value
      assert(prev.nonEmpty)
      assert(prev.forall(_.extraAttributes.isEmpty))
    }
  )

inThisBuild(List(
  scalaVersion := "2.12.11",
  organization := "io.github.alexarchambault.sbtversionpolicy.test",
))

lazy val check = taskKey[Unit]("")

