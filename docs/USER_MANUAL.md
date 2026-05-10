# User Manual: Gradle Lighthouse V2.0

Welcome to Gradle Lighthouse — Build Intelligence for Android & Kotlin Multiplatform.

---

## 1. Installation

### 1.1 From Gradle Plugin Portal (Recommended)

Simply add to your `build.gradle.kts`:

```kotlin
plugins {
    id("io.github.dev-vikas-soni.lighthouse") version "2.0.2"
}
```

**That's it.** No repository configuration needed. No `resolutionStrategy`. Works immediately.

### 1.2 Multi-Module Setup

Apply to the **root** project for the aggregate dashboard, and to **each module** for per-module auditing:

```kotlin
// root build.gradle.kts
plugins {
    id("io.github.dev-vikas-soni.lighthouse") version "2.0.2"
}

// each module's build.gradle.kts
plugins {
    id("com.android.library")
    id("io.github.dev-vikas-soni.lighthouse") version "2.0.2"
}
```

---

## 2. Running Audits

### 2.1 Per-Module Audit
```bash
./gradlew :app:lighthouseAudit
```
**Output**: HTML report at `app/build/reports/lighthouse/app-index.html`

### 2.2 Full Project Audit
```bash
./gradlew lighthouseAudit lighthouseAggregate
```
**Output**: Global dashboard at `build/reports/lighthouse/project-dashboard.html`

### 2.3 Terminal Output

Every run prints a colorful dashboard summary:
```
┌──────────────────────────────────────────────────────────┐
│  🏗️  Gradle Lighthouse — Score: 72/100 (+8)              │
│  Rank: Standard → Expert 🎯                              │
├──────────────────────────────────────────────────────────┤
│  ✅ Build caching enabled                                 │
│  ✅ Parallel execution enabled                            │
│  ⚠️  3 unused dependencies found                          │
│  ❌ Configuration cache not compatible (2 tasks)          │
│  ❌ KAPT detected — migrate to KSP (save 104h/year)      │
├──────────────────────────────────────────────────────────┤
│  5 issues: 2 error · 2 warn · 1 info                     │
│  💡 Fix 2 errors to unlock Expert rank                    │
└──────────────────────────────────────────────────────────┘
```

---

## 3. Configuration (Optional)

Everything is **enabled by default**. Only add `lighthouse {}` to customize:

```kotlin
lighthouse {
    // === Core Targeting ===
    targetVariant.set("release")              // Only audit release deps

    // === Auditor Toggles (all true by default) ===
    enableBuildSpeed.set(true)                // KAPT→KSP, caching, Jetifier, BuildConfig
    enableConfigCacheCheck.set(true)          // Configuration Cache readiness
    enableModuleGraphCheck.set(true)          // Cycle detection, coupling, DOT graph
    enableUnusedDependencyCheck.set(true)     // Dead dependencies
    enableTestCoverageCheck.set(true)         // Dark module detection
    enableSecurityCheck.set(true)             // Secrets, signing, wrapper version
    enableVersionCatalogHygiene.set(true)     // TOML hygiene
    enableModuleSizeCheck.set(true)           // LOC, complexity metrics
    enableTrendTracking.set(true)             // Score history / delta
    enableDependencyHealth.set(true)          // Dynamic versions, leaked APIs
    enableAppSize.set(true)                   // Minification validation
    enableStabilityCheck.set(true)            // R8 safety, manifest compliance
    enableConflictCheck.set(true)             // Version conflict detection
    enableModernizationCheck.set(true)        // Compose vs XML ratio
    enableCatalogMigration.set(true)          // TOML migration readiness
    enablePlayPolicy.set(true)                // Play Store compliance
    enableKmpCheck.set(true)                  // KMP structure validation

    // === CI/CD Integration ===
    failOnSeverity.set("NONE")               // NONE | INFO | WARNING | ERROR | FATAL
    enableSarifReport.set(true)              // GitHub Security Tab
    enableJunitXmlReport.set(true)           // Jenkins / CI test tabs
}
```

---

## 4. What Gets Checked

### Build Performance
- KAPT → KSP migration opportunities (with ROI hours/year)
- Configuration Cache readiness (allprojects/subprojects, eager tasks, buildSrc)
- Build caching & parallel execution flags
- Jetifier still enabled
- Non-transitive R class not enabled
- Unnecessary BuildConfig generation
- Deprecated kotlin-android-extensions

### Module Architecture
- Circular dependencies (DFS cycle detection)
- Feature modules depending on other features
- High coupling score (>10 module deps)
- Module dependency graph visualization (DOT format)
- Module size & complexity (LOC, public API classes, build file lines)

### Dependencies
- Unused declared dependencies
- Duplicate declarations across configurations
- Silent major version jumps from conflict resolution
- Hardcoded versions (should use TOML catalog)
- Version catalog unused entries & bundle opportunities

### Security & Compliance
- Hardcoded secrets in gradle.properties
- Plain-text passwords in signing configs
- Outdated Gradle wrapper version
- Missing dependency locking
- Missing JDK toolchain config
- Dangerous Android permissions (Play Store risk)

