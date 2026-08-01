# Installation

Gradle Lighthouse is distributed via the [Gradle Plugin Portal](https://plugins.gradle.org/plugin/io.github.dev-vikas-soni.lighthouse).

## Requirements

* **Gradle**: 8.0 or higher (Fully supports Gradle 9.0+)
* **Java**: 17 or higher
* **Android Gradle Plugin (Optional)**: 7.4 or higher (if auditing Android modules)

## Single Module Project

Add the following to your `build.gradle.kts` file:

```kotlin
plugins {
    id("io.github.dev-vikas-soni.lighthouse") version "2.3.2"
}
```

## Multi-Module Project

For multi-module projects, it is recommended to apply the plugin in the root `build.gradle.kts` to enable aggregation features, and then apply it to any sub-module you wish to audit.

### Root Project

```kotlin
// root build.gradle.kts
plugins {
    id("io.github.dev-vikas-soni.lighthouse") version "2.3.2"
}
```

### Sub-Modules

You can apply the plugin to sub-modules individually:

```kotlin
// sub-module build.gradle.kts
plugins {
    id("io.github.dev-vikas-soni.lighthouse")
}
```

### Using Version Catalogs (Recommended)

If you are using Gradle Version Catalogs, add the plugin definition to your `libs.versions.toml`:

```toml
[plugins]
lighthouse = { id = "io.github.dev-vikas-soni.lighthouse", version = "2.3.2" }
```

Then apply it in your build scripts:

```kotlin
plugins {
    alias(libs.plugins.lighthouse)
}
```

## Configuration Cache & Project Isolation

Gradle Lighthouse is built from the ground up to be compatible with modern Gradle performance features.

* **Configuration Cache**: Fully supported. No `Project` access during task execution.
* **Project Isolation**: Fully supported. Does not use `allprojects` or `subprojects` during execution.
