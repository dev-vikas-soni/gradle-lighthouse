# Project Isolation Guide

Gradle 9.0 introduces **Project Isolation**, a feature that prevents projects from accessing each other's mutable state. Lighthouse is one of the few architectural plugins that is fully compatible with this model.

## Why typical plugins break
Most dependency analysis plugins use `project.allprojects` or `subprojects` to build their graph. This is forbidden under Project Isolation because it cross-pollinates mutable state.

## How Lighthouse stays compatible
Lighthouse uses a **Decoupled Discovery** mechanism:

1. **Local Capture**: Each project's `LighthouseTask` only captures its own direct dependencies.
2. **Artifact-Based Aggregation**: The root `LighthouseAggregateTask` uses a `ConfigurableFileCollection` to collect JSON reports as "artifacts." It never calls `project(":module")` directly.

## Requirements for Isolation
To ensure Lighthouse remains compatible in your project:

* **Avoid `allprojects {}`**: Use convention plugins in `buildSrc` or `composite builds` instead.
* **Lazy Initialization**: Ensure you are using the lazy `lighthouse { ... }` configuration block rather than eager assignment.

## Testing for Isolation
You can verify your project's isolation readiness by running:

```bash
./gradlew lighthouseAudit --isolated-projects
```
