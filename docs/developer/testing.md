# Testing

This guide describes useful test layers for changes to Gradle Lighthouse. The presence of a test category in this document is not a claim that every auditor, Gradle version, AGP version, or feature currently has comprehensive coverage.

## Run the repository checks

Start with:

```bash
./gradlew test
./gradlew build
```

Use the tasks configured by the repository's current Gradle build. If a task name differs, inspect `./gradlew tasks --all`.

## Unit tests

Use focused unit tests for scoring, fingerprinting, parsing, graph algorithms, and individual auditor behavior. For each rule, cover:
- A fixture where the issue should be reported.
- A fixture where the issue should not be reported.
- Boundary cases and malformed or missing inputs where relevant.

## Functional tests

Use Gradle TestKit for plugin application, task execution, generated reports, and failure behavior. Prefer small fixture projects checked into test resources.

## Compatibility tests

Configuration Cache, Isolated Projects, Gradle/JDK versions, AGP, Kotlin, and KMP layouts need explicit functional tests. Do not infer compatibility from a unit test or design intention. Publish the exact passing matrix for each release.

## Regression tests

Every confirmed false positive or missed finding should become a reproducible regression fixture where practical. Test baseline behavior and ensure console/HTML/JSON/SARIF/JUnit outputs agree.

## Reporting test gaps

When adding or changing a check, document the inputs it inspects, blind spots, and the fixture coverage. See the [roadmap](../../ROADMAP.md) for current reliability priorities.
