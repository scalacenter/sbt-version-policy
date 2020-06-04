# sbt-compatibility

*sbt-compatibility*:
- configures [sbt-mima](https://github.com/lightbend/mima) automatically
- ensures that none of your dependencies are bumped or removed in an incompatible way.

## How to use

Add to your `project/plugins.sbt`:
```scala
addSbtPlugin("io.github.alexarchambault.sbt" % "sbt-compatibility" % "0.0.4")
```
The latest version is [![Maven Central](https://img.shields.io/maven-central/v/io.github.alexarchambault.sbt/sbt-compatibility-dummy.svg)](https://maven-badges.herokuapp.com/maven-central/io.github.alexarchambault.sbt/sbt-compatibility-dummy).

sbt-compatibility depends on [sbt-mima](https://github.com/lightbend/mima), so that you don't need to explicitly
depend on it.

## `compatibilityCheck`

The `compatibilityCheck` key:
- runs `mimaReportBinaryIssues`,
- along with `compatibilityReportDependencyIssues`

`compatibilityReportDependencyIssues` itself checks for
- removed dependencies, or
- dependencies bumped in an incompatible way,

and fails if any of these checks fails.

## Automatic previous version calculation

sbt-compatibility automatically sets `mimaPreviousArtifacts`, depending on the current value of `version`, kind of like
[sbt-mima-version-check](https://github.com/ChristopherDavenport/sbt-mima-version-check) does.
The previously compatible version is computed from `version` the following way:
- drop any "metadata part" (anything after a `+`, including the `+` itself)
  - if the resulting version contains only zeros (like `0.0.0`), leave `mimaPreviousArtifacts` empty,
  - else if the resulting version does not contain a qualifier (see below), it is used in `mimaPreviousArtifacts`.
- else, drop the qualifier part, that is any suffix like `-RC1` or `-M2` or `-alpha` or `-SNAPSHOT`
  - if the resulting version ends with `.0`, `mimaPreviousArtifacts` is left empty
  - else, the last numerical part of this version is decreased by one, and used in `mimaPreviousArtifacts`.

## `compatibilityPreviousArtifacts`

`compatibilityReportDependencyIssues` compares the dependencies of `compatibilityPreviousArtifacts` to the current ones.

By default, `compatibilityPreviousArtifacts` relies on `mimaPreviousArtifacts` from sbt-mima, so that only setting / changing `mimaPreviousArtifacts` is enough for both sbt-mima and sbt-compatibility.

## `compatibilityReconciliations`

`compatibilityReconciliations` allows to specify whether some version bumps are allowed or not, like
```scala
compatibilityReconciliations += "org.scala-lang.modules" %% "scala-xml" % "semver"
```

The following compatility types are available:
- `semver`: assumes the matched modules follow [semantic versioning](https://semver.org),
- `pvp`: assumes the matched modules follow [package versioning policy](https://pvp.haskell.org) (quite common in Scala),
- `always`: assumes all versions of the matched modules are compatible with each other,
- `strict`: requires exact matches between the wanted and the selected versions of the matched modules.

If no rule for a module is found in `compatibilityReconciliations`, `compatibilityDefaultReconciliation` is used
as a compatibility type. It's default value is `VersionCompatibility.PackVer` (package versioning policy).
