# sbt-version-policy

*sbt-version-policy*:
- configures [sbt-mima](https://github.com/lightbend/mima) to guarantee that your library
  follows the [recommended versioning scheme],
- ensures that none of your dependencies are bumped or removed in an incompatible way.

## How to use

Add to your `project/plugins.sbt`:

```scala
addSbtPlugin("ch.epfl.scala" % "sbt-version-policy" % "1.0.0-RC1")
```

The latest version is [![Maven Central](https://img.shields.io/maven-central/v/ch.epfl.scala/sbt-version-policy-dummy_2.12.svg)](https://maven-badges.herokuapp.com/maven-central/ch.epfl.scala/sbt-version-policy-dummy_2.12).

sbt-version-policy depends on [sbt-mima](https://github.com/lightbend/mima), so that you don't need to explicitly
depend on it.

## `versionPolicyCheck`

The `versionPolicyCheck` task:
- runs `mimaReportBinaryIssues`,
- along with `versionPolicyReportDependencyIssues`

`versionPolicyReportDependencyIssues` itself checks for
- removed dependencies, or
- dependencies bumped in an incompatible way,

and fails if any of these checks fails.

## Automatic previous version calculation

sbt-version-policy automatically sets `mimaPreviousArtifacts`, depending on the current value of `version`, kind of like
[sbt-mima-version-check](https://github.com/ChristopherDavenport/sbt-mima-version-check) does.
The previously compatible version is computed from `version` the following way:
- drop any "metadata part" (anything after a `+`, including the `+` itself)
  - if the resulting version contains only zeros (like `0.0.0`), leave `mimaPreviousArtifacts` empty,
  - else if the resulting version does not contain a qualifier (see below), it is used in `mimaPreviousArtifacts`.
- else, drop the qualifier part, that is any suffix like `-RC1` or `-M2` or `-alpha` or `-SNAPSHOT`
  - if the resulting version ends with `.0`, `mimaPreviousArtifacts` is left empty
  - else, the last numerical part of this version is decreased by one, and used in `mimaPreviousArtifacts`.

## `versionPolicyPreviousArtifacts`

`versionPolicyReportDependencyIssues` compares the dependencies of `versionPolicyPreviousArtifacts` to the current ones.

By default, `versionPolicyPreviousArtifacts` relies on `mimaPreviousArtifacts` from sbt-mima, so that only setting / changing `mimaPreviousArtifacts` is enough for both sbt-mima and sbt-version-policy.

## Dependency compatibility adjustments

Set `versionPolicyDependencyRules` to specify whether library dependency upgrades are compatible or not. For instance:

```scala
versionPolicyDependencyRules += "org.scala-lang" % "scala-compiler" % "strict"
```

The following compatility types are available:
- `early-semver`: assumes the matched modules follow a variant of [Semantic Versioning](https://semver.org) that enforces compatibility within 0.1.z,
- `semver-spec`: assumes the matched modules follow [semantic versioning](https://semver.org),
- `pvp`: assumes the matched modules follow [package versioning policy](https://pvp.haskell.org) (quite common in Scala),
- `always`: assumes all versions of the matched modules are compatible with each other,
- `strict`: requires exact matches between the wanted and the selected versions of the matched modules.

If no rule for a module is found in `versionPolicyDependencyRules`, `versionPolicyDefaultReconciliation` is used
as a compatibility type. Its default value is `VersionCompatibility.PackVer` (package versioning policy).

## Acknowledgments

<img src="https://scala.epfl.ch/resources/img/scala-center-swirl.png" width="40px" />

*sbt-version-policy* is funded by the [Scala Center](https://scala.epfl.ch).

[recommended versioning scheme]: https://docs.scala-lang.org/overviews/core/binary-compatibility-for-library-authors.html#recommended-versioning-scheme
