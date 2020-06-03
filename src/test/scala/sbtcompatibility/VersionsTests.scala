package sbtcompatibility

import lmcoursier.definitions.Reconciliation
import sbtcompatibility.version.Version
import utest._

object VersionsTests extends TestSuite {

  val tests = Tests {

    "latestCompatible" - {

      def check(version: String, expected: String): Unit = {
        val res = Version.latestCompatibleWith(version)
        assert(res == Some(expected))
      }
      def checkEmpty(version: String): Unit = {
        val res = Version.latestCompatibleWith(version)
        assert(res == None)
      }

      * - check("1.1", "1.0")
      * - check("1.1+3", "1.1")
      * -  check("1.1-RC2", "1.0")
      * - checkEmpty("1.0-RC2")
      * - checkEmpty("1.0-RC2+43")
      * - checkEmpty("0.0.0+3-70919203-SNAPSHOT")
    }

    "compatible" - {
      * - {
        val compatible = Version.compatible(Reconciliation.SemVer, "0.1.0", "0.1.0+foo")
        assert(compatible)
      }
    }
  }

}
