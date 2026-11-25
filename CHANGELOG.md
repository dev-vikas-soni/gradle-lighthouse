# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

> **Documentation clarification (2026-11):** Some historical entries below use terms such as "GenAI remediation", "community telemetry", "industry benchmarking", "test coverage", and "predictive impact" more strongly than the implementation evidence supports. These entries preserve the historical release notes, not a current capability guarantee. In particular, the `lighthouseFix --ai` path is deterministic/pattern-based rather than an LLM integration; source-file ratios are not test coverage; and SDK impact/percentile values must not be treated as empirical or representative benchmarks without a published dataset and methodology. See the current [README](README.md), [auditor catalog](docs/reference/auditor-catalog.md), and [roadmap](ROADMAP.md) for qualified descriptions.

---

## [2.5.0] - 2026-11-25

### Added
- Anonymous audit completion telemetry system reporting `audit_completed`, `installationId`, `pluginVersion`, and `gradleVersion`.
- Persistent anonymous installation identifier saved in `$GRADLE_USER_HOME/caches/gradle-lighthouse/installation-id`.
- Telemetry opt-out controls via CLI property (`-PgradleLighthouseTelemetry=false`), environment variable (`GRADLE_LIGHTHOUSE_TELEMETRY=false`), and plugin DSL (`lighthouse { enableTelemetry.set(false) }`).
- Support for enterprise custom telemetry endpoint overrides (`lighthouse { telemetryEndpoint.set(...) }`).

### Fixed
- Hardened automated integration test suite isolation to prevent test builds from issuing production telemetry requests (`d089c05`).
- Non-blocking failure isolation ensuring network timeouts, HTTP errors, or missing connectivity never fail audit tasks.

### Changed
- Promoted plugin version to 2.5.0.

## [2.4.0] - 2025-03-04

### Added
- Release readiness engineering verification suite and automated health score benchmarking.
- Enhanced Gradle 9.x compatibility safeguards and Project Isolation task input providers.

### Changed
- Promoted plugin version to 2.4.0 for minor feature release.

## [2.3.2] - 2026-08-02

### Changed
- Refined root project aggregation logic for single-module projects.
- Hardened task configuration validation for `baseReportDir`.
- Documentation maintenance.

## [2.3.1] - 2026-08-01

### Added
- Historical release notes described KAPT-to-KSP and version-catalog transformations in `lighthouseFix`. These are deterministic text/pattern-based changes, not AI-assisted or guaranteed migrations.
- GitHub Action integration and report delta-related behavior.
- Curated SDK-related estimates. These should not be interpreted as community-measured impact data.

### Fixed
- Internal reliability improvements and baseline portability enhancements.
- Improved dependency regex for version-catalog transformations.

## [2.3.0] - 2026-06-11

### Added
- Health score model and category breakdowns.
- Benchmark snapshot/export infrastructure. The presence of snapshots does not establish a statistically valid industry benchmark or percentile.
- Baseline task and fix-task infrastructure.
- Additional report and aggregation behavior.

### Changed
- Updated score aggregation and report output.

Earlier release entries are retained as historical records. Refer to tagged source and release artifacts for exact implementation details.
