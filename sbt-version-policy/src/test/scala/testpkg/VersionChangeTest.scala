package testpkg

import coursier.version.VersionCompatibility._
import sbtversionpolicy.VersionChange
import verify._

object VersionChangeTest extends BasicTestSuite {
  test("SemVer Spec") {
    assert(VersionChange("0.1.0", "0.1.0", SemVerSpec) == VersionChange.Same)
    assert(VersionChange("0.1.0", "0.1.1", SemVerSpec) == VersionChange.Prerelease)
    assert(VersionChange("0.1.0", "0.2.0", SemVerSpec) == VersionChange.Prerelease)
    assert(VersionChange("1.0.0", "1.0.0-M1", SemVerSpec) == VersionChange.Prerelease)
    assert(VersionChange("1.0.0", "1.0.1", SemVerSpec) == VersionChange.Patch)
    assert(VersionChange("1.0.0", "1.1.0", SemVerSpec) == VersionChange.MinorUpgrade)
    assert(VersionChange("1.0.0", "2.0.0", SemVerSpec) == VersionChange.MajorUpgrade)
  }

  test("Early SemVer") {
    assert(VersionChange("0.1.0", "0.1.0", SemVer) == VersionChange.Same)
    assert(VersionChange("0.1.0", "0.1.1", SemVer) == VersionChange.MinorUpgrade)
    assert(VersionChange("0.1.0", "0.2.0", SemVer) == VersionChange.MajorUpgrade)
    assert(VersionChange("1.0.0", "1.0.0-M1", SemVer) == VersionChange.Prerelease)
    assert(VersionChange("1.0.0", "1.0.1", SemVer) == VersionChange.Patch)
    assert(VersionChange("1.0.0", "1.1.0", SemVer) == VersionChange.MinorUpgrade)
    assert(VersionChange("1.0.0", "2.0.0", SemVer) == VersionChange.MajorUpgrade)
  }

  test("Pack Ver") {
    assert(VersionChange("0.1.0", "0.1.0", PackVer) == VersionChange.Same)
    assert(VersionChange("0.1.0", "0.1.1", PackVer) == VersionChange.MinorUpgrade)
    assert(VersionChange("0.1.0", "0.2.0", PackVer) == VersionChange.MajorUpgrade)
    assert(VersionChange("1.0.0", "1.0.0-M1", PackVer) == VersionChange.Prerelease)
    assert(VersionChange("1.0.0", "1.0.1", PackVer) == VersionChange.Patch)
    assert(VersionChange("1.0.0", "1.1.0", PackVer) == VersionChange.MajorUpgrade)
    assert(VersionChange("1.0.0", "2.0.0", PackVer) == VersionChange.MajorUpgrade)
  }
}
