# Contributing


We follow the standard GitHub [fork & pull](https://help.github.com/articles/using-pull-requests/#fork--pull) approach to pull requests. Just fork the official repo, develop in a branch, and submit a PR!

You're always welcome to submit your PR straight away and start the discussion (without reading the rest of this wonderful doc, or the [`README.md`](README.md)). The goal of these notes is to make your experience contributing to this project as smooth and pleasant as possible. We're happy to guide you through the process once you've submitted your PR.

## Requirements

You need to have a JDK and sbt installed on your machine.

## Project Structure

This project contains only a single module, `sbt-version-policy`. Its
sources are under `src/`, at the root of the project.

The sources of the sbt plugin itself live under `src/main`.

Some unit tests can be found under `src/test`.

`src/sbt-test` contains [scripted tests](https://www.scala-sbt.org/1.x/docs/Testing-sbt-plugins.html), that test the plugin against actual sbt projects.

## Developer Workflow

To run the unit tests, start sbt, and run `test`:
```text
$ sbt
> test
```

To run the scripted tests, start sbt and run `scripted`:
```text
$ sbt
> scripted
```

Individual scripted tests can be run like
```text
$ sbt
> scripted sbt-version-policy/simple
```
Note that sbt is able to auto-complete the test names. See the completions by entering Tab.

## Submitting a PR

Whenever it makes sense, add unit tests and / or scripted tests when adding
new features or fixing existing ones.
