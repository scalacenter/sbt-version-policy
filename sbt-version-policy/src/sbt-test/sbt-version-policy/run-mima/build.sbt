lazy val core = project
  .settings(
    crossVersion := CrossVersion.disabled,
    scalaVersion := "2.12.21",
    organization := "com.github.alexarchambault",
    name := "argonaut-shapeless_6.3",
    moduleName := name.value + "_" + scalaBinaryVersion.value,
    version := "1.3.2-SNAPSHOT",
    libraryDependencies ++= Seq(
      "com.chuusai" %% "shapeless" % "2.3.9",
      "io.argonaut" %% "argonaut" % "6.3.8"
    ),
    versionPolicyIntention := Compatibility.BinaryCompatible
  )
