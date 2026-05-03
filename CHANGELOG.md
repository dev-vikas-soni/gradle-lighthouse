# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [2.0.0] - 2026-05-03

### Added
- **Gradle Plugin Portal Distribution**: Plugin now installable with one line: `id("io.github.dev-vikas-soni.lighthouse") version "2.0.0"`. No JitPack/resolutionStrategy needed.
- **Colorful Terminal Dashboard**: Screenshot-worthy box-drawing output with ANSI colors, score delta, rank progression, and actionable next steps.
- **Configuration Cache Readiness Auditor**: Detects allprojects/subprojects blocks, eager task creation, buildSrc usage, non-transitive R class, Project access in task actions.
- **Module Graph Auditor**: DFS-based circular dependency detection, feature-to-feature coupling violation, high coupling score, DOT graph generation.
- **Unused Dependency Auditor**: Import-based analysis to detect declared-but-unused dependencies and duplicate declarations.
- **Test Coverage Auditor**: Dark module detection (zero tests), test-to-source ratio, JaCoCo presence, consumer-rules.pro validation.
- **Security Auditor**: Hardcoded secrets detection, signing config safety, Gradle wrapper version check, dependency locking, JDK toolchain.
- **Module Size Auditor**: Lines of code, public API surface, build file complexity with splitting recommendations.
- **Version Catalog Hygiene Auditor**: Hardcoded version detection, unused TOML entries, bundle opportunity suggestions.
- **Trend Tracking Auditor**: Historical score persistence to `.lighthouse/`, delta reporting, regression alerts.
- **BuildConfig Waste Detection**: Flags library modules generating unused BuildConfig.java.
- **kotlin-android-extensions Detection**: Flags deprecated plugin blocking Kotlin upgrades.
- **GitHub Action** (`action.yml`): Composite action with SARIF upload, PR comment bot, configurable severity threshold.
- **CI Workflow** (`.github/workflows/lighthouse-ci.yml`): Build/test/publish pipeline.

### Changed
- **Plugin ID**: Changed from `com.gradlelighthouse.plugin` to `io.github.dev-vikas-soni.lighthouse` for Gradle Plugin Portal.
- **Group ID**: Changed from `com.github.dev-vikas-soni` to `io.github.dev-vikas-soni`.
- **Gradle Version**: Upgraded wrapper from 7.6.4 to 8.10.2.
- **Version**: Bumped to 2.0.0 (breaking change: plugin ID).
- **Extension**: Added 8 new toggle properties (all default true): `enableConfigCacheCheck`, `enableModuleGraphCheck`, `enableUnusedDependencyCheck`, `enableTestCoverageCheck`, `enableVersionCatalogHygiene`, `enableSecurityCheck`, `enableModuleSizeCheck`, `enableTrendTracking`.
- **JUnit XML**: Now emits all V2.0 categories (BuildPerformance, Security, Quality, etc.).
- **ConsoleLogger**: Rewritten with ANSI colors and box-drawing dashboard.
- **Docs**: Complete rewrite of HLD, LLD, and User Manual.

### Removed
- JitPack `resolutionStrategy` hack from settings.gradle.kts.
- Old plugin ID `com.gradlelighthouse.plugin` (breaking change — use new ID).

## [1.0.0] - 2026-05-02

### Added
- **Global Rebranding**: Officially rebranded to `Gradle Lighthouse`.
- **Enterprise Capabilities**: Added `lighthouseAggregate` task to generate a single 360° dashboard for 100+ module ecosystems.
- **Reporting Engines**: Added native SARIF v2.1.0 and JUnit XML generators for deep CI/CD integration.
- **Auditor Extensibility**: Re-architected core engine to support isolated `Auditor` interfaces.
- **Play Policy Auditor**: New compliance scanner to detect restricted permissions and target SDK mismatches.
- **Configuration Cache Safety**: Reworked `AuditContext` to strictly serialize inputs, guaranteeing zero cache invalidations in Gradle 8+.

### Changed
- Refactored package from `com.droidunplugged` to `com.gradlelighthouse`.
- DSL Configuration block renamed to `lighthouse { }`.
- Audit tasks renamed from `depAudit` to `lighthouseAudit`.

### Removed
- Removed legacy tightly-coupled monolithic execution patterns.

