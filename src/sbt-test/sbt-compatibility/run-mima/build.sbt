lazy val core = project
  .settings(
    crossVersion := CrossVersion.disabled,
    scalaVersion := "2.11.12",
    organization := "com.github.alexarchambault",
    name := "argonaut-shapeless_6.1",
    moduleName := name.value + "_" + scalaBinaryVersion.value,
    version := "1.1.2-SNAPSHOT",
    libraryDependencies ++= Seq(
      "com.chuusai" %% "shapeless" % "2.3.3",
      "io.argonaut" %% "argonaut" % "6.1a"
    )
  )
