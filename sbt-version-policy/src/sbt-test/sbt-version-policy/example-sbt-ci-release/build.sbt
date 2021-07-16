inThisBuild(List(
  organization := "org.organization",
  homepage := Some(url("https://github.com/organization/project")),
  licenses := List("Apache-2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0")),
  developers := List(
    Developer(
      "julienrf",
      "Julien Richard-Foy",
      "julien@organization.org",
      url("https://github.com/julienrf")
    )
  ),
  scalaVersion := "3.0.1",
  versionPolicyIntention := Compatibility.BinaryAndSourceCompatible
))

val module = project

val root = project.in(file("."))
  .aggregate(module)
  .settings(publish / skip := true)

// Unfortunately, sbt-ci-release can only be configured via environment variables,
// and I couldn’t find a way to change the environment variables with sbt-scripted,
// so I have to resort to this hack.
Global / onLoad := {
  val previous = (Global / onLoad).value
  val configureCiRelease = { (s: State) =>
    val env = System.getenv()
    val field = env.getClass.getDeclaredField("m")
    field.setAccessible(true)
    val writeableEnv = field.get(env).asInstanceOf[java.util.Map[String, String]]
    writeableEnv.put("CI_RELEASE", "+publishLocal") // Publish locally for our tests only, in practice you will publish to Sonatype
    writeableEnv.put("CI_SONATYPE_RELEASE", "")
    writeableEnv.put("PGP_PASSPHRASE", "")
    writeableEnv.put("PGP_SECRET", "")
    writeableEnv.put("SONATYPE_PASSWORD", "")
    writeableEnv.put("SONATYPE_USERNAME", "")
    writeableEnv.put("CI_COMMIT_TAG", "dummy")
    s
  }
  configureCiRelease compose previous
}
