# Gradle Lighthouse 🏗️

**Build Intelligence for Android & Kotlin Multiplatform**

[![Gradle Plugin Portal](https://img.shields.io/gradle-plugin-portal/v/io.github.dev-vikas-soni.lighthouse?label=Gradle%20Plugin%20Portal&color=orange)](https://plugins.gradle.org/plugin/io.github.dev-vikas-soni.lighthouse)
[![Version: 2.1.0](https://img.shields.io/badge/Version-2.1.0-orange.svg)](https://github.com/dev-vikas-soni/gradle-lighthouse/releases)
[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)
![Kotlin](https://img.shields.io/badge/kotlin-%237F52FF.svg?style=flat&logo=kotlin&logoColor=white)
![Gradle 8.x-9.x](https://img.shields.io/badge/Gradle-8.x--9.x-green.svg)

> *"Google Lighthouse for your Gradle builds"* — One plugin, 20+ architectural checks, zero configuration.

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
│  💡 Fix 2 errors to unlock Expert rank                    │
└──────────────────────────────────────────────────────────┘
```

---

## ⚡ Quick Start (One Line)

```kotlin
// build.gradle.kts
plugins {
    id("io.github.dev-vikas-soni.lighthouse") version "2.1.0"
}
```

**That's it.** No configuration needed. Run:

```bash
./gradlew lighthouseAudit
```

---

## 🚀 Installation

### Single Module
```kotlin
plugins {
    id("io.github.dev-vikas-soni.lighthouse") version "2.1.0"
}
```

### Multi-Module (Recommended)
Apply to root `build.gradle.kts` for the aggregate dashboard, and each module for per-module auditing:

```kotlin
// root build.gradle.kts
plugins {
    id("io.github.dev-vikas-soni.lighthouse") version "2.1.0"
}

// each module build.gradle.kts
plugins {
    id("com.android.library")
    id("io.github.dev-vikas-soni.lighthouse") version "2.1.0"
}
```

Run full project analysis:
```bash
./gradlew lighthouseAudit lighthouseAggregate
```

---

## 🛠️ Zero Configuration (Convention over Configuration)

Everything is **enabled by default**. Only add the `lighthouse {}` block to customize:

```kotlin
lighthouse {
    targetVariant.set("release")              // Only audit release dependencies

    // Toggle specific auditors (all true by default)
    enableBuildSpeed.set(true)                // KAPT→KSP, caching, Jetifier, BuildConfig
    enableConfigCacheCheck.set(true)          // Configuration Cache readiness
    enableModuleGraphCheck.set(true)          // Cycle detection, coupling score, DOT graph
    enableUnusedDependencyCheck.set(true)     // Dead dependencies
    enableTestCoverageCheck.set(true)         // Dark module detection
    enableSecurityCheck.set(true)             // Hardcoded secrets, CVE awareness
    enableVersionCatalogHygiene.set(true)     // TOML hygiene
    enableModuleSizeCheck.set(true)           // LOC, complexity metrics
    enableTrendTracking.set(true)             // Historical score comparison
    enableAppSize.set(true)                   // Minification validation
    enableStabilityCheck.set(true)            // R8 safety, manifest compliance
    enableModernizationCheck.set(true)        // Compose vs XML ratio
    enablePlayPolicy.set(true)                // Play Store compliance
    enableKmpCheck.set(true)                  // KMP structure validation

    // CI/CD
    failOnSeverity.set("NONE")               // NONE | INFO | WARNING | ERROR | FATAL
    enableSarifReport.set(true)              // GitHub Security Tab integration
    enableJunitXmlReport.set(true)           // Jenkins/CI test tab integration
}
```

---

## 📊 What It Checks (20+ Rules)

### 🔴 Build Performance (Highest ROI)
| Check | What it detects |
|-------|----------------|
| **KAPT → KSP ROI** | Libraries still using KAPT, calculates hours saved/year |
| **Config Cache Readiness** | allprojects/subprojects blocks, eager task creation, Project access |
| **Build Caching** | Missing `org.gradle.caching=true` |
| **Parallel Execution** | Missing `org.gradle.parallel=true` |
| **Jetifier** | `android.enableJetifier=true` still active |
| **Non-Transitive R** | Missing `android.nonTransitiveRClass=true` |
| **BuildConfig Waste** | Modules generating unused BuildConfig.java |
| **buildSrc** | Suggests migration to composite builds |
| **Deprecated Plugins** | kotlin-android-extensions detection |

### 🔗 Module Architecture
| Check | What it detects |
|-------|----------------|
| **Circular Dependencies** | DFS-based cycle detection across module graph |
| **Feature Coupling** | Feature modules depending on other features |
| **High Coupling Score** | Modules with >10 direct module dependencies |
| **DOT Graph** | Generates `module-graph.dot` (use Graphviz to render) |

### 🗑️ Dependency Analysis
| Check | What it detects |
|-------|----------------|
| **Unused Dependencies** | Declared but never imported in source |
| **Duplicate Declarations** | Same dep in multiple configurations |
| **Version Conflicts** | Silent major version jumps from resolution |
| **Hardcoded Versions** | Deps not using version catalog |

### 🔐 Security & Compliance
| Check | What it detects |
|-------|----------------|
| **Hardcoded Secrets** | Passwords/tokens in gradle.properties |
| **Signing Config** | Plain-text passwords in build files |
| **Gradle Wrapper** | Outdated versions with known CVEs |
| **Dependency Locking** | Missing lockfiles for reproducibility |
| **JDK Toolchain** | Inconsistent JDK versions |
| **Play Store** | Dangerous permissions, target SDK |

### 🧪 Quality & Modernization
| Check | What it detects |
|-------|----------------|
| **Dark Modules** | Modules with zero test files |
| **Test Ratio** | Low test-to-source ratio |
| **JaCoCo** | Missing coverage configuration |
| **Consumer Rules** | Library modules without proguard rules |
| **Compose vs XML** | Modernization index |
| **Module Size** | LOC/complexity thresholds |
| **Version Catalog** | Unused TOML entries, bundle opportunities |

---

## 📈 Scoring & Ranks

Score uses exponential decay: `score = 100 × 0.98^(weighted_impact)`

| Rank | Score | Meaning |
|------|-------|---------|
| 🏆 Grandmaster | 95+ | Optimized for scale |
| ⭐ Expert | 85-94 | Production-ready architecture |
| 🔧 Standard | 70-84 | Good baseline, room to improve |
| ⚠️ At Risk | 50-69 | Technical debt accumulating |
| 🔴 Legacy | <50 | Urgent modernization needed |

---

## 🔄 CI/CD Integration

### GitHub Actions (Recommended)
```yaml
- uses: dev-vikas-soni/gradle-lighthouse@v2
  with:
    fail-on-severity: 'ERROR'    # Fail PR if errors found
    upload-sarif: 'true'         # Upload to Security tab
    comment-on-pr: 'true'        # Post score as PR comment
```

This will:
1. Run `lighthouseAudit` + `lighthouseAggregate`
2. Upload SARIF to GitHub Security tab (inline annotations)
3. Post a score summary comment on every PR
4. Fail the build if threshold exceeded

### Manual CI Setup
```yaml
steps:
  - run: ./gradlew lighthouseAudit lighthouseAggregate

  - uses: github/codeql-action/upload-sarif@v3
    with:
      sarif_file: build/reports/lighthouse/
```

### Jenkins / Others
```bash
./gradlew lighthouseAudit -Plighthouse.failOnSeverity=ERROR
# JUnit XML at: build/reports/lighthouse/*-report.xml
# SARIF at: build/reports/lighthouse/*-report.sarif
```

---

## 📊 Reports

| Command | Output |
|---------|--------|
| `./gradlew lighthouseAudit` | Per-module HTML + SARIF + JUnit XML |
| `./gradlew lighthouseAggregate` | Global Dashboard HTML |

Reports are generated at `build/reports/lighthouse/`.

---

## 📈 Historical Trend Tracking

Lighthouse stores scores in `.lighthouse/` directory. Commit this to your repo to track trends:

```bash
# After first run, you'll see:
.lighthouse/
  app-history.json
  feature-login-history.json
  ...
```

On subsequent runs, the terminal dashboard shows delta: `Score: 72/100 (+8)`.

---

## 🏗️ Architecture

```
gradle-lighthouse/
├── core/                 # Scoring engine, Auditor interface, AuditContext
├── auditors/             # 20+ rule implementations
├── extension/            # lighthouse {} DSL
├── task/                 # LighthouseTask, LighthouseAggregateTask
├── reporting/            # HTML, SARIF, JUnit XML generators
└── action.yml            # GitHub Action (composite)
```

All auditors are stateless and receive a serializable `AuditContext` — fully Configuration Cache compatible.

---

## 🚀 Publishing to Gradle Plugin Portal

This plugin is published to the [Gradle Plugin Portal](https://plugins.gradle.org/plugin/io.github.dev-vikas-soni.lighthouse).

To publish a new version:
```bash
# Set credentials (one-time)
# Get key/secret from https://plugins.gradle.org/u/YOUR_USERNAME
export GRADLE_PUBLISH_KEY=your_key
export GRADLE_PUBLISH_SECRET=your_secret

# Publish
./gradlew publishPlugins
```

Or use the CI workflow — push a commit starting with `release:` to `main`.

---

## 📄 License

MIT License — see [LICENSE](LICENSE) for details.

---

**Built for the Android Architect community.** Star ⭐ if Lighthouse improves your builds.

> *"The difference between a legacy app and a scaleable one is 50ms of build time compounded over 100 modules."*
