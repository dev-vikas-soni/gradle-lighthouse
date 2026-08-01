# Multi-Module Strategy

Lighthouse is specifically optimized for large-scale multi-module Android and KMP repositories.

## Optimal Setup

For a project with 50+ modules, we recommend the following setup:

### 1. Root Configuration
Apply the plugin once in the root project to enable aggregation.

```kotlin
// root build.gradle.kts
plugins {
    id("io.github.dev-vikas-soni.lighthouse") version "2.3.2"
}

lighthouse {
    failOnDependencyCycle.set(true)
    minHealthScore.set(80)
}
```

### 2. Sub-Module Enforcement
Use a **Convention Plugin** to apply Lighthouse to all modules automatically and enforce local standards.

```kotlin
// build-logic/ConventionPlugin.kt
subprojects {
    plugins.apply("io.github.dev-vikas-soni.lighthouse")

    extensions.configure<LighthouseExtension> {
        failOnSeverity.set("ERROR")
    }
}
```

## Scaling Performance

Lighthouse uses several techniques to remain fast in large repositories:

* **Snapshot Architecture**: Tasks analyze serializable DTOs, avoiding the overhead of the Gradle Model during execution.
* **Lazy Aggregation**: `lighthouseAggregate` uses a "pull" model, only collecting reports from modules that were actually audited.
* **Parallel Execution**: Each module audit runs in its own worker thread.

## Identifying Hotspots

Use the **Galaxy Graph** in the aggregate dashboard to find:
* **Coupling Hotspots**: Modules with a high "halo" size have many incoming dependencies and should be the target of refactoring.
* **Orphaned Modules**: Modules with low connectivity that might be candidates for deletion or consolidation.
