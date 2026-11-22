# Release checklist

Use this checklist for a release candidate. It is a manual gate; checked items must be verified for the release being prepared.

## Build and validation

- [ ] `./gradlew clean build` passes on JDK 17.
- [ ] `./gradlew validatePlugins` passes.
- [ ] Test output has been reviewed (including TestKit tests).
- [ ] The example project audit runs: `./gradlew :example:lighthouseAudit`.
- [ ] Supported Gradle/JDK/Android Gradle Plugin/Kotlin combinations are documented with evidence. Do not treat untested versions as verified.
- [ ] Configuration Cache or Isolated Projects claims are backed by tests for the release.

## Reports and user-facing behavior

- [ ] Console, HTML, JSON, SARIF, and JUnit behavior is reviewed for any changed report logic.
- [ ] Baseline and CI gate behavior is tested, including new and existing findings.
- [ ] Documentation and examples match the current implementation.
- [ ] Changelog is updated and version references are consistent.

## Publishing

- [ ] Plugin publication credentials are available as environment variables or Gradle properties; no credentials are committed.
- [ ] Publishing to Maven Local is verified when appropriate: `./gradlew publishToMavenLocal`.
- [ ] Release tag matches the version being published (`vX.Y.Z`).
- [ ] Plugin Portal publication and GitHub release notes are verified after publish.

See [release process](docs/developer/release-process.md) for the steps.