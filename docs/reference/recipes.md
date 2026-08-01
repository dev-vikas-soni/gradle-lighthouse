# Remediation Recipes

Standardized solutions for common architectural issues flagged by Lighthouse.

## 1. Migrating to KSP
KSP is up to 2x faster than KAPT.

1. Add the KSP plugin to your version catalog or build script.
2. Replace `kapt` with `ksp` in your dependencies.
3. Remove `id("kotlin-kapt")`.

**Lighthouse Shortcut:** Run `./gradlew lighthouseFix --ai=true`.

## 2. Bootstrapping a Version Catalog
Hardcoded versions make updates painful.

1. Create `gradle/libs.versions.toml`.
2. Move version strings to the `[versions]` block.
3. Define library aliases in the `[libraries]` block.
4. Replace strings in `build.gradle.kts` with `libs.*` accessors.

**Lighthouse Shortcut:** Run `./gradlew lighthouseAudit` to generate the suggested TOML structure in your console.

## 3. Breaking a Circular Dependency
Cycles prevent task parallelization and make refactoring impossible.

1. Use the **Galaxy Graph** to identify the edge closing the loop.
2. Extract common interfaces/models into a new `:domain` or `:api` module.
3. Have both cyclic modules depend on the new common module.
4. Remove the direct dependency between them.

## 4. Disabling Jetifier
Jetifier adds several minutes to clean builds in large repositories.

1. Ensure all your dependencies have migrated to AndroidX.
2. Set `android.enableJetifier=false` in `gradle.properties`.
3. Run `./gradlew lighthouseAudit` to check if any non-AndroidX libraries remain.

## 5. Enabling Configuration Cache
Dramatically speed up the configuration phase.

1. Set `org.gradle.configuration-cache=true` in `gradle.properties`.
2. Fix "Capturing Project" errors by removing `project` access from custom task actions.
3. Use `Provider<T>` for all task inputs.
