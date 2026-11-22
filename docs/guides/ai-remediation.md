# Deterministic remediation

> **Naming note:** This page retains its historical filename for existing links. The `lighthouseFix` task is not an AI or LLM-powered remediation engine. The `--ai` flag does not make an LLM call.

The fix task applies a limited set of deterministic, pattern-based changes. Its behavior is implementation-specific and may not understand every Gradle DSL construct, convention plugin, version catalog, or project-specific build convention.

## Before running

1. Commit or stash your working tree.
2. Run the task on a disposable branch or a clean working copy.
3. Inspect every changed file.
4. Run the project's normal Gradle validation and test suite.
5. Revert changes that are not correct for your build.

## Current implementation caveats

Some transformations use text matching or regular expressions rather than Gradle's complete semantic model. For example, KAPT-to-KSP and version-catalog transformations may not be valid for every plugin, processor, version combination, or dependency declaration. Do not assume the task can safely migrate an arbitrary project automatically.

No build-time savings, correctness improvement, or successful migration is guaranteed. Validate compatibility with the libraries and plugin versions in your own project.

## Example

If the task is available in your plugin version, invoke it with:

```bash
./gradlew lighthouseFix
```

The historical `--ai` option should not be interpreted as a request to an external AI service. Check the task implementation and release notes for the exact behavior of the version you use.

## Future direction

A genuine AI-assisted workflow would require an explicit model/provider integration, clear data-flow and privacy documentation, human review, safe patch previews, and verification after edits. That is future work, not a current capability.
