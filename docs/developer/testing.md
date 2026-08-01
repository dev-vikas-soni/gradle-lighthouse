# Testing Strategy

Gradle Lighthouse uses a multi-layered testing strategy to ensure reliability across Gradle and AGP versions.

## 1. Unit Tests

**Target**: Individual logic components like the `HealthScoreEngine`.
**Location**: `src/test/kotlin/com/gradlelighthouse/core`
**Tool**: JUnit 5 + Kotlin Test.

These tests are fast and do not require a real Gradle environment. They are ideal for verifying the mathematical correctness of the scoring model.

## 2. Functional Tests

**Target**: The full plugin lifecycle, task execution, and file generation.
**Location**: `src/test/kotlin/com/gradlelighthouse/LighthousePluginTest.kt`
**Tool**: `GradleRunner` (Gradle TestKit).

These tests create a temporary project, apply the plugin, and execute tasks.
* Use `withArguments("lighthouseAudit", "--stacktrace")`.
* Assert against the content of generated files in the `build/` directory.

## 3. The Integration Sandbox (`example/`)

**Target**: Real-world multi-module scenarios and interactive features like the Galaxy Graph.

This is a permanent Gradle project inside the repository.
* Run manually: `./gradlew :example:lighthouseAudit`.
* It is also used during the CI build to ensure no regressions in the report visualization.

## 4. Configuration Cache Verification

Every functional test should ideally verify compatibility:
1. Run a task once.
2. Run it again with `--configuration-cache`.
3. Assert "Reusing configuration cache" in the output.

## 5. Mocking AGP

Since Lighthouse analyze DTOs, you can test Android auditors without actually applying the Android plugin by manually populating the `sourceSetData` and `pluginIds` in a mock `AuditContext`.