### Quality
- Modules with zero test files ("dark modules")
- Low test-to-source ratio
- Missing JaCoCo configuration
- Library modules without consumer-rules.pro
- Missing ProGuard/R8 keep rules for reflection libraries
- Android Manifest exported component safety

### Modernization
- Compose vs XML layout ratio
- Startup performance (heavy SDK, ContentProvider bloat)
- App size (minification, resource shrinking, large assets)

---

## 5. CI/CD Integration

### 5.1 GitHub Actions (Full Integration)

```yaml
name: Architecture Audit
on: [pull_request]

jobs:
  lighthouse:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'temurin'

      - uses: dev-vikas-soni/gradle-lighthouse@v2
        with:
          fail-on-severity: 'ERROR'
          upload-sarif: 'true'
          comment-on-pr: 'true'
```

This will:
1. Run all audits
2. Upload SARIF → appears in GitHub Security tab with inline annotations
3. Post PR comment with score + delta
4. Fail the build if ERROR+ issues are found

### 5.2 Manual GitHub Actions Setup

```yaml
steps:
  - run: ./gradlew lighthouseAudit lighthouseAggregate

  - uses: github/codeql-action/upload-sarif@v3
    if: always()
    with:
      sarif_file: build/reports/lighthouse/
      category: gradle-lighthouse
```

### 5.3 Jenkins / GitLab / Bitrise

```bash
./gradlew lighthouseAudit -Plighthouse.failOnSeverity=ERROR
```

JUnit XML output: `build/reports/lighthouse/*-report.xml`
Configure your CI's test report parser to read these files.

### 5.4 Build Gate (Fail on Severity)

Set in DSL:
```kotlin
lighthouse {
    failOnSeverity.set("ERROR")  // Fails build if any ERROR or FATAL found
}
```

Or via command line:
```bash
./gradlew lighthouseAudit -Plighthouse.failOnSeverity=FATAL
```

---

## 6. Reports

| Report | Location | Format |
|--------|----------|--------|
| Module HTML | `{module}/build/reports/lighthouse/{module}-index.html` | Interactive HTML |
| Module SARIF | `{module}/build/reports/lighthouse/{module}-report.sarif` | SARIF v2.1.0 |
| Module JUnit | `{module}/build/reports/lighthouse/{module}-report.xml` | JUnit XML |
| Module JSON | `{module}/build/reports/lighthouse/module-report.json` | JSON (for aggregation) |
| Global Dashboard | `build/reports/lighthouse/project-dashboard.html` | Interactive HTML |
| Module Graph | `build/reports/lighthouse/module-graph.dot` | Graphviz DOT |

### Viewing DOT Graphs

```bash
# Install Graphviz
brew install graphviz  # macOS
apt install graphviz   # Linux

# Render
dot -Tsvg build/reports/lighthouse/module-graph.dot -o module-graph.svg
```

Or paste into [Graphviz Online](https://dreampuf.github.io/GraphvizOnline/).

---

## 7. Historical Trend Tracking

Lighthouse persists scores in `.lighthouse/` after each run:

```
.lighthouse/
  app-history.json
  feature-login-history.json
  core-network-history.json
```

**Recommended**: Commit `.lighthouse/` to your repo for team-wide visibility.

On subsequent runs, the terminal shows score delta: `Score: 72/100 (+8)`

If score drops >5 points, an ERROR-level finding is raised.

---

## 8. Scoring System

**Algorithm**: `score = 100 × 0.98^(total_weighted_impact)`

| Severity | Weight | Example |
|----------|--------|---------|
| FATAL | 35 | Missing exported= attribute (crash on Android 12+) |
| ERROR | 15 | KAPT still in use, config cache incompatible |
| WARNING | 5 | Jetifier enabled, unused dependencies |
| INFO | 1 | JaCoCo not configured, dependency locking missing |

| Rank | Score Range |
|------|-------------|
| 🏆 Grandmaster Architect | 95-100 |
| ⭐ Expert Architect | 85-94 |
| 🔧 Standard Architect | 70-84 |
| ⚠️ At Risk | 50-69 |
| 🔴 Legacy | 0-49 |

---

## 9. Understanding the ROI Engine

Each finding includes quantified business impact:

- **"Save 150h/year"**: Calculated developer waiting time saved by migrating KAPT → KSP
- **"Crash Prevention"**: Missing ProGuard rule = `ClassNotFoundException` in production
- **"30-50% faster builds"**: Non-transitive R class impact on resource compilation
- **"Supply chain security"**: Dependency locking prevents silent compromise

The ROI fields help prioritize: fix the highest-impact issues first.

---

## 10. Troubleshooting

| Issue | Solution |
|-------|----------|
| `Task not found` | Ensure plugin is applied in the module's build.gradle.kts |
| Empty report | Check `targetVariant` — set to `""` to audit all variants |
| False positive on unused deps | The heuristic checks imports; runtime-only deps may be flagged. Suppress via `enableUnusedDependencyCheck.set(false)` |
| Terminal colors garbled | Set `TERM=xterm-256color` in CI environment |
| Score seems too low | Run once, fix FATALs first (35 points each!), then re-run |
