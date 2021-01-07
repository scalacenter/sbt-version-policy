# sbt-version-policy

*sbt-version-policy*:
- configures [sbt-mima](https://github.com/lightbend/mima) to guarantee that your library
  follows the [recommended versioning scheme],
- ensures that none of your dependencies are bumped or removed in an incompatible way.

## Install

Add to your `project/plugins.sbt`:

```scala
addSbtPlugin("ch.epfl.scala" % "sbt-version-policy" % "<version>")
```

The latest version is [![Maven Central](https://img.shields.io/maven-central/v/ch.epfl.scala/sbt-version-policy-dummy_2.12.svg)](https://maven-badges.herokuapp.com/maven-central/ch.epfl.scala/sbt-version-policy-dummy_2.12).

sbt-version-policy depends on [sbt-mima](https://github.com/lightbend/mima), so that you don't need to explicitly
depend on it.

## Configure

The plugin introduces a new key, `versionPolicyIntention`, that you need
to set to the level of compatibility that your next release is intended
to provide. It can take the following three values:

- ~~~ scala
  // Your next release will provide no compatibility guarantees with the
  // previous one.
  ThisBuild / versionPolicyIntention := Compatibility.None
  ~~~
- ~~~ scala
  // Your next release will be binary compatible with the previous one,
  // but it may not be source compatible.
  versionPolicyIntention := Compatibility.BinaryCompatible
  ~~~
- ~~~ scala
  // Your next release will be both binary compatible and source compatible
  // with the previous one.
  versionPolicyIntention := Compatibility.BinaryAndSourceCompatible
  ~~~

## Use

### Check that pull requests don’t break the intended compatibility level

In your CI server, run the task `versionPolicyCheck` on pull requests.

~~~
$ sbt versionPolicyCheck
~~~

This task checks that the PR does not break the compatibility guarantees
claimed by your `versionPolicyIntention`. For instance, if your intention
is to have `BinaryAndSourceCompatible` changes, the task
`versionPolicyCheck` will fail if the PR breaks binary compatibility
or source compatibility.

### Check that release version numbers are valid with respect to the compatibility guarantees they provide

Before you cut a release, run the task `versionCheck`.

~~~
$ sbt versionCheck
~~~

Note: make sure that the `version` is set to the new release version
number before you run `versionCheck`.

This task checks that the release version number is consistent with the
intended compatibility level as per `versionPolicyIntention`. For instance,
if your intention is to publish a release that breaks binary compatibility,
the task `versionCheck` will fail if you didn’t bump the major version
number.

## How does `versionPolicyCheck` work?

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
