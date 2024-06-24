ThisBuild / scalaVersion := "2.13.8"
ThisBuild / resolvers += Resolver.typesafeIvyRepo("releases")
ThisBuild / versionPolicyIntention := Compatibility.None
ThisBuild / versionPolicyPreviousVersionRepositories := CoursierDefaultRepositories

val root =
  project.in(file("."))
