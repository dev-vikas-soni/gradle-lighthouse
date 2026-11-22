# Remediation recipes

These are general investigation patterns, not guaranteed fixes. Check compatibility with your Gradle, AGP, Kotlin, and library versions before changing a build.

## 1. Evaluating KAPT to KSP migration

KSP may be appropriate when the annotation processor and its version support it. It is not a drop-in replacement for every KAPT processor.

1. Check the processor's official KSP support and version compatibility.
2. Consult the library's migration guide.
3. Update the plugin and processor dependency deliberately.
4. Run compilation and tests for all affected variants and targets.
5. Compare build performance using repeatable measurements if speed is the reason for migrating.

The `lighthouseFix` task may offer pattern-based transformations, but it is not an AI migration engine and must not be assumed safe for arbitrary builds.

## 2. Using a version catalog

1. Create or update `gradle/libs.versions.toml`.
2. Move selected version declarations into the `[versions]` section.
3. Add aliases under `[libraries]` and `[plugins]` as appropriate.
4. Replace build-script declarations carefully.
5. Run Gradle sync/build and inspect the resulting dependency graph.

Automated text transformations can miss aliases, convention plugins, or unusual declaration forms. Review every changed file.

## 3. Investigating a circular module dependency

1. Inspect the module dependency graph and identify the cycle.
2. Confirm that the reported edge corresponds to a real project dependency.
3. Consider extracting a stable API or shared model module if that matches the domain.
4. Change the build graph and run compilation/tests for affected modules.

A graph visualization is diagnostic; it does not prove which refactor is architecturally correct.

## 4. Reviewing Jetifier configuration

Do not disable Jetifier solely because a heuristic recommends it. Verify all dependencies and transitive dependencies are AndroidX-compatible, then test relevant build variants.

## 5. Evaluating Configuration Cache

Follow Gradle's Configuration Cache diagnostics and the [project guide](../guides/configuration-cache.md). Measure build impact in your own environment rather than relying on generic speed claims.
