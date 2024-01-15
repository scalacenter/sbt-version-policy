// For using the plugins in their own build
Compile / unmanagedSourceDirectories ++= Seq(
  (ThisBuild / baseDirectory).value.getParentFile / "modules" / "sbt-version-policy" / "src" / "main" / "scala",
)

addSbtPlugin("com.typesafe" % "sbt-mima-plugin" % "1.1.3")
libraryDependencies += "io.get-coursier" % "interface" % "1.0.19"
libraryDependencies += "io.get-coursier" %% "versions" % "0.3.1"
libraryDependencies += "com.lihaoyi" %% "ujson" % "3.1.4" // FIXME shade