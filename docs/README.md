# Gradle Lighthouse Documentation

Welcome to the official documentation for **Gradle Lighthouse**, the architectural intelligence engine for Android and Kotlin Multiplatform projects.

---

## 🚀 Getting Started
* [**Installation**](getting-started/installation.md): How to add Lighthouse to your project.
* [**Quick Start**](getting-started/quick-start.md): Run your first audit in 60 seconds.
* [**Configuration**](getting-started/configuration.md): Customize checks and enforcement gates.
* [**First Audit**](getting-started/first-audit.md): Walkthrough of your initial results.
* [**Understanding the Report**](getting-started/understanding-report.md): How to interpret scores and ranks.

## 🧠 Core Concepts
* [**What is Lighthouse?**](concepts/what-is-gradle-lighthouse.md): The problem and our philosophy.
* [**Snapshot Architecture**](concepts/snapshot-architecture.md): How we achieve 100% Configuration Cache safety.
* [**Audit Pipeline**](concepts/audit-pipeline.md): The journey from build script to finding.
* [**Health Score Model**](concepts/health-score.md): Understanding the square root deduction math.
* [**Galaxy Graph**](concepts/galaxy-graph.md): Interactive dependency visualization and sandbox.
* [**Baseline System**](concepts/baseline-system.md): Managing historical technical debt.
* [**Architectural Enforcement**](concepts/enforcement.md): Turning insights into build guardrails.

## 🛡️ Practical Guides
* [**CI/CD Integration**](guides/ci-cd.md): Using Lighthouse in your pipeline.
* [**GitHub Actions**](guides/github-actions.md): Setup the official composite action.
* [**Lighthouse PR Bot**](guides/pr-bot.md): Intelligent deltas for code review.
* [**Custom Rules**](guides/custom-rules.md): YAML-based layering and isolation rules.
* [**AI Remediation**](guides/ai-remediation.md): Automated KSP and TOML migrations.
* [**Multi-Module Strategy**](guides/multi-module.md): Best practices for large repositories.

## 📋 Reference
* [**Auditor Catalog**](reference/auditor-catalog.md): Detailed reference of every check.
* [**DSL Reference**](reference/dsl-reference.md): Every property in the `lighthouse {}` block.
* [**Task Reference**](reference/task-reference.md): CLI commands and options.
* [**Remediation Recipes**](reference/recipes.md): Solutions for common findings.

## 👩‍💻 Developer Resources
* [**Developer Architecture**](developer/architecture.md): Internal mechanics of the plugin.
* [**Repository Tour**](developer/repository-tour.md): A map of the codebase.
* [**Adding an Auditor**](developer/adding-an-auditor.md): Contributor guide for new checks.
* [**Design Principles**](developer/design-principles.md): The technical "Why" behind our choices.
* [**Architecture Decisions (ADR)**](developer/architecture-decisions.md): Log of major design choices.

## ❓ Support & FAQ
* [**FAQ**](faq.md): Frequently Asked Questions.
* [**Compatibility Matrix**](compatibility.md): Supported versions of Gradle, Java, and AGP.
* [**Troubleshooting**](troubleshooting.md): Solutions for common setup issues.
* [**Glossary**](glossary.md): Terminology used throughout the project.
