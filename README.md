# Gradle Lighthouse

**Architecture and build diagnostics for Android and Kotlin Multiplatform Gradle projects.**

Gradle Lighthouse is an open-source Gradle plugin that inspects project structure and selected build configuration, then reports findings intended to help teams understand module dependencies, build hygiene, and maintainability risks.

It is a diagnostic aid—not a substitute for code review, build profiling, security review, Android Lint, or tests. Findings depend on the information Lighthouse can inspect; they should be validated against your project before acting on them.

[![Gradle Plugin Portal](https://img.shields.io/gradle-plugin-portal/v/io.github.dev-vikas-soni.lighthouse?label=Gradle%20Plugin%20Portal)](https://plugins.gradle.org/plugin/io.github.dev-vikas-soni.lighthouse)
[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

## What it currently focuses on

- **Module dependency visibility:** inspect project module relationships and report detected dependency cycles.
- **Build configuration checks:** flag selected Gradle settings and dependency declarations that may deserve review.
- **Architecture rules:** configure supported module/layer constraints using the documented rules format.
- **Baselines and CI output:** record existing findings and emit supported report formats such as JSON, SARIF, and JUnit XML.
- **HTML reports:** review findings and available project/module visualizations.

The exact checks and their limitations are documented in the [auditor catalog](docs/reference/auditor-catalog.md). Not every advertised analysis is a measurement of runtime behavior or a complete semantic analysis of your source code.

## Quick start

Apply the plugin to a Gradle project:

```kotlin
plugins {
    id("io.github.dev-vikas-soni.lighthouse") version "2.4.0"
}
```

Run the audit:

```bash
./gradlew lighthouseAudit
```

For multi-module aggregation, if configured in your project:

```bash
./gradlew lighthouseAudit lighthouseAggregate
```

See [installation](docs/getting-started/installation.md), [quick start](docs/getting-started/quick-start.md), and [report interpretation](docs/getting-started/understanding-report.md).

## Be precise about what the results mean

- A health score is a project-specific heuristic derived from configured checks. It is not an industry benchmark, quality certification, or prediction of production reliability.
- A file-count ratio is not test coverage. Build-setting heuristics are not measured build-time improvements.
- SDK impact estimates are not benchmark measurements unless a report explicitly documents a reproducible measurement method and dataset.
- Baselines affect which findings are treated as new; consult the baseline documentation before interpreting scores and CI gates.
- Fix tasks can modify build files. Review the diff and keep your work under version control.

## Compatibility and maturity

Compatibility claims should be read alongside the [compatibility notes](docs/compatibility.md). A Gradle or Android Gradle Plugin version is not considered verified merely because it appears in a badge or broad version range. Please report your exact Gradle, AGP, Kotlin, and JDK versions when filing compatibility issues.

Kotlin Multiplatform checks are evolving and should be treated as partial diagnostics, not comprehensive KMP architecture validation.

## Anonymous Usage Telemetry

Gradle Lighthouse collects anonymous telemetry upon successful completion of an audit execution to understand plugin adoption and Gradle version compatibility across the ecosystem.

### What is collected
Each telemetry payload contains exactly four anonymous fields:
- `event`: Always `"audit_completed"`.
- `installationId`: A randomly generated UUID persisted in Gradle User Home (`~/.gradle/caches/gradle-lighthouse/installation-id`) and reused across invocations.
- `pluginVersion`: The version of Gradle Lighthouse executing (e.g., `"2.4.0"`).
- `gradleVersion`: The Gradle runtime version executing the build (e.g., `"9.0"`).

### What is NEVER collected
Gradle Lighthouse never collects project names, project paths, source code, dependency coordinates, repository URLs, audit findings, health scores, usernames, machine names, IP addresses, or build metadata.

### How to disable telemetry
Telemetry is enabled by default. You can disable telemetry using any of the following mechanisms:

1. **Gradle Command-line Property:**
   ```bash
   ./gradlew lighthouseAudit -PgradleLighthouseTelemetry=false
   ```

2. **Environment Variable:**
   ```bash
   export GRADLE_LIGHTHOUSE_TELEMETRY=false
   ```

3. **Gradle Build Script DSL:**
   ```kotlin
   lighthouse {
       enableTelemetry.set(false)
   }
   ```

## Open source

Issues, bug reports, documentation improvements, and focused pull requests are welcome. Please include a minimal reproducer and environment versions for behavior reports.

- [Contributing](CONTRIBUTING.md)
- [Roadmap](ROADMAP.md)
- [Known limitations and trust policy](docs/compatibility.md)
- [Issue tracker](https://github.com/dev-vikas-soni/gradle-lighthouse/issues)

## License

MIT. See [LICENSE](LICENSE).
