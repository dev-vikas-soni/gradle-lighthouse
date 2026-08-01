# What is Gradle Lighthouse?

**Architecture Intelligence for Android & Kotlin Multiplatform**

Gradle Lighthouse is a diagnostic engine for Gradle projects. Unlike traditional linters that focus on code syntax or style, Lighthouse analyzes the **structural health** of your project.

## The Problem: Architectural Rot

As a project grows, it becomes harder to maintain.

* Modules become highly coupled, leading to circular dependencies.
* Build scripts accumulate technical debt (KAPT, Jetifier, hardcoded versions).
* New features take longer to implement because the architecture is opaque.
* Build times explode because of misconfigured tools and caching.

## The Solution: Architectural Visibility

Lighthouse makes these invisible problems visible and actionable.

### 1. Unified Health Model
A transparent scoring engine that evaluates every module across multiple domains like Security, Performance, and Complexity.

### 2. Actionable Improvement Roadmap
The "Path to 90" feature tells you which 3 or 4 changes will have the largest impact on your build speed and maintenance cost.

### 3. Dependency Visualization
The **Galaxy Graph** provides an interactive map of your module graph, automatically detecting cycles and architectural layer leaks.

### 4. Automated Remediation
The `lighthouseFix` task can automatically apply best practices to your build scripts, including complex refactorings like KAPT-to-KSP and Version Catalog migration.

## Why Lighthouse?

Lighthouse is built on three core pillars:

1. **Enterprise Grade**: Hardened for projects with 500+ modules.
2. **Zero-Config**: Works out of the box with sensible industry standards.
3. **Gradle Native**: 100% compatible with Configuration Cache and Isolated Projects.

## Next Steps

* [Learn about Snapshot Architecture](snapshot-architecture.md)
* [Understand the Scoring Model](health-score.md)
