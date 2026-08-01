# Gradle Lighthouse 🛡️

**Architecture Intelligence for Android & Kotlin Multiplatform**

Gradle Lighthouse analyzes your Gradle project, detects architectural risks, evaluates build health, and generates actionable recommendations to improve maintainability, scalability, and developer productivity.

[![Gradle Plugin Portal](https://img.shields.io/gradle-plugin-portal/v/io.github.dev-vikas-soni.lighthouse?label=Gradle%20Plugin%20Portal&color=orange)](https://plugins.gradle.org/plugin/io.github.dev-vikas-soni.lighthouse)
[![Version: 2.3.2](https://img.shields.io/badge/Version-2.3.2-orange.svg)](https://github.com/dev-vikas-soni/gradle-lighthouse/releases)
[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)
![Kotlin](https://img.shields.io/badge/kotlin-%237F52FF.svg?style=flat&logo=kotlin&logoColor=white)
![Gradle 8.x-9.x](https://img.shields.io/badge/Gradle-8.x--9.x-green.svg)

---

## 💡 Why Gradle Lighthouse?

Most teams know they have technical debt. What they don't know is:
*   Which modules are slowing down incremental builds?
*   Where are architectural boundaries leaking?
*   Which technical debt deserves attention first?
*   How has our architecture evolved over the last 30 builds?

Gradle Lighthouse answers these questions with a single audit.

```bash
./gradlew lighthouseAudit lighthouseAggregate
```

---

## ✨ Key Features

### 📈 Architectural Health Score
A transparent health model that evaluates Security, Build Performance, Dependency Hygiene, Modernization, and more. Uses a **Square Root Deduction Model** to ensure fair scoring across massive projects.

### 🌌 Galaxy Graph
An interactive module dependency map with:
* **Cycle Detection**: Glowing red edges highlight circular dependencies.
* **Refactoring Sandbox**: Simulate refactors by "cutting" links to see instant score impacts.
* **Layer Visualization**: Modules grouped by App, Feature, Domain, Data, and Core.

### 🤖 GenAI Remediation
Go beyond diagnosis. Lighthouse can surgically refactor your build scripts:
* **KAPT to KSP**: Automatic migration of annotation processors.
* **Version Catalogs**: Bootstrap your `libs.versions.toml` automatically.

### 🔮 Predictive Intelligence
Estimate the architectural footprint of an SDK *before* merging it. See binary size, method count, and startup impact based on community data.

---

## 📋 Compatibility

| Feature | Supported Versions |
| :--- | :--- |
| **Gradle** | 8.0 - 9.x+ |
| **Java** | 17, 21+ |
| **Android (AGP)** | 7.4 - 8.x+ |
| **Kotlin** | 1.9.x, 2.x |

### 🛡️ PR Bot & CI/CD
Automated PR comments showing score deltas, new cycles, and regressions. Full integration with **GitHub Actions**, SARIF, and JUnit.

---

## 📋 Compatibility

| Feature | Supported Versions |
| :--- | :--- |
| **Gradle** | 8.0 - 9.x+ |
| **Java** | 17, 21+ |
| **Android (AGP)** | 7.4 - 8.x+ |
| **Kotlin** | 1.9.x, 2.x |

---

## 🚀 Quick Start

### 1. Installation
Apply to your root project:

```kotlin
plugins {
    id("io.github.dev-vikas-soni.lighthouse") version "2.3.2"
}
```

### 2. Run Audit
```bash
./gradlew lighthouseAudit lighthouseAggregate
```

### 3. View Reports
* **Global Dashboard**: `build/reports/lighthouse/project-dashboard.html`
* **Module Audit**: `module/build/reports/lighthouse/index.html`

---

## 📖 Documentation

* [**Getting Started**](docs/getting-started/installation.md)
* [**Concepts**](docs/concepts/what-is-gradle-lighthouse.md)
* [**CI/CD Guide**](docs/guides/ci-cd.md)
* [**Auditor Catalog**](docs/reference/auditor-catalog.md)
* [**Developer Architecture**](docs/developer/architecture.md)

---

## 🤝 Contributing

We welcome contributions! See the [Contributing Guide](CONTRIBUTING.md) and [Developer Docs](docs/developer/repository-tour.md) to get started.

## 📄 License
MIT License.
