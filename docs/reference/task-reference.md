# Task Reference

Lighthouse exposes several tasks for auditing, aggregation, and automated fixing.

## `lighthouseAudit`
Executes the full diagnostic scan for a module.
* **Scope**: Individual module.
* **Outputs**: Terminal summary, JSON, HTML, SARIF, JUnit.
* **Configuration Cache**: Safe.

## `lighthouseAggregate`
Aggregates module reports into a global project dashboard.
* **Scope**: Root project.
* **Prerequisite**: Must be run after (or with) `lighthouseAudit`.
* **Gates**: Evaluates `failOnDependencyCycle`, `failOnLayerViolation`, and `minHealthScore`.

## `lighthouseFix`
Automatically applies architectural improvements to build scripts.
* **Deterministic Fixes**: Updating `gradle.properties` (caching, parallel).
* **AI Fixes (`--ai=true`)**: KSP migration and Version Catalog bootstrapping.

## `lighthouseRecordBaseline`
Captures all current audit issues and suppresses them in future runs.
* **Scope**: Root or Module.
* **Output**: `lighthouse-baseline.txt`.

## `lighthouseBenchmarkStatus`
Checks your current project health against embedded industry benchmarks.
* **Reference Projects**: Signal Android, Now in Android.

## `lighthouseExportBenchmark`
Exports your current architectural state as a benchmark JSON file for reuse.
* **Output**: `benchmark.json`.

## Common CLI Options

### Fail on Severity
```bash
./gradlew lighthouseAudit -Plighthouse.failOnSeverity=ERROR
```

### Enable AI Fixes
```bash
./gradlew lighthouseFix --ai=true
```

### Provide Base Reports (Deltas)
```bash
./gradlew lighthouseAggregate -Plighthouse.baseReportDir=build/base-reports
```
