# AI Remediation (`--ai`)

Lighthouse doesn't just find architectural rot—it helps you fix it. The AI remediation engine can perform complex, multi-file refactorings that usually require manual effort.

## How to use

Run the `lighthouseFix` task with the `--ai` flag:

```bash
./gradlew lighthouseFix --ai=true
```

## Supported Refactorings

### 1. KAPT to KSP Migration
KAPT is a major build speed bottleneck. Lighthouse can automatically migrate annotation processors like Room and Hilt to KSP.
* **Logic**: Adds the KSP plugin, comments out KAPT, and swaps dependency notations.
* **ROI**: Saves approx. 15-40s per clean build cycle.

### 2. Version Catalog Bootstrapping
Still using hardcoded dependency strings? Lighthouse can move your project to a modern `libs.versions.toml`.
* **Logic**: Scans for static version strings, generates the TOML file, and surgically updates your `build.gradle.kts` files to use type-safe accessors.
* **Benefit**: Centralized version management and 100% type-safe dependency access.

## Safety & Rollback

> [!WARNING]
> AI fixes modify your build scripts directly. We strongly recommend committing all changes to Git before running remediation so you can easily revert if needed.

The engine uses **Surgical Regex Patches** designed to be as non-destructive as possible. It preserves your comments and original file formatting where possible.

## Future: GenAI Integration
Future versions of Lighthouse will allow you to provide your own LLM API key to handle even more complex refactorings, such as extracting logic into separate modules or converting Java utility classes to idiomatic Kotlin.
