# Configuration

Gradle Lighthouse is designed with "Zero-Config" in mind. All 20+ auditors are enabled by default with sensible industry-standard rules.

However, you can customize the behavior using the `lighthouse {}` block in your `build.gradle.kts`.

## Basic Configuration

 ```kotlin
 lighthouse {
     // Fail the build if an issue with ERROR severity or higher is found
     failOnSeverity.set("ERROR")

     // Target a specific build variant (default is all)
     targetVariant.set("release")
 }
 ```

## Disabling Specific Checks

If a specific category of checks is not relevant to your project, you can disable it:

 ```kotlin
 lighthouse {
     enablePlayPolicy.set(false)
     enableKmpCheck.set(false)
 }
 ```

## CI/CD Enforcement

For large teams, you can enforce architectural boundaries at the aggregate level:

 ```kotlin
 lighthouse {
     // Fail if there is a circular dependency between modules
     failOnDependencyCycle.set(true)

     // Fail if an architectural layer violation is found (e.g. Core -> App)
     failOnLayerViolation.set(true)

     // Fail if the global health score falls below 80
     minHealthScore.set(80)
 }
 ```

## Delta Analysis (PR Bot)

To enable Pull Request summaries with score deltas:

 ```kotlin
 lighthouse {
     // Path to the reports from the 'main' branch
     baseReportDir.set(file(".lighthouse/main-branch-reports"))
 }
 ```

## Full DSL Reference

For a complete list of all properties and their default values, see the [DSL Reference](../reference/dsl-reference.md).
