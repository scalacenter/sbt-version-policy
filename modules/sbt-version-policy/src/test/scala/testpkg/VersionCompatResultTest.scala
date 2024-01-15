package testpkg

import coursier.version.VersionCompatibility._
import sbtversionpolicy.Compatibility
import sbtversionpolicy.Compatibility._
import verify._

object VersionCompatResultTest extends BasicTestSuite {
  test("SemVer Spec") {
    assert(Compatibility("0.1.0", "0.1.0", SemVerSpec) == BinaryAndSourceCompatible)
    assert(Compatibility("0.1.0", "0.1.1", SemVerSpec) == None)
    assert(Compatibility("0.1.0", "0.2.0", SemVerSpec) == None)
    assert(Compatibility("0.1.0", "1.0.0-M1", SemVerSpec) == None)
    assert(Compatibility("1.0.0-M1", "1.0.0", SemVerSpec) == None)
    assert(Compatibility("1.0.0", "1.0.1", SemVerSpec) == BinaryAndSourceCompatible)
    assert(Compatibility("1.0.0", "1.1.0", SemVerSpec) == BinaryCompatible)
    assert(Compatibility("1.0.0", "2.0.0", SemVerSpec) == None)
  }

  test("Early SemVer") {
    assert(Compatibility("0.1.0", "0.1.0", EarlySemVer) == BinaryAndSourceCompatible)
    assert(Compatibility("0.1.0", "0.1.1", EarlySemVer) == BinaryCompatible)
    assert(Compatibility("0.1.0", "0.2.0", EarlySemVer) == None)
    assert(Compatibility("0.1.0", "1.0.0-M1", EarlySemVer) == None)
    assert(Compatibility("1.0.0", "1.0.0-M1", EarlySemVer) == None)
    assert(Compatibility("1.0.0", "1.0.1", EarlySemVer) == BinaryAndSourceCompatible)
    assert(Compatibility("1.0.0", "1.1.0", EarlySemVer) == BinaryCompatible)
    assert(Compatibility("1.0.0", "2.0.0", EarlySemVer) == None)
  }

  test("Pack Ver") {
    assert(Compatibility("0.1.0", "0.1.0", PackVer) == BinaryAndSourceCompatible)
    assert(Compatibility("0.1.0", "0.1.1", PackVer) == BinaryCompatible)
    assert(Compatibility("0.1.0", "0.2.0", PackVer) == None)
    assert(Compatibility("0.1.0", "1.0.0-M1", PackVer) == None)
    assert(Compatibility("1.0.0", "1.0.0-M1", PackVer) == None)
    assert(Compatibility("1.0.0", "1.0.1", PackVer) == BinaryAndSourceCompatible)
    assert(Compatibility("1.0.0", "1.1.0", PackVer) == None)
    assert(Compatibility("1.0.0", "2.0.0", PackVer) == None)
  }
}
