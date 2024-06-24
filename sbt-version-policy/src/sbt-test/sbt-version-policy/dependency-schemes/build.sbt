ThisBuild / scalaVersion := "2.13.6"
ThisBuild / organization := "ch.epfl.scala"
ThisBuild / versionPolicyPreviousVersionRepositories := CoursierDefaultRepositories

val b1 = project
  .settings(
    name := "dependency-schemes-test-b",
    version := "1.0.0",
    libraryDependencies += "org.scala-lang.modules" %% "scala-collection-compat" % "2.4.4",
  )

val b2 = project
  .settings(
    name := "dependency-schemes-test-b",
    version := "1.1.0",
    versionPolicyIntention := Compatibility.BinaryCompatible,
    libraryDependencies += "org.scala-lang.modules" %% "scala-collection-compat" % "2.5.0",
  )

val b3 = project
  .settings(
    name := "dependency-schemes-test-b",
    version := "1.1.0",
    versionPolicyIntention := Compatibility.BinaryCompatible,
    libraryDependencies += "org.scala-lang.modules" %% "scala-collection-compat" % "2.5.0",
    libraryDependencySchemes += "org.scala-lang.modules" %% "scala-collection-compat" % "pvp"
  )
