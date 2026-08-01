# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [2.3.2] - 2026-08-02

### Changed
- Refined root project aggregation logic for single-module projects.
- Hardened task configuration validation for `baseReportDir`.
- Comprehensive documentation overhaul and Navigator established.
- Standardized terminology across the intelligence suite.

## [2.3.1] - 2026-08-01

### Added
- **GenAI Remediation (`lighthouseFix --ai`)**: Introduced AI-assisted surgical refactoring.
    - Automated **KAPT to KSP** migration (plugin swap + dependency conversion).
    - Automated **Version Catalog** bootstrapping (hardcoded string to TOML mapping).
- **Lighthouse PR Bot & Delta Analysis**: Intelligent CI/CD integration for Pull Requests.
    - Automatic score delta calculation against the base branch.
    - Specialized flagging of **New** vs. existing dependency cycles.
    - Automated PR summaries in GitHub via `action.yml`.
- **Predictive Dependency Intelligence**: Proactive SDK impact estimation.
    - Warns about binary size, method count, and startup overhead *before* merging SDKs.
    - Curated knowledge base for popular SDKs (Facebook, Google Ads, Instabug, etc.).
- **Anonymized Telemetry**: Privacy-first opt-in layer to crowdsource SDK performance data and improve community predictions.

### Fixed
- Internal reliability improvements and baseline portability enhancements.
- Improved dependency regex for more accurate Version Catalog migrations.

## [2.3.0] - 2026-06-11

### Added
- **Modern Health Score Engine (V2)**: Replaced global exponential decay with a category-based square root model ($100 - K \times \sqrt{RawImpact}$).
- **Architecture Health Breakdown**: New dashboard section featuring independent scores for 9 domains: Architecture, Security, Performance, Build Performance, Complexity, Quality, Modernization, Dependency Hygiene, and App Size.
- **Industry Benchmarking**: Dynamic percentile engine that ranks projects against industry giants like *Signal Android* and *Now in Android*.
- **Benchmark Export Pipeline**: New task `lighthouseExportBenchmark` to automatically generate "Ground Truth" snapshots from real audits.
- **Project Persona Classification**: Automatic classification into personas (e.g., *Modular Monolith*, *Enterprise Android*) for peer-group comparison.
- **Baseline System**: Support for suppressing existing technical debt via `lighthouseRecordBaseline`.
- **Lighthouse Fix Engine**: New `lighthouseFix` task to automatically apply best practices to `gradle.properties`.
- **Thread-Safe Registry**: `BenchmarkRegistry` now supports parallel multi-module execution and double-checked locking for snapshot caching.

### Changed
- **Weakest Link Logic**: Overall score is now calculated as the average of the weighted mean and the poorest category score to prevent masking critical failures.
- **ANSI Terminal Dashboard**: Updated to include Category Health summary and strongest/weakest domain indicators.
- **JSON Aggregation Schema**: Intermediate `module-report.json` now includes full category breakdowns and top risks.

### Migration Notes
*   **Scoring Shift**: Projects will likely see a score movement (typically +/- 5pts) as the engine shifted from "Finding Counter" to "Category Model".
*   **Version Safety**: Benchmarks generated with V2.2.0 or older will trigger a "Re-audit recommended" warning due to the scoring engine overhaul.

## [2.2.2] - 2026-05-18

### Fixed
- **Sandbox Mode Overhaul**: Rebuilt the simulation engine to use the same exponential decay scoring as the Gradle auditors.
- **Position Persistence**: Modules no longer jump or randomize when cutting dependencies in simulation.
- **Surgical UI**: Added "Cycle Leak" highlighting and in-panel "Cut" buttons to make architectural refactoring simulations more intuitive.
- **Interaction Accuracy**: Increased graph hit-box tolerance by 150% for easier dependency selection.

## [2.2.1] - 2026-05-17

### Changed
- Refreshed the public documentation set (`README.md`, `docs/USER_MANUAL.md`, `docs/ENTERPRISE_ENFORCEMENT.md`) to reflect the current Phase 2 dashboard, Galaxy Graph, enforcement behavior, actual DSL defaults, and GitHub Actions usage.

## [2.2.0] - 2026-05-17

### Added
- **Interactive Galaxy Graph**: canvas-based module dependency visualisation embedded in the aggregate dashboard. Modules are grouped by architectural layer into orbital rings. Cycle edges are drawn with a red glow. Self-contained — no CDN.
- **Velocity Analytics**: trend charts for global health score, fatal issue count, and coupling density across the last 30 builds, persisted in `.lighthouse/global-history.json`.
- **Enforcement gates**: `failOnDependencyCycle`, `failOnLayerViolation`, `minHealthScore` — evaluated by `lighthouseAggregate`. All default to off.
- **Custom architecture rules**: `lighthouse-rules.yaml` support for isolation rules (`:feature:* !-> :feature:*`) and layering rules (`App -> Feature -> Core`), evaluated without any Gradle scripting.
- **Sandbox Mode**: click any dependency edge in the Galaxy Graph to remove it from simulation. Cycle count and score update live.
- **God Module Detection**: modules above coupling thresholds are given a larger visual halo in the graph.
- **Rank system**: score-based progression from Legacy to Grandmaster Architect, shown in the terminal dashboard and global report.

