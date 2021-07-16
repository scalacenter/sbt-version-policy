# sbt-version-policy

sbt-version-policy helps library maintainers to follow the [recommended versioning scheme].
This plugin:

- configures [MiMa] to check for binary or source incompatibilities,
- ensures that none of your dependencies are bumped or removed in an incompatible way.

## Install

Make sure your project uses a version of sbt higher than 1.5.0.

Add to your `project/plugins.sbt`:

```scala
addSbtPlugin("ch.epfl.scala" % "sbt-version-policy" % "<version>")
```

The latest version is ![Scaladex](https://index.scala-lang.org/scalacenter/sbt-version-policy/sbt-version-policy/latest.svg).

sbt-version-policy depends on [MiMa], so that you don't need to explicitly
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
  ThisBuild / versionPolicyIntention := Compatibility.BinaryCompatible
  ~~~
- ~~~ scala
  // Your next release will be both binary compatible and source compatible
  // with the previous one.
  ThisBuild / versionPolicyIntention := Compatibility.BinaryAndSourceCompatible
  ~~~

The plugin uses [MiMa] to check for incompatibilities with the previous
release. The previous release version is automatically computed from
the current value of the `version` key in your build. This means that
you have to set this key to the _next_ version you want to release:

~~~
// Next version will be 1.1.0
ThisBuild / version := "1.1.0"
~~~

In case you use a plugin like [sbt-dynver], which automatically sets
the `version` based on the Git status, you have nothing to do (the
`version` set by sbt-dynver will just work with sbt-version-policy).

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

## How to integrate with `sbt-ci-release`?

sbt-version-policy itself uses sbt-version-policy and [sbt-ci-release](https://github.com/olafurpg/sbt-ci-release).
You can have a look at our [Github workflow](./.github/workflows/ci.yml) as an example of integration.

The key step is to run the task `versionCheck` before running the command `ci-release` (assuming the task
`versionPolicyCheck` has run already, in another step of the CI pipeline):

~~~ yaml
steps
  - name: Release
    run: sbt versionCheck ci-release
~~~

You can also have a look at the test [example-sbt-ci-release](./sbt-version-policy/src/sbt-test/sbt-version-policy/example-sbt-ci-release)
for a minimalistic sbt project using both sbt-version-policy and sbt-ci-release.

## How to integrate with `sbt-release`?

[sbt-release](https://github.com/sbt/sbt-release) is able to run sophisticated release pipelines
including running the tests, setting the release version, publishing the artifacts, and pushing
a Git tag named after the release version.

You can have a look at the test [example-sbt-release](./sbt-version-policy/src/sbt-test/sbt-version-policy/example-sbt-release)
for an example of sbt project using both sbt-version-policy and sbt-release.

This example project customizes sbt-release to:

1. Compute the release version based on its compatibility guarantees (as per `versionPolicyIntention`).
   
   We achieve this by setting `releaseVersion` like the following:

   ~~~ scala
   releaseVersion := {
     val maybeBump = versionPolicyIntention.value match {
        case Compatibility.None                      => Some(Version.Bump.Major)
        case Compatibility.BinaryCompatible          => Some(Version.Bump.Minor)
        case Compatibility.BinaryAndSourceCompatible => None // No need to bump the patch version, because it has already been bumped when sbt-release set the next release version
      }
      { (currentVersion: String) =>
        val versionWithoutQualifier =
          Version(currentVersion)
            .getOrElse(versionFormatError(currentVersion))
            .withoutQualifier
        (maybeBump match {
          case Some(bump) => versionWithoutQualifier.bump(bump)
          case None       => versionWithoutQualifier
        }).string
      }
   }
   ~~~

2. Run `versionCheck` after setting the release version, by adding the following
   release step:

   ~~~ scala
   releaseStepCommand("versionCheck")
   ~~~ 

3. Reset `versionPolicyIntention` to `Compatibility.BinaryAndSourceCompatible` after
   every release.
   
   We achieve this by managing the setting `versionPolicyIntention` in a separate file
   (like sbt-release manages the setting `version` in a separate file, by default),
   and by adding a step that overwrites the content of that file and commits it.

## How does `versionPolicyCheck` work?

The `versionPolicyCheck` task:

- checks that there are no binary or source incompatibilities
  between the current state of the project and the previous
  release (it uses `mimaReportBinaryIssues` under the hood),
- and, that no dependencies of your project have been removed
  or bumped in an incompatible way (it uses a subtask
  `versionPolicyReportDependencyIssues` under the hood).

The task `versionPolicyCheck` fails if any of these checks fails.

### Automatic previous version calculation

sbt-version-policy automatically sets `mimaPreviousArtifacts`, depending on the current value of `version`, kind of like
[sbt-mima-version-check](https://github.com/ChristopherDavenport/sbt-mima-version-check) does.
The previously compatible version is computed from `version` the following way:

- if it contains "metadata" (anything after a `+`, including the `+` itself), drop the
  metadata part
  - if the resulting version contains only zeros (like `0.0.0`), leave `mimaPreviousArtifacts` empty,
  - else if the resulting version does not contain a qualifier (see below), it is used in
    `mimaPreviousArtifacts`. For instance, if `version` is `1.0.0+3-abcd1234`, then
    `mimaPreviousArtifacts` will contain the artifacts of version `1.0.0`.
- else, drop the qualifier part, that is any suffix like `-RC1` or `-M2` or `-alpha` or `-SNAPSHOT`
  - if the resulting version ends with `.0.0`, which corresponds to a major version bump
    like `1.0.0`, or `2.0.0`, `mimaPreviousArtifacts` is left empty,
  - else, this is a minor or patch version bump, so the last numerical part of this version
    is decreased by one, and used in `mimaPreviousArtifacts`. For instance, if `version` is
    `1.2.0`, then `mimaPreviousArtifacts` will contain the artifacts of version `1.1.0`, and
    if `version` is `1.2.3`, then `mimaPreviousArtifacts` will contain the artifacts of
    version `1.2.2`.

You can see the value of the previous version computed by the plugin by inspecting the key
`versionPolicyPreviousVersions`.

### Source incompatibilities detection

[MiMa] can only detect binary incompatibilities. To detect source incompatibilities, this
plugin uses MiMa in forward mode as an approximation. This is not always correct and may
lead to false positives or false negatives. This is a known limitation of the current
implementation.

### Incompatibilities caused by removed or bumped dependencies

The subtask `versionPolicyReportDependencyIssues` checks that you did not remove or
bump your dependencies in an incompatible way. For instance, if your intention for
the next release is to keep binary compatibility, you can only bump your dependencies
to binary compatible versions.

`versionPolicyReportDependencyIssues` compares the dependencies of `versionPolicyPreviousArtifacts` to the current ones.

By default, `versionPolicyPreviousArtifacts` relies on `mimaPreviousArtifacts` from sbt-mima, so that only setting / changing `mimaPreviousArtifacts` is enough for both sbt-mima and sbt-version-policy.

### Dependency compatibility adjustments

Set `libraryDependencySchemes` to specify the versioning scheme used by your libraries.
For instance:

```scala
libraryDependencySchemes += "org.scala-lang" % "scala-compiler" % "strict"
```

The following compatibility types are available:
- `early-semver`: assumes the matched modules follow a variant of [Semantic Versioning](https://semver.org) that enforces compatibility within 0.1.z,
- `semver-spec`: assumes the matched modules follow [semantic versioning](https://semver.org),
- `pvp`: assumes the matched modules follow [package versioning policy](https://pvp.haskell.org) (quite common in Scala),
- `always`: assumes all versions of the matched modules are compatible with each other,
- `strict`: requires exact matches between the wanted and the selected versions of the matched modules.

If no rules for a module are found in `libraryDependencySchemes`, `versionPolicyDefaultScheme` is used
as a compatibility type. Its default value is `VersionCompatibility.PackVer` (package versioning policy).

### Disable the tasks `versionPolicyCheck` or `versionCheck` on a specific project

You can disable the tasks `versionPolicyCheck` and `versionCheck` at the
project level by using the `skip` key.

By default, both `versionPolicyCheck / skip` and `versionCheck / skip` are
initialized to `(publish / skip).value`. So, to disable both tasks on
a given project, set the following:

~~~ scala
publish / skip := true
~~~

Or, if you need more fine-grained control:

~~~ scala
versionPolicyCheck / skip := true
versionCheck / skip := true
~~~

## Acknowledgments

<img src="https://scala.epfl.ch/resources/img/scala-center-swirl.png" width="40px" />

*sbt-version-policy* is funded by the [Scala Center](https://scala.epfl.ch).

[recommended versioning scheme]: https://docs.scala-lang.org/overviews/core/binary-compatibility-for-library-authors.html#recommended-versioning-scheme
[MiMa]: https://github.com/lightbend/mima
[sbt-dynver]: https://github.com/dwijnand/sbt-dynver