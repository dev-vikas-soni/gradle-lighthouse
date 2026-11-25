# Installation

Gradle Lighthouse is published through the [Gradle Plugin Portal](https://plugins.gradle.org/plugin/io.github.dev-vikas-soni.lighthouse).

## Requirements

- **Gradle:** 8.0 or later is the documented minimum.
- **JDK:** 17 or later is the documented minimum.
- **Android Gradle Plugin:** required for Android-specific checks.

These are minimum requirements, not a claim that every later Gradle/JDK/AGP/Kotlin combination is tested. See the [compatibility notes](../compatibility.md) and verify the exact versions used by your project.

## Apply the plugin

In a single-module project, add to `build.gradle.kts`:

```kotlin
plugins {
    id("io.github.dev-vikas-soni.lighthouse") version "2.4.0"
}
```

For a multi-module project, apply the plugin where the audit tasks are needed. Root-level aggregation depends on the plugin's configuration and the report inputs available from participating modules.

### Version catalog

```toml
[plugins]
lighthouse = { id = "io.github.dev-vikas-soni.lighthouse", version = "2.4.0" }
```

Then:

```kotlin
plugins {
    alias(libs.plugins.lighthouse)
}
```

## Verify the installation

Run:

```bash
./gradlew tasks --all
./gradlew lighthouseAudit
```

Review the output and generated reports. If the task is not found or the audit fails, include the Gradle wrapper version, JDK, AGP/Kotlin versions, and stack trace when reporting the issue.

Configuration Cache and Isolated Projects behavior should be verified in your own build; see the [Configuration Cache guide](../guides/configuration-cache.md).