### Changed
- **Global dashboard redesign**: dark-mode space theme, real-time module filtering, full-screen graph mode, PNG export.
- **Report portability**: all graph logic and styles are fully inlined in the HTML output. No external assets.

---

## [2.1.1] - 2026-05-12

### Added
- Gradle 9.5 compatibility: removed dependencies on internal Gradle classes that changed between 8.x and 9.x.
- Isolated Projects support: module graph scanning no longer crosses project boundaries at execution time.

### Fixed
- Configuration Cache serialisation: eliminated "Error while saving task graph" by ensuring only serialisable data is captured at configuration time.
- Lazy dependency resolution: "Configuration resolved during configuration time" warnings gone — all heavy scanning moved to lazy `Provider` delegates.
- Broken navigation links in the global dashboard.
- Report collision: modules with the same simple name in different paths (`:feature:ui`, `:core:ui`) were overwriting each other's reports. Fixed with path-sanitized subdirectories.

### Changed
- Performance: configuration phase is faster in large projects because all intelligence scanning is deferred to task execution.
- Updated GitHub Actions (Checkout, setup-java, github-script, gradle/actions) to latest stable versions.
- Gradle Wrapper updated to 9.5. JUnit Platform updated to 6.0.3.

---

## [2.1.0] - 2026-05-10

### Added
- Phase 2 scaffolding: aggregate task infrastructure and enforcement engine stubs.

---

## [2.0.1] - 2026-05-03

### Fixed
- Initial stability fixes following publication to the Gradle Plugin Portal.

---

## [2.0.0] - 2026-05-03

### Added
- **Gradle Plugin Portal distribution**: `id("io.github.dev-vikas-soni.lighthouse") version "2.0.0"` — no JitPack, no `resolutionStrategy`.
- **ANSI terminal dashboard**: color-coded box-drawing output with score delta, rank, and next steps.
- **ConfigCacheReadinessAuditor**: detects `allprojects`/`subprojects` blocks, eager task creation, `buildSrc` usage, non-transitive R class, `Project` access in task actions.
- **ModuleGraphAuditor**: DFS cycle detection, feature-to-feature coupling check, coupling density, DOT graph output.
- **UnusedDependencyAuditor**: import-based source analysis for declared-but-unused dependencies and duplicates.
- **TestCoverageAuditor**: dark module detection, test-to-source ratio, JaCoCo presence, `consumer-rules.pro` check.
- **SecurityAuditor**: hardcoded secrets, signing config safety, Gradle wrapper version, dependency locking, JDK toolchain.
- **ModuleSizeAuditor**: LOC analysis, public API surface measurement, build file complexity.
- **VersionCatalogHygieneAuditor**: hardcoded versions, unused TOML entries, bundle opportunities.
- **TrendTrackingAuditor**: score history in `.lighthouse/`, delta display, regression alerts.
- BuildConfig waste detection in library modules.
- `kotlin-android-extensions` detection.
- `action.yml`: composite GitHub Action with SARIF upload, PR comment bot, configurable severity threshold.

### Changed
- **Plugin ID**: `com.gradlelighthouse.plugin` → `io.github.dev-vikas-soni.lighthouse`.
- **Group ID**: `com.github.dev-vikas-soni` → `io.github.dev-vikas-soni`.
- Gradle Wrapper upgraded from 7.6.4 to 8.10.2.
- 8 new extension toggles (all defaulting to `true`): `enableConfigCacheCheck`, `enableModuleGraphCheck`, `enableUnusedDependencyCheck`, `enableTestCoverageCheck`, `enableVersionCatalogHygiene`, `enableSecurityCheck`, `enableModuleSizeCheck`, `enableTrendTracking`.
- JUnit XML output now covers all categories (BuildPerformance, Security, Quality, etc.).
- `ConsoleLogger` rewritten with ANSI colors and box-drawing.

### Removed
- JitPack `resolutionStrategy` workaround.
- Legacy plugin ID `com.gradlelighthouse.plugin` — **breaking change**: update your `plugins {}` block.

---

## [1.0.0] - 2026-05-02

### Added
- Rebranded to Gradle Lighthouse.
- `lighthouseAggregate` task for multi-module 360° dashboards.
- Native SARIF v2.1.0 and JUnit XML report generators.
- `Auditor` interface — isolated, independently testable check implementations.
- `PlayPolicyAuditor`: restricted permissions and target SDK compliance.
- Reworked `AuditContext` for strict input serialisation and zero CC cache invalidation.

### Changed
- Package renamed from `com.droidunplugged` to `com.gradlelighthouse`.
- DSL block renamed to `lighthouse {}`.
- Task renamed from `depAudit` to `lighthouseAudit`.

### Removed
- Legacy monolithic execution model.
