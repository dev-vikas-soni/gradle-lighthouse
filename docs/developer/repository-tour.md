# Repository Tour

A map of the codebase for new contributors.

## Root Directory
* `build.gradle.kts`: The plugin's own build script. Uses `java-gradle-plugin`.
* `action.yml`: The GitHub Action metadata.
* `lighthouse-rules.yaml`: Example custom rules file.

## `src/main/kotlin/com/gradlelighthouse`

### `.core`
**The Kernel.**
* `AuditContext.kt`: The serializable DTO that auditors inspect.
* `Auditor.kt`: The interface contract and severity enums.
* `HealthScoreEngine.kt`: The mathematical scoring implementation.
* `ConsoleLogger.kt`: Handles the colorful terminal dashboard.

### `.auditors`
**The Logic Workers.**
* Every file here is a standalone diagnostic check.
* *Example*: `BuildSpeedAuditor.kt` handles caching and KSP checks.

### `.task`
**The Orchestration Layer.**
* `LighthouseTask.kt`: The worker for a single module.
* `LighthouseAggregateTask.kt`: The root-project aggregator and cycle detector.
* `LighthouseFixTask.kt`: The repair engine (Deterministic + AI).

### `.reporting`
**The Presentation Layer.**
* `HtmlReportGenerator.kt`: Built-in template engine for dashboards.
* `SarifReportGenerator.kt`: SARIF v2.1.0 implementation.

### `.extension`
**The User Interface.**
* `LighthouseExtension.kt`: Defines the properties visible in the `lighthouse {}` block.

## `src/test/kotlin`
* Functional tests using `gradleTestKit`.
* Unit tests for the `HealthScoreEngine`.

## `example/`
* A sandbox project for manual testing. Run `./gradlew :example:lighthouseAudit` to see your changes in action.
