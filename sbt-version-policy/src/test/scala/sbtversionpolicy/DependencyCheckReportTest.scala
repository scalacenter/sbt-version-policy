package sbtversionpolicy

import verify.BasicTestSuite
import DependencyCheckReport.isSourceCompatible
import coursier.version.VersionCompatibility

object DependencyCheckReportTest extends BasicTestSuite {

  def withScheme(scheme: VersionCompatibility)(f: ((String, String) => Unit, (String, String) => Unit) => Unit): Unit = {
    val isCompatible =
      (currentVersion: String, previousVersion: String) =>
        assert(isSourceCompatible(currentVersion, previousVersion, scheme))
    val isBreaking =
      (currentVersion: String, previousVersion: String) =>
        assert(!isSourceCompatible(currentVersion, previousVersion, scheme))
    f(isCompatible, isBreaking)
  }

  test("isSourceCompatible") {
    withScheme(VersionCompatibility.PackVer) { (isCompatible, isBreaking) =>
      isBreaking  ("1.0.1",     "1.0.0")
      isBreaking  ("1.1.0",     "1.0.0")
      isCompatible("1.2.3-RC1", "1.2.3-RC1")
      isBreaking  ("1.2.3",     "1.2.3-RC1")
      isCompatible("1.2.3",     "1.2.3")
      isBreaking  ("2.0.0",     "1.0.0")
    }
    withScheme(VersionCompatibility.EarlySemVer) { (isCompatible, isBreaking) =>
      isBreaking  ("0.1.1",          "0.1.0")
      isBreaking  ("0.2.0",          "0.1.0")
      isCompatible("1.0.0-RC1",      "1.0.0-RC1")
      isBreaking  ("1.0.0-RC2",      "1.0.0-RC1")
      isBreaking  ("1.0.0",          "1.0.0-RC1")
      isCompatible("1.0.1-RC1",      "1.0.0")
      isCompatible("1.0.1-RC2",      "1.0.1-RC1")    
      isBreaking  ("1.0.1",          "1.0.0-RC1")
      isCompatible("1.0.1",          "1.0.0")
      isBreaking  ("1.1.0-RC1",      "1.0.0")
      isBreaking  ("1.1.0-RC2",      "1.1.0-RC1")
      isBreaking  ("1.1.0",          "1.0.0-RC1")
      isBreaking  ("1.1.0",          "1.0.0")
      isCompatible("1.2.1-SNAPSHOT", "1.2.0")
      isBreaking  ("2.0.0-RC1",      "1.0.0")
      isBreaking  ("2.0.0",          "1.0.0-RC1")
      isBreaking  ("2.0.0",          "1.0.0")
    }
  }

}
