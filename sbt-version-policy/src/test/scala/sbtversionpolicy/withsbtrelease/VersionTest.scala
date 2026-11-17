package sbtversionpolicy.withsbtrelease

import verify._

object VersionTest extends BasicTestSuite {
  test("release versions") {
    assert(Version("1").exists(_.string == "1"))
    assert(Version("1.2.3").exists(_.string == "1.2.3"))
  }

  test("prerelease versions") {
    assert(Version("1.2.3-M1").exists(_.string == "1.2.3-M1"))
    assert(Version("1.2.3-SNAPSHOT").exists(_.string == "1.2.3-SNAPSHOT"))
  }

  test("malformed versions and integer overflow") {
    assert(Version("").isEmpty)
    assert(Version("invalid").isEmpty)
    assert(Version("2147483648.0").isEmpty)
    assert(Version("1.2147483648").isEmpty)
  }
}
