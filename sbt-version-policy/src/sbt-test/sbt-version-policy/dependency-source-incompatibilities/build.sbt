inThisBuild(List(
  scalaVersion := "2.12.11",
  libraryDependencySchemes += "org.typelevel" %% "cats-kernel" % "semver-spec",
  organization := "io.github.alexarchambault.sbtversionpolicy.test",
  versionPolicyPreviousVersionRepositories := CoursierDefaultRepositories,
))

lazy val a = project
  .settings(
    name := "source-incompat-test",
    libraryDependencies ++= Seq(
      "com.chuusai" %% "shapeless" % "2.3.2" // Uses PVP
    ),
    version := "0.1.0"
  )

lazy val b = project
  .settings(
    versionPolicyIntention := Compatibility.BinaryAndSourceCompatible,
    name := "source-incompat-test",
    libraryDependencies ++= Seq(
      "com.chuusai" %% "shapeless" % "2.3.3"
    ),
    version := "0.1.1"
  )

lazy val c = project
  .settings(
    // bumping shapeless from 2.3.2 to 2.3.3 requires breaking source compatibility
    versionPolicyIntention := Compatibility.BinaryCompatible,
    name := "source-incompat-test",
    libraryDependencies ++= Seq(
      "com.chuusai" %% "shapeless" % "2.3.3"
    ),
    version := "0.2.0"
  )

lazy val d = project
  .settings(
    name := "source-incompat-test",
    libraryDependencies ++= Seq(
      "org.typelevel" %% "cats-kernel" % "1.6.0" // Uses semver-spec
    ),
    version := "1.0.0"
  )

lazy val e = project
  .settings(
    versionPolicyIntention := Compatibility.BinaryAndSourceCompatible,
    name := "source-incompat-test",
    libraryDependencies ++= Seq(
      "org.typelevel" %% "cats-kernel" % "2.3.0"
    ),
    version := "1.0.1"
  )

lazy val f = project
  .settings(
    // bumping cats from 1.x to 2.x requires breaking binary compatibility
    versionPolicyIntention := Compatibility.None,
    name := "source-incompat-test",
    libraryDependencies ++= Seq(
      "org.typelevel" %% "cats-kernel" % "2.3.0"
    ),
    version := "2.0.0"
  )

lazy val g = project
  .settings(
    // bumping cats from 2.3.0 to 2.3.1 is binary and source compatible
    versionPolicyIntention := Compatibility.BinaryAndSourceCompatible,
    name := "source-incompat-test",
    libraryDependencies ++= Seq(
      "org.typelevel" %% "cats-kernel" % "2.3.1"
    ),
    version := "2.0.1"
  )

lazy val h = project
  .settings(
    versionPolicyIntention := Compatibility.BinaryAndSourceCompatible,
    name := "source-incompat-test",
    libraryDependencies ++= Seq(
      "org.typelevel" %% "cats-kernel" % "2.6.0"
    ),
    version := "2.0.2"
  )

lazy val i = project
  .settings(
    // bumping cats from 2.3.1 to 2.6.0 requires breaking source compatibility
    versionPolicyIntention := Compatibility.BinaryCompatible,
    name := "source-incompat-test",
    libraryDependencies ++= Seq(
      "org.typelevel" %% "cats-kernel" % "2.6.0"
    ),
    version := "2.1.0"
  )

lazy val j = project
  .settings(
    versionPolicyIntention := Compatibility.BinaryAndSourceCompatible,
    name := "source-incompat-test",
    libraryDependencies ++= Seq(
      "org.typelevel" %% "cats-kernel" % "2.3.1",
      "com.google.apis" % "google-api-services-container" % "v1-rev20220727-2.0.0"
    ),
    version := "2.1.0"
  )

lazy val k = project
  .settings(
    // bumping custom version scheme for google-api-services-container causes library issues
    versionPolicyIntention := Compatibility.BinaryAndSourceCompatible,
    name := "source-incompat-test",
    libraryDependencies ++= Seq(
      "org.typelevel" %% "cats-kernel" % "2.3.1",
      "com.google.apis" % "google-api-services-container" % "v1-rev20220826-2.0.0"
    ),
    version := "2.1.1"
  )

lazy val l = project
  .settings(
    versionPolicyIntention := Compatibility.BinaryAndSourceCompatible,
    name := "source-incompat-test",
    versionPolicyModuleVersionExtractor := {
      case m if m.name.startsWith("google-api-services") => m.revision.split('-').last
    },
    libraryDependencies ++= Seq(
      "org.typelevel" %% "cats-kernel" % "2.3.1",
      "com.google.apis" % "google-api-services-container" % "v1-rev20220826-2.0.0"
    ),
    version := "2.1.1"
  )
