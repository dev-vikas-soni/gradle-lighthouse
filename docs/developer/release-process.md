# Release Process

This document outlines the steps for publishing a new version of the Gradle Lighthouse plugin.

## 1. Pre-Release Checklist

1. [ ] Update `CHANGELOG.md` with all new features and fixes.
2. [ ] Bump the version in `build.gradle.kts` and `LighthousePlugin.VERSION`.
3. [ ] Run all tests: `./gradlew test`.
4. [ ] Verify the example project: `./gradlew :example:lighthouseAudit`.

## 2. Publish to Maven Local (Verification)

Before going public, verify the artifacts locally:

```bash
./gradlew publishToMavenLocal
```

Then in the `example/` project, change the version to the new one and ensure everything still works.

## 3. Official Publication

Lighthouse is published to the **Gradle Plugin Portal**.

1. Ensure you have your `gradle.publish.key` and `gradle.publish.secret` in your environment or `local.properties`.
2. Execute the publish task:

```bash
./gradlew publishPlugins
```

## 4. GitHub Release

1. Create a new tag in Git: `git tag v2.3.x`.
2. Push the tag: `git push origin v2.3.x`.
3. Draft a new release on GitHub, pasting the latest section of the `CHANGELOG.md`.

## 5. Documentation Update

If new auditors or DSL properties were added:
1. Update `docs/reference/auditor-catalog.md`.
2. Update `docs/reference/dsl-reference.md`.
