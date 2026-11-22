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
    id("io.github.dev-vikas-soni.lighthouse") version "2.3.2"
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

## Optional usage analytics

Gradle Lighthouse does not send usage telemetry by default. Projects that explicitly opt in can enable a minimal audit-completed event:

```kotlin
lighthouse {
    enableTelemetry.set(true)
}
```

When enabled, Lighthouse sends a random, persistent installation identifier, the plugin version, and the Gradle version to the configured HTTPS telemetry endpoint. The identifier is stored under the user's Gradle User Home (in `caches/gradle-lighthouse/installation-id`), not in the project. This allows opted-in installations to be counted across multiple projects without collecting a developer's name or email.

Telemetry does **not** include project names or paths, module names, dependencies, source code, audit findings, or health scores. Events are best-effort: network failures never fail the build. To opt out, leave `enableTelemetry` unset (the default) or set it to `false`. To reset the installation identifier, delete the file described above.

See [the telemetry and privacy notes](docs/telemetry.md) for the event contract and endpoint configuration.

## Compatibility and maturity

Compatibility claims should be read alongside the [compatibility notes](docs/compatibility.md). A Gradle or Android Gradle Plugin version is not considered verified merely because it appears in a badge or broad version range. Please report your exact Gradle, AGP, Kotlin, and JDK versions when filing compatibility issues.

Kotlin Multiplatform checks are evolving and should be treated as partial diagnostics, not comprehensive KMP architecture validation.

## Open source

Issues, bug reports, documentation improvements, and focused pull requests are welcome. Please include a minimal reproducer and environment versions for behavior reports.

- [Contributing](CONTRIBUTING.md)
- [Roadmap](ROADMAP.md)
- [Known limitations and trust policy](docs/compatibility.md)
- [Issue tracker](https://github.com/dev-vikas-soni/gradle-lighthouse/issues)

## License

MIT. See [LICENSE](LICENSE).
