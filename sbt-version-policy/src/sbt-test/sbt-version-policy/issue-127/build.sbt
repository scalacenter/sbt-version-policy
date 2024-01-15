ThisBuild / scalaVersion := "2.13.8"
ThisBuild / resolvers += Resolver.typesafeIvyRepo("releases")
ThisBuild / versionPolicyIntention := Compatibility.None

val root =
  project.in(file("."))
