# gradle-lighthouse

Architecture intelligence for Android and Kotlin Multiplatform Gradle builds.

Gradle Lighthouse audits module structure, dependency hygiene, security, build performance, and code health directly from your Gradle project. It generates per-module reports, a global dashboard with the Galaxy Graph and trend analytics, and optional CI enforcement gates for cycles, layer leaks, and score regressions.

[![Gradle Plugin Portal](https://img.shields.io/gradle-plugin-portal/v/io.github.dev-vikas-soni.lighthouse?label=Gradle%20Plugin%20Portal&color=orange)](https://plugins.gradle.org/plugin/io.github.dev-vikas-soni.lighthouse)
[![Version: 2.3.0](https://img.shields.io/badge/Version-2.3.0-orange.svg)](https://github.com/dev-vikas-soni/gradle-lighthouse/releases)
[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)
![Kotlin](https://img.shields.io/badge/kotlin-%237F52FF.svg?style=flat&logo=kotlin&logoColor=white)
![Gradle 8.x-9.x](https://img.shields.io/badge/Gradle-8.x--9.x-green.svg)

---

## Why use it

`gradle-lighthouse` is built for teams that want more than lint-style findings.

<img width="1512" height="783" alt="phase-2-ss-1" src="https://github.com/user-attachments/assets/bb098ecb-8a88-49bb-a024-3301efaadeac" />


<img width="1223" height="792" alt="actual_nia_project_dashbaord" src="https://github.com/user-attachments/assets/4357074f-6070-4d55-b152-395e15e1eb51" />


<img width="1223" height="785" alt="actual_nia_module_dashbaord" src="https://github.com/user-attachments/assets/9d708aa9-e34b-4c4e-86c5-fdcd5c994436" />

It helps you:
- detect circular dependencies and illegal layer crossings in multi-module builds
- spot risky dependency patterns, unused declarations, catalog hygiene gaps, and version conflicts
- track architectural health over time with build-to-build trend history
- explore module coupling visually in the aggregate HTML dashboard
- fail CI intentionally when the codebase crosses agreed architectural boundaries
- export results as self-contained HTML, SARIF, and JUnit XML

This is especially useful for:
- Android multi-module apps
- Kotlin Multiplatform repos
- platform / DevEx teams
- codebases moving toward stronger modular architecture

---

## What's New in v2.3 (Phase 3)

Version `2.3.0` moves beyond auditing into **Automation and Adoption**:

- **Baseline System**: Record current architectural debt with `./gradlew lighthouseRecordBaseline`. Focus your team only on *new* regressions.
- **Lighthouse Fix**: Automatically apply performance and best-practice fixes to your project with `./gradlew lighthouseFix`.
- **Remediation Recipes**: Every issue now links to a dedicated technical "Recipe" URL for detailed fix instructions.
- **Interactive Galaxy Graph**: Visual module dependency exploration with cycle highlighting.
- **Trend & Velocity Analytics**: Track health score, fatals, and coupling density over the last 30 builds.

---

## Quick start

### 1. Apply the plugin

Apply it to the root project and to each module you want audited.

```kotlin
// root build.gradle.kts
plugins {
    id("io.github.dev-vikas-soni.lighthouse") version "2.3.0"
}
```

```kotlin
// feature/login/build.gradle.kts
plugins {
    id("com.android.library")
    id("io.github.dev-vikas-soni.lighthouse") version "2.3.0"
}
```

No extra repository configuration is needed. The plugin is published on the Gradle Plugin Portal.

### 2. Run the audit

```bash
./gradlew lighthouseAudit lighthouseAggregate
```

### 3. Record a Baseline (Optional)

If you are adding Lighthouse to a legacy project, record a baseline to suppress existing issues:

```bash
./gradlew lighthouseRecordBaseline
```

### 4. Apply Auto-Fixes

```bash
./gradlew lighthouseFix
```

### 5. Open the reports

- per-module reports: `{module}/build/reports/lighthouse/`
- aggregate dashboard: `build/reports/lighthouse/project-dashboard.html`

---

## Tasks

| Task | Scope | Output |
|------|-------|--------|
| `lighthouseAudit` | Current module | HTML, SARIF, JUnit XML, module JSON for aggregation |
| `lighthouseAggregate` | Root multi-module project | Global dashboard, Galaxy Graph, trend charts, enforcement gates |
| `lighthouseRecordBaseline` | Module/Root | Records existing issues into `lighthouse-baseline.txt` to suppress them |
| `lighthouseFix` | Module/Root | Automatically applies deterministic fixes (caching, parallel, etc.) |

Recommended flow for multi-module projects:

```bash
./gradlew lighthouseAudit lighthouseAggregate
```

If you want per-module gating only, run a specific module task:

```bash
./gradlew :app:lighthouseAudit
```

---

## Configuration

The `lighthouse {}` block is optional.

### Actual defaults

- all auditors are enabled by default
- SARIF and JUnit XML are enabled by default
- `failOnSeverity` defaults to `"NONE"`
- aggregate gates default to off
- `targetVariant` defaults to `""`, which means Lighthouse inspects the standard available configurations instead of forcing a single variant

### Full DSL example

```kotlin
lighthouse {
    // ── Aggregate enforcement gates ─────────────────────────────────────────────
    failOnDependencyCycle.set(true)
    failOnLayerViolation.set(true)
    minHealthScore.set(85)

    // ── Per-module gate ────────────────────────────────────────────────────────
    failOnSeverity.set("ERROR")   // NONE | INFO | WARNING | ERROR | FATAL

    // ── Dependency/configuration targeting ─────────────────────────────────────
    targetVariant.set("release")  // default: "" (no single-variant restriction)

    // ── Auditor toggles ────────────────────────────────────────────────────────
    enableDependencyHealth.set(true)
    enablePlayPolicy.set(true)
    enableCatalogMigration.set(true)
    enableBuildSpeed.set(true)
    enableAppSize.set(true)
    enableStabilityCheck.set(true)
    enableConflictCheck.set(true)
    enableModernizationCheck.set(true)
    enableKmpCheck.set(true)
    enableConfigCacheCheck.set(true)
    enableModuleGraphCheck.set(true)
    enableUnusedDependencyCheck.set(true)
    enableTestCoverageCheck.set(true)
    enableVersionCatalogHygiene.set(true)
    enableSecurityCheck.set(true)
    enableModuleSizeCheck.set(true)
    enableTrendTracking.set(true)

    // ── Report formats ─────────────────────────────────────────────────────────
    enableSarifReport.set(true)
    enableJunitXmlReport.set(true)
}
```

---

## What it checks

`gradle-lighthouse` currently ships with 19 auditors across these areas:

### Build performance and build hygiene
- KAPT / KSP migration opportunities
- Jetifier, parallel build, and build cache flags
- Configuration Cache blockers
- startup and app size signals

### Module architecture
- circular dependencies
- feature-to-feature coupling
- module size and complexity trends
- coupling density and graph structure

### Dependency hygiene
- unused dependencies
- version drift and conflict intelligence
- version catalog migration gaps
- unused catalog aliases and bundle opportunities

### Security and compliance
- hardcoded secrets in Gradle properties
- signing config and toolchain safety
- wrapper/toolchain checks
- Play policy and target SDK checks

### Quality and modernization
- test presence and coverage setup
- manifest / ProGuard / R8 safety
- deprecated Android / Kotlin patterns
- KMP structure checks
- score trend tracking over time

---

## Reports

| Report | Path | Notes |
|--------|------|-------|
| Module HTML | `{module}/build/reports/lighthouse/{module}-index.html` | Self-contained human-readable report |
| Module SARIF | `{module}/build/reports/lighthouse/{module}-report.sarif` | For GitHub Security / code scanning |
| Module JUnit XML | `{module}/build/reports/lighthouse/{module}-report.xml` | For CI test views |
| Aggregate dashboard | `build/reports/lighthouse/project-dashboard.html` | Galaxy Graph + trend analytics + aggregate health |
| Global history | `.lighthouse/global-history.json` | Last 30 aggregate runs |
| Module history | `.lighthouse/{module}-history.json` | Per-module score history |

All HTML is self-contained. No CDN, no external fonts, no network dependency.

---

## Enforcement gates

The aggregate gates run only during `lighthouseAggregate` because they need the full module graph.

```kotlin
lighthouse {
    failOnDependencyCycle.set(true)
    failOnLayerViolation.set(true)
    minHealthScore.set(85)
}
```

### Supported aggregate gates

| Gate | Meaning |
|------|---------|
| `failOnDependencyCycle` | Fail if any global cycle is found |
| `failOnLayerViolation` | Fail if a lower-level layer depends on a higher-level layer |
| `minHealthScore` | Fail if the global average health score falls below the threshold |

For single-module or module-local CI behavior, use `failOnSeverity` instead.

---

## Custom architecture rules

Drop `lighthouse-rules.yaml` at the repository root.

```yaml
rules:
  - name: "Feature Isolation"
    condition: ":feature:* !-> :feature:*"
    level: "error"

  - name: "Standard Layering"
    condition: "App -> Feature -> Domain -> Data -> Core"
    level: "error"

  - name: "Core Purity"
    condition: ":core:* !-> :feature:*"
    level: "fatal"
```

### Supported rule levels

- `warning` → logged, does not fail the build
- `error` → fails the aggregate build
- `fatal` → fails the aggregate build

See `docs/ENTERPRISE_ENFORCEMENT.md` for the full rule syntax and CI examples.

---

## GitHub Actions

This repository also publishes a composite GitHub Action:

```yaml
- name: Run Gradle Lighthouse
  uses: dev-vikas-soni/gradle-lighthouse@v2.2.2
  with:
    fail-on-severity: 'ERROR'
    upload-sarif: 'true'
    comment-on-pr: 'true'
```

For full workflow examples, see `docs/USER_MANUAL.md` and `docs/ENTERPRISE_ENFORCEMENT.md`.

---

## Galaxy Graph

The aggregate dashboard includes a canvas-based dependency explorer.

Features include:
- inline graph view inside the dashboard
- fullscreen inspection mode
- cycle highlighting
- live dependency cutting in Sandbox Mode
- layer filtering and search
- PNG export
- node details and link details panels

This is meant to help teams answer: _what is tightly coupled right now, and what single edge would reduce the most risk?_

---

## Scoring model

```text
score = 100 × 0.98^(total_weighted_impact)
```

Severity weights:
- `FATAL = 35`
- `ERROR = 15`
- `WARNING = 5`
- `INFO = 1`

Rank bands:

| Score | Rank |
|-------|------|
| 95–100 | 🏆 Grandmaster Architect |
| 85–94 | ⭐ Expert Architect |
| 70–84 | 🔧 Standard Architect |
| 50–69 | ⚠️ At Risk |
| 0–49 | 🔴 Legacy |

---

## Documentation

| Document | What it covers |
|----------|----------------|
| [`docs/USER_MANUAL.md`](docs/USER_MANUAL.md) | installation, tasks, DSL reference, reports, CI usage |
| [`docs/ENTERPRISE_ENFORCEMENT.md`](docs/ENTERPRISE_ENFORCEMENT.md) | aggregate gates, YAML rules, enforcement behavior, pipelines |
| [`docs/HLD.md`](docs/HLD.md) | system-level design |
| [`docs/LLD.md`](docs/LLD.md) | implementation details and extension points |
| [`CHANGELOG.md`](CHANGELOG.md) | release history |
| [`ROADMAP.md`](ROADMAP.md) | planned work |
| [`CONTRIBUTING.md`](CONTRIBUTING.md) | contribution workflow |

---

## When this is overkill

If your project is a tiny single-module app and you only need dependency updates, `gradle-lighthouse` may be more than you need.

It becomes much more valuable when you have:
- multiple modules
- architecture boundaries to protect
- CI quality gates
- a need to visualize structural drift over time

---

## License

MIT — see [LICENSE](LICENSE).
