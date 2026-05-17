# User Manual

> Gradle Lighthouse v2.2.0 | Plugin ID: `io.github.dev-vikas-soni.lighthouse`

---

## Table of Contents

1. [Installation](#1-installation)
2. [Running audits](#2-running-audits)
3. [Configuration reference](#3-configuration-reference)
4. [Auditors](#4-auditors)
5. [CI/CD enforcement](#5-cicd-enforcement)
6. [Reports](#6-reports)
7. [Galaxy Graph and dashboard](#7-galaxy-graph-and-dashboard)
8. [Custom architecture rules](#8-custom-architecture-rules)
9. [Scoring](#9-scoring)
10. [GitHub Actions](#10-github-actions)
11. [Troubleshooting](#11-troubleshooting)

---

## 1. Installation

### From the Gradle Plugin Portal

Apply the plugin in the root project:

```kotlin
plugins {
    id("io.github.dev-vikas-soni.lighthouse") version "2.2.0"
}
```

No extra repositories or `resolutionStrategy` blocks are required.

### Multi-module setup

Apply Lighthouse to the root project for aggregation and to each module you want audited:

```kotlin
// root build.gradle.kts
plugins {
    id("io.github.dev-vikas-soni.lighthouse") version "2.2.0"
}

// feature/login/build.gradle.kts
plugins {
    id("com.android.library")
    id("io.github.dev-vikas-soni.lighthouse") version "2.2.0"
}
```

For larger repositories, centralize the version in your convention plugin or version catalog to avoid repeating it in every module build file.

---

## 2. Running audits

### Per-module audit

```bash
./gradlew :app:lighthouseAudit
```

Typical outputs:
- `app/build/reports/lighthouse/app-index.html`
- `app/build/reports/lighthouse/app-report.sarif`
- `app/build/reports/lighthouse/app-report.xml`

### Full project audit and aggregation

```bash
./gradlew lighthouseAudit lighthouseAggregate
```

Outputs:
- per-module reports under `{module}/build/reports/lighthouse/`
- aggregate dashboard at `build/reports/lighthouse/project-dashboard.html`

### CI-friendly sequence

```bash
./gradlew lighthouseAudit lighthouseAggregate --no-daemon --stacktrace
```

`lighthouseAudit` prints a terminal dashboard for each module, including score, rank, counts by severity, and the most important open issues.

---

## 3. Configuration reference

The `lighthouse {}` block is optional.

### Real defaults

| Property | Default |
|----------|---------|
| `targetVariant` | `""` |
| `failOnSeverity` | `"NONE"` |
| `failOnDependencyCycle` | `false` |
| `failOnLayerViolation` | `false` |
| `minHealthScore` | `0` |
| all `enable*Check` / `enable*` auditor toggles | `true` |
| `enableSarifReport` | `true` |
| `enableJunitXmlReport` | `true` |

`targetVariant = ""` means Lighthouse scans the standard relevant configurations rather than forcing a single named variant.

### Full DSL

```kotlin
lighthouse {
    // ── Aggregate gates ────────────────────────────────────────────────────────
    failOnDependencyCycle.set(true)
    failOnLayerViolation.set(true)
    minHealthScore.set(85)

    // ── Per-module build gate ─────────────────────────────────────────────────
    failOnSeverity.set("ERROR")   // NONE | INFO | WARNING | ERROR | FATAL

    // ── Variant targeting ─────────────────────────────────────────────────────
    targetVariant.set("release")  // default: ""

    // ── Core auditor toggles ──────────────────────────────────────────────────
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

    // ── Report format toggles ─────────────────────────────────────────────────
    enableSarifReport.set(true)
    enableJunitXmlReport.set(true)
}
```

### Scope notes

- `failOnSeverity` is evaluated by `lighthouseAudit`
- `failOnDependencyCycle`, `failOnLayerViolation`, and `minHealthScore` are evaluated by `lighthouseAggregate`
- aggregate gates only make sense when the root project can see the full module graph

---

## 4. Auditors

Gradle Lighthouse currently ships with 19 auditors.

### Build performance and build hygiene

| Auditor | What it checks |
|---------|----------------|
| `BuildSpeedAuditor` | KAPT/KSP opportunities, Jetifier, build speed flags |
| `ConfigCacheReadinessAuditor` | Configuration Cache blockers, eager task creation, `allprojects` / `subprojects` issues |
| `StartupPerformanceAuditor` | startup-impacting patterns |
| `AppSizeAuditor` | app or module size signals |

### Module architecture

| Auditor | What it checks |
|---------|----------------|
| `ModuleGraphAuditor` | cycles, feature coupling, graph structure, coupling density |
| `ModuleSizeAuditor` | LOC, public API surface, module complexity |
| `TrendTrackingAuditor` | score delta and historical movement |

### Dependency hygiene

| Auditor | What it checks |
|---------|----------------|
| `DependencyAuditor` | dependency health issues |
| `UnusedDependencyAuditor` | declared but unused dependencies |
| `ConflictIntelligenceAuditor` | Gradle-resolved major version jumps |
| `CatalogMigrationAuditor` | hardcoded versions that belong in the version catalog |
| `VersionCatalogHygieneAuditor` | unused aliases, bundle opportunities, catalog hygiene |

### Security, quality, and compliance

| Auditor | What it checks |
|---------|----------------|
| `SecurityAuditor` | secrets, wrapper/toolchain checks, dependency locking, signing safety |
| `TestCoverageAuditor` | missing tests, low coverage signals, JaCoCo presence |
| `ProguardSafetyAuditor` | R8 / ProGuard configuration safety |
| `ManifestAuditor` | manifest issues |
| `ModernizationAuditor` | deprecated Android / Kotlin patterns |
| `PlayPolicyAuditor` | Play policy and target SDK checks |
| `KmpStructureAuditor` | Kotlin Multiplatform structure checks |
| `Stability`-related checks | surfaced via enabled stability toggles and auditors above |

---

## 5. CI/CD enforcement

The aggregate gates are evaluated only by `lighthouseAggregate`.

```kotlin
lighthouse {
    failOnDependencyCycle.set(true)
    failOnLayerViolation.set(true)
    minHealthScore.set(85)
}
```

### Aggregate gate behavior

| Gate | Meaning |
|------|---------|
| `failOnDependencyCycle` | Fail if any project-wide cycle is detected |
| `failOnLayerViolation` | Fail if a lower-level layer depends on a higher-level layer |
| `minHealthScore` | Fail if the aggregate average score is below the threshold |

### Scenarios

| Scenario | Aggregate gates active? |
|----------|--------------------------|
| `./gradlew lighthouseAudit` | No |
| `./gradlew lighthouseAggregate` in a multi-module repo | Yes |
| Single-module project | Usually not meaningful |

For local or per-module CI gating, use:

```kotlin
lighthouse {
    failOnSeverity.set("FATAL")
}
```

For detailed examples, see [`docs/ENTERPRISE_ENFORCEMENT.md`](ENTERPRISE_ENFORCEMENT.md).

---

## 6. Reports

| Report | Path | Purpose |
|--------|------|---------|
| Module HTML | `{module}/build/reports/lighthouse/{module}-index.html` | human-readable module report |
| Module SARIF | `{module}/build/reports/lighthouse/{module}-report.sarif` | GitHub Security / code scanning |
| Module JUnit XML | `{module}/build/reports/lighthouse/{module}-report.xml` | CI test tabs |
| Module JSON | `{module}/build/reports/lighthouse/module-report.json` | aggregation input |
| Aggregate HTML | `build/reports/lighthouse/project-dashboard.html` | global dashboard |
| Global history | `.lighthouse/global-history.json` | aggregate trend history |
| Module history | `.lighthouse/{module}-history.json` | per-module score history |

All generated HTML is self-contained.

---

## 7. Galaxy Graph and dashboard

The aggregate dashboard contains two major visualization layers:

### Trend & Velocity chart
- health score over time
- fatal count over time
- coupling density over time
- persisted in `.lighthouse/global-history.json`

### Galaxy Graph
- canvas-based module dependency graph
- layer-based grouping
- cycle highlighting
- fullscreen mode
- sandbox edge cutting
- node and link inspection panels
- PNG snapshot export
- XP / rank / score framing in the dashboard UI

Use the dashboard when you need to answer:
- where are the strongest couplings?
- which edges create cycles?
- how has the architecture changed over the last few builds?
- what breaks if we enforce stricter boundaries now?

---

## 8. Custom architecture rules

Create `lighthouse-rules.yaml` at the repository root.

```yaml
rules:
  - name: "Feature Isolation"
    condition: ":feature:* !-> :feature:*"
    level: "error"

  - name: "Core Purity"
    condition: ":core:* !-> :feature:*"
    level: "fatal"

  - name: "Standard Layering"
    condition: "App -> Feature -> Domain -> Data -> Core"
    level: "error"
```

### Supported constructs

| Construct | Syntax | Meaning |
|-----------|--------|---------|
| Layering rule | `A -> B -> C` | defines the allowed high-to-low dependency order |
| Isolation rule | `:feature:* !-> :feature:*` | forbids edges matching both sides |
| Wildcard | `*` | wildcard matching in module-path patterns |

### Supported levels

| Level | Behavior |
|-------|----------|
| `warning` | logged, does not fail the aggregate build |
| `error` | fails the aggregate build |
| `fatal` | fails the aggregate build |

Use only `warning`, `error`, or `fatal`. Other values are treated like build-failing errors by the current implementation.

---

## 9. Scoring

### Formula

```text
score = 100 × 0.98^(total_weighted_impact)
```

### Severity weights

| Severity | Weight |
|----------|--------|
| `FATAL` | 35 |
| `ERROR` | 15 |
| `WARNING` | 5 |
| `INFO` | 1 |

### Rank table

| Score | Rank |
|-------|------|
| 95–100 | 🏆 Grandmaster Architect |
| 85–94 | ⭐ Expert Architect |
| 70–84 | 🔧 Standard Architect |
| 50–69 | ⚠️ At Risk |
| 0–49 | 🔴 Legacy |

---

## 10. GitHub Actions

### Composite action

```yaml
# .github/workflows/lighthouse.yml
name: Lighthouse

on:
  pull_request:
    branches: [ main ]

permissions:
  security-events: write
  pull-requests: write

jobs:
  audit:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: '17'

      - name: Run Gradle Lighthouse
        uses: dev-vikas-soni/gradle-lighthouse@v2.2.0
        with:
          fail-on-severity: 'ERROR'
          upload-sarif: 'true'
          comment-on-pr: 'true'
```

### Manual setup

```yaml
- name: Run Lighthouse
  run: ./gradlew lighthouseAudit lighthouseAggregate

- name: Upload SARIF
  uses: github/codeql-action/upload-sarif@v4
  if: always()
  with:
    sarif_file: '**/build/reports/lighthouse/*.sarif'

- name: Upload HTML reports
  uses: actions/upload-artifact@v4
  if: always()
  with:
    name: lighthouse-reports
    path: '**/build/reports/lighthouse/'
```

Required permission for SARIF upload:

```yaml
permissions:
  security-events: write
```

---

## 11. Troubleshooting

### `failOnDependencyCycle` is not triggering

That gate runs during `lighthouseAggregate`, not `lighthouseAudit`.

```bash
./gradlew lighthouseAudit lighthouseAggregate
```

### Module reports overwrite each other

This was fixed in `2.1.1`. Upgrade if you still see modules with the same simple name colliding.

### Configuration Cache errors

If you see configuration-cache serialization problems, first confirm you are on `2.1.1+`. Then inspect your own build logic for task actions or custom plugins that still capture live `Project` references.

### SARIF upload fails in GitHub Actions

Make sure the workflow has:

```yaml
permissions:
  security-events: write
```

### The aggregate dashboard looks stale

Delete `build/reports/lighthouse/` and rerun:

```bash
./gradlew lighthouseAudit lighthouseAggregate
```

### Trend chart is not showing history yet

The aggregate history file is populated across runs. Execute `lighthouseAggregate` more than once to build up data in `.lighthouse/global-history.json`.

