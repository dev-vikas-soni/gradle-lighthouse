# Release process

This document describes the repository's tag-triggered release flow. Confirm the release checklist before publishing.

## 1. Prepare

1. Review changes and test them on JDK 17.
2. Update `CHANGELOG.md` and version declarations in the build/plugin sources.
3. Run `./gradlew clean build validatePlugins`.
4. Run the example audit: `./gradlew :example:lighthouseAudit`.
5. Verify documentation and compatibility claims for the release.

## 2. Verify locally

If useful, publish to Maven Local before the official release:

```bash
./gradlew publishToMavenLocal
```

## 3. Publish

The GitHub Actions workflow `.github/workflows/publish-plugin.yml` publishes when a version tag in the form `vX.Y.Z` is pushed.

1. Ensure `GRADLE_PUBLISH_KEY` and `GRADLE_PUBLISH_SECRET` are configured in repository Actions secrets.
2. Create and push the tag for the release version:

```bash
git tag vX.Y.Z
git push origin vX.Y.Z
```

3. Review the publishing workflow and confirm the plugin is available on the Gradle Plugin Portal.

## 4. GitHub release and documentation

Create the GitHub release from the tag and include the relevant changelog section. Update the auditor catalog and DSL reference if their behavior or configuration changes.

Never commit publication credentials or put them in release logs.