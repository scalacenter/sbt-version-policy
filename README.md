# sbt-version-policy

sbt-version-policy helps library maintainers to follow the [recommended versioning scheme].
This plugin:

- configures [MiMa] to check for binary or source incompatibilities,
- ensures that none of your dependencies are bumped or removed in an incompatible way,
- reports incompatibilities with previous releases,
- sets the [`versionScheme`](https://www.scala-sbt.org/1.x/docs/Publishing.html#Version+scheme) of the project to `"early-semver"`.

## Install

Make sure your project uses a version of sbt higher than 1.5.0.

Add to your `project/plugins.sbt`:

```scala
addSbtPlugin("ch.epfl.scala" % "sbt-version-policy" % "<version>")
```

The latest version is ![Scaladex](https://index.scala-lang.org/scalacenter/sbt-version-policy/sbt-version-policy/latest.svg).

sbt-version-policy depends on [MiMa], so that you don't need to explicitly
depend on it.

## Use

The plugin supports multiple types of workflow. It can validate that pull requests don’t break the binary compatibility or source compatibility, it can assess the compatibility level of a project compared to a previous release, and it can be used in combination with release plugins such as `sbt-ci-release` or `sbt-release`.

### How to check that a project does not violate the desired compatibility level?

The main use case in `sbt-version-policy` is to check that incoming pull requests don’t break the intended level of compatibility. For instance, a contribution targeting a branch that accepts only bug fixes should not introduce binary incompatibilities nor source incompatibilities.

To achieve this, you need to set the intended level of compatibility of the project with the setting `versionPolicyIntention`, to set the next release version with the setting `version`, and to run the task `versionPolicyCheck` in your continuous integration system.

#### 1. Set `versionPolicyIntention`

The setting `versionPolicyIntention` can take the following three values:

- ~~~ scala
  // Your next release will provide no compatibility guarantees with the
  // previous one (ie, it will be a major release).
  ThisBuild / versionPolicyIntention := Compatibility.None
  ~~~
- ~~~ scala
  // Your next release will be binary compatible with the previous one,
  // but it may not be source compatible (ie, it will be a minor release).
  ThisBuild / versionPolicyIntention := Compatibility.BinaryCompatible
  ~~~
- ~~~ scala
  // Your next release will be both binary compatible and source compatible
  // with the previous one (ie, it will be a patch release).
  ThisBuild / versionPolicyIntention := Compatibility.BinaryAndSourceCompatible
  ~~~

#### 2. Run `versionPolicyCheck`

The task `versionPolicyCheck` will report any incompatibilities beyond the intended compatibility level. You typically want to run this task in your CI pipeline to fail it when the changes in a pull request violate the intended compatibility level.

~~~ shell
sbt versionPolicyCheck
~~~

The task `versionPolicyCheck` checks that the dependencies of the module did not change in an incompatible way (for instance, if the intended compatibility level is `BinaryCompatible`, you cannot bump a dependency of your module to a new major version, otherwise the classpath would end up not being binary compatible), and that the code changes in the module itself do not violate the intended compatibility level (ie, it checks that the type signatures of existing public methods stay unchanged if the compatibility level is `BinaryCompatible`). [More details](#how-does-versionpolicycheck-work).

The plugin uses [MiMa] to check for incompatibilities with the previous release. To achieve this, it has to know what was the previous release version. By default, the previous release version is automatically computed from the current value of the `version` key in your build (more details [here](#automatic-previous-version-calculation)). This means that you have to set this key to the _next_ version you want to release:

~~~ scala
// Next version will be 1.1.0
ThisBuild / version := "1.1.0"
~~~

In practice, the way the version is defined in your build depends on your release process. For instance, if you use a plugin like [sbt-dynver] or [sbt-ci-release], which automatically set
the `version` based on the Git status, [read below](#how-to-integrate-with-sbt-dynver). If you use [sbt-release], read the [corresponding section](#how-to-integrate-with-sbt-release).

Alternatively, you can define your own logic to compute the previous version (e.g. to not require the `version` to be set) by redefining the setting `versionPolicyPreviousVersions`.

Note that `versionPolicyCheck` fails if it finds incompatibilities that violate the intended compatibility level. If you want to find such incompatibilities without failing, use the task `versionPolicyFindIssues`.

### How to check that the release version is valid with respect to the compatibility guarantees it provides?

Some release processes require you to manually set the release version. This is the case for all the release processes triggered by pushing a Git tag, such as [sbt-ci-release].

In such a case, your release process should check that the version you set is valid with respect to the compatibility guarantees of the release (as defined by `versionPolicyIntention`). For instance, a release that breaks the binary compatibility should bump the major version number.

You can check that by running the task `versionCheck` in your release process:

1. set the `version` to the new release version (e.g., `"1.2.3"`),
2. make sure `versionPolicyIntention` is set to the intended compatibility level of the release,
3. run `sbt versionCheck` before publishing your module artifacts.

The task `versionCheck` checks that the release version number is consistent with the
intended compatibility level as per `versionPolicyIntention`. For instance,
if your intention is to publish a release that breaks binary compatibility,
the task `versionCheck` will fail if you didn’t bump the major version
number.

See below how to integrate [with sbt-ci-release](#how-to-integrate-with-sbt-ci-release) or [with sbt-release](#how-to-integrate-with-sbt-release) for instructions specific to these release processes.

### How to assess the compatibility level of a project?

In case you don’t want to force a compatibility level but are interested in knowing the current level of compatibility of the project compared to its previous version, you can use the task `versionPolicyAssessCompatibility`:

1. do not assign a value to `versionPolicyIntention`,
2. set the `version` to the next release version,
3. use the task `versionPolicyAssessCompatibility` to compute the compatibility level.

The reason why you need to set the `version` to the next release version before running `versionPolicyAssessCompatiblity` is because we use it to compute the previous release version, against which assess the compatibility level. Alternatively, you can manually define the previous release version by redefining the setting `versionPolicyPreviousVersions`.

## Integrate with your release process

Some release processes require you to define the release version beforehand (e.g., [sbt-ci-release]), and some of them compute the release version as part of the process (e.g., [sbt-release]). That difference impacts the integration with sbt-version-policy.

### How to integrate with `sbt-ci-release`?

[sbt-ci-release] uses Git tags to compute the project version. You can integrate sbt-version-policy into a project that uses [sbt-ci-release] as follows:

- check that incoming pull requests do not violate the intended compatibility level ([detailed documentation](#how-to-check-that-a-project-does-not-violate-the-desired-compatibility-level))
  1. if your project contains multiple sub-projects, set `versionPolicyIgnoredInternalDependencyVersions` as explained in the [sbt-dynver integration](#supporting-multi-projects-builds):
     ~~~ scala
     versionPolicyIgnoredInternalDependencyVersions := Some("^\\d+\\.\\d+\\.\\d+\\+\\d+".r)
     ~~~
  2. set the intended compatibility level of the next release with the setting `versionPolicyIntention`
  3. run `sbt versionPolicyCheck` in your CI pipeline:
     ~~~ yaml
     steps
       - name: Check compatibility
         run: sbt versionPolicyCheck
     ~~~
- check that a new release version is valid with respect to its compatibility guarantees ([detailed documentation](#how-to-check-that-the-release-version-is-valid-with-respect-to-the-compatibility-guarantees-it-provides))
  1. run `sbt versionCheck` in your CI pipeline before running `ci-release`:
     ~~~ yaml
     steps
       - name: Release
         run: sbt versionCheck ci-release
     ~~~

Since [sbt-ci-release] uses [sbt-dynver] under the hood, please
read over the [next section](#how-to-integrate-with-sbt-dynver).

#### Examples

sbt-version-policy itself uses sbt-version-policy and [sbt-ci-release].
You can have a look at our [Github workflow](./.github/workflows/ci.yml) as an example of integration.

You can also have a look at the test [example-sbt-ci-release](./sbt-version-policy/src/sbt-test/sbt-version-policy/example-sbt-ci-release)
for a minimalistic sbt project using both sbt-version-policy and sbt-ci-release.

### How to integrate with `sbt-dynver`?

`sbt-dynver` generates version numbers looking like `1.2.3+4-abcd1234` when the Git history
contains commits, or changes, after the last tag.

#### Supporting multi-projects builds

The version numbers generated by sbt-dynver are usually not a problem, except when checking for dependency issues
between projects of the current build (e.g., if a project `a` depends on another project `b`
in the current build). In such a case, `sbt-version-policy` might report a false incompatibility
when checking the dependencies of `a` (because the project `b` now has a non-normalized version
number, from which we are unable to draw any conclusions).

To solve this issue, you can tell `sbt-version-policy` to ignore the dependencies to internal
projects when their version number matches some regular expression:

~~~ scala
// Ignore dependencies to internal modules whose version is like `1.2.3+4...`
ThisBuild / versionPolicyIgnoredInternalDependencyVersions := Some("^\\d+\\.\\d+\\.\\d+\\+\\d+".r)
~~~

#### Unsupported custom `dynverSeparator`

When sbt-version-policy computes the previous version of the release, it 
only supports `"+"` as a `dynverSeparator`.
This can be an issue in case you changed this setting to use a more
[portable version string](https://github.com/dwijnand/sbt-dynver#portable-version-strings).

In the specific case of Docker usage, a [workaround](https://github.com/scalacenter/sbt-version-policy/issues/103)
is to keep the default `dynverSeparator` value (`"+"`), and to tweak the 
`Docker / version`:

~~~ scala
Docker / version := version.value.replace('+', '-')
~~~

#### Example

You can have a look at the test [example-sbt-dynver](./sbt-version-policy/src/sbt-test/sbt-version-policy/example-sbt-dynver)
for a minimalistic sbt project using both sbt-version-policy and sbt-dynver.

### How to integrate with `sbt-release`?

[sbt-release] is able to run sophisticated release pipelines
including running the tests, setting the release version, publishing the artifacts, and pushing
a Git tag named after the release version.

There are two ways to use sbt-version-policy along with sbt-release:
 - define the intended compatibility level of the next release, and check that the changes applied to the project do not violate it,
 - or, let the project evolve freely and, at the time of the release, compute the release version according to the level of incompatibilities introduced in the project.

#### Constrained compatibility level

In this mode, you can use sbt-version-policy to check that incoming pull requests do not violate the intended compatibility level, and to compute the next release version according to the compatibility level.

- check that incoming pull requests do not violate the intended compatibility level ([detailed documentation](#how-to-check-that-a-project-does-not-violate-the-desired-compatibility-level))
  1. set the intended compatibility level of the next release with the setting `versionPolicyIntention`
  2. run `sbt versionPolicyCheck` in your CI pipeline:
     ~~~ yaml
     steps
       - name: Check compatibility
         run: sbt versionPolicyCheck
     ~~~
- compute the next release version according to its compatibility guarantees
    1. set the key `releaseVersion` as follows:
       ~~~ scala
       import sbtversionpolicy.withsbtrelease.ReleaseVersion
       releaseVersion := ReleaseVersion.fromCompatibility(versionPolicyIntention.value)
       ~~~
       The `releaseVersion` function bumps the release version according to the compatibility guarantees defined
       by `versionPolicyIntention`. Optionally, you can also define a _qualifier_ to append to the release version
       by setting the environment variable `VERSION_POLICY_RELEASE_QUALIFIER` (e.g., `VERSION_POLICY_RELEASE_QUALIFIER="-RC1"`).
    2. Reset `versionPolicyIntention` to `Compatibility.BinaryAndSourceCompatible` after every release.
       This can be achieved by managing the setting `versionPolicyIntention` in a separate file (like [sbt-release] manages the setting `version` in a separate file, by default), and by adding a step that overwrites the content of that file and commits it.

##### Example

You can have a look at the test [example-sbt-release](./sbt-version-policy/src/sbt-test/sbt-version-policy/example-sbt-release)
for an example of sbt project using both sbt-version-policy and sbt-release.

In that example, we also automatically reset the intended compatibility level to `BinaryAndSourceCompatible` as the last step of the release process.

#### Unconstrained compatibility level

In this mode, you can use sbt-version-policy to assess the incompatibilities introduced in the project since the last release and compute the new release version accordingly (ie, to bump the major version number if you introduced binary incompatibilities):

1. make sure `versionPolicyIntention` is not set
2. define `releaseVersion` from the compatibility level returned by `versionPolicyAssessCompatibility`:
   ~~~ scala
   import sbtversionpolicy.withsbtrelease.ReleaseVersion

   releaseVersion := {
     ReleaseVersion.fromAssessedCompatibilityWithLatestRelease().value
   }
   ~~~
   Alternatively, if your project contains multiple modules, you want to use the aggregated assessed compatibility level:
   ~~~ scala
   import sbtversionpolicy.withsbtrelease.ReleaseVersion
   
   releaseVersion := {
     ReleaseVersion.fromAggregatedAssessedCompatibilityWithLatestRelease().value
   }
   ~~~
   In both cases, the `releaseVersion` function sets the release version according to the compatibility level
   with the latest release. Optionally, you can also define a _qualifier_ to append to the release version
   by setting the environment variable `VERSION_POLICY_RELEASE_QUALIFIER` (e.g., `VERSION_POLICY_RELEASE_QUALIFIER="-RC1"`).

Note that for the first release you have to set the release version yourself via the file `version.sbt` (e.g., set
`1.0.0-SNAPSHOT` or `0.1.0-SNAPSHOT`). This is because `sbt-version-policy` needs a previous release to exist to be
able to assess the compatibility level of the current state of the project with that release.

##### Example

We demonstrate the “unconstrained” mode in [this example](./sbt-version-policy/src/sbt-test/sbt-version-policy/example-sbt-release-unconstrained).

### How to generate compatibility reports?

You can export the compatibility reports in JSON format with the task `versionPolicyExportCompatibilityReport`.

1. It does not matter whether `versionPolicyIntention` is set or not. If it is set, the report will list the incompatibilities that violate the intended compatibility level. If it is not set, all the incompatibilities will be reported.
2. Invoke the task `versionPolicyExportCompatibilityReport` on the module you want to generate a report for. For example, for the default root module:
   ~~~ shell
   sbt versionPolicyExportCompatibilityReport
   ~~~
   The task automatically aggregates the compatibility reports of all its aggregated submodules.
3. Read the file `target/scala-2.13/compatibility-report.json` (or `target/scala-3/compatibility-report.json`).
   You can see an example of compatibility report [here](./sbt-version-policy/src/sbt-test/sbt-version-policy/export-compatibility-report/expected-compatibility-report.json).
   
   Here are examples of how to read some specific fields of the compatibility report with `jq`:
   ~~~ shell
   # Get the highest compatibility level satisfied by all the aggregated modules.
   # Returns either 'incompatible', 'binary-compatible', or 'binary-and-source-compatible'.
   cat compatibility-report.json | jq '.aggregated.compatibility.value'
   
   # Get a human-readable description of the highest compatibility level sastisfied
   # by all the aggregated modules.
   cat compatibility-report.json | jq '.aggregated.compatibility.label'
   
   # Get the version of the project against which the compatibility level
   # was assessed.
   cat compatibility-report.json | jq '.aggregated.modules[0]."previous-version"'
   # Or, in the case of a single module report (no aggregated submodules):
   cat compatibility-report.json | jq '."previous-version"'
   ~~~

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

### Custom parsing of version numbers, `versionPolicyModuleVersionExtractor`

You can specify custom logic for parsing version numbers on a per-dependency basis. This is useful when artifact providers use their own version naming scheme.

For example, say you have this dependency

~~~ scala
libraryDependencies += "com.google.apis" % "google-api-services-iam" % "v1-rev20211104-1.32.1"
~~~

Google's version scheme is to include the target REST API version, `v1-rev20211104` with the Java API version, `1.32.1`.

In order to tell sbt-version-policy how to parse this version number, you can bind `versionPolicyModuleVersionExtractor`:

~~~ scala
versionPolicyModuleVersionExtractor := {
  case m if m.name.startsWith("google-api-services") => m.revision.split('-').last
}
~~~

## Acknowledgments

<img src="https://scala.epfl.ch/resources/img/scala-center-swirl.png" width="40px" />

*sbt-version-policy* is funded by the [Scala Center](https://scala.epfl.ch).

[recommended versioning scheme]: https://docs.scala-lang.org/overviews/core/binary-compatibility-for-library-authors.html#recommended-versioning-scheme
[MiMa]: https://github.com/lightbend/mima
[sbt-dynver]: https://github.com/sbt/sbt-dynver
[sbt-release]: https://github.com/sbt/sbt-release
[sbt-ci-release]: https://github.com/sbt/sbt-ci-release
