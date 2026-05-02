# High-Level Technical Design Document (HLD)

## 1. Project Overview

**Gradle Lighthouse** is an enterprise-grade Gradle diagnostic engine designed for Android and Kotlin Multiplatform (KMP) codebases. Its primary objective is to scale to 1M+ developers by providing instantaneous, architectural intelligence during the build process.

The plugin acts as an automated "Principal Engineer," intercepting the build pipeline to scan for structural flaws, performance bottlenecks, and compliance violations, ultimately saving hundreds of engineering hours per year across large organizations.

## 2. System Architecture

The plugin is designed to run locally on developer machines and in CI/CD environments seamlessly. It relies heavily on Gradle's modern features to ensure lightning-fast execution without breaking caching mechanisms.

```mermaid
graph TD
    A[Gradle Configuration Phase] -->|Plugin Applied| B(LighthousePlugin)
    B -->|Registers Extensions| C[LighthouseExtension]
    B -->|Registers Tasks| D[LighthouseTask]
    D -.->|Extracts Context| E((AuditContext))
    
    subgraph Execution Phase [Gradle Execution Phase (Isolated)]
        E --> F[Auditor Engine]
        F --> G1[DependencyAuditor]
        F --> G2[ManifestAuditor]
        F --> G3[BuildSpeedAuditor]
        F --> G4[ModernizationAuditor]
        F --> G5[... 7 other Auditors]
    end
    
    G1 --> H{HealthScoreEngine}
    G2 --> H
    G3 --> H
    G4 --> H
    G5 --> H
    
    H --> I[Reporting Layer]
    I --> J1[HTML Dashboard]
    I --> J2[SARIF Report]
    I --> J3[JUnit XML Report]
```

### 2.1 Core Components

1.  **LighthousePlugin (Entry Point)**: Applies the DSL extension (`lighthouse { }`) and registers tasks across the project hierarchy.
2.  **AuditContext (The Snapshot)**: A `Serializable` data class that captures the entire project state (dependencies, manifest, properties) during the Configuration Phase. This entirely decouples the analysis logic from the live `org.gradle.api.Project` object.
3.  **Auditor Engine**: An extensible pipeline of `Auditor` interfaces. Each auditor receives the `AuditContext` and returns a list of `AuditIssue` objects.
4.  **Health Score Engine**: Applies an exponential decay algorithm based on the severity of findings to calculate an overarching "Architecture Rank" (e.g., A, B, C, F).
5.  **Reporting Layer**: Converts findings into human-readable HTML, security-standard SARIF (for GitHub Advanced Security), and JUnit XML (for CI test tabs).

## 3. Design Decisions & Constraints

### 3.1 Configuration Cache Compatibility
**Problem**: Traditional Gradle plugins often fail when Gradle's Configuration Cache is enabled, due to accessing the `Project` object during the execution phase.
**Solution**: Gradle Lighthouse strictly adheres to the input/output serialization model. The `AuditContext` snapshot is generated during configuration and passed as an `@Input` to the execution phase. This guarantees 100% compatibility with Gradle 8.x+ Configuration Caches.

### 3.2 Isolated Projects Compatibility
**Problem**: In Gradle 9.x, subprojects will be entirely isolated in memory. A root task cannot reach into a subproject to read its data directly.
**Solution**: The `LighthouseAggregateTask` does not read subprojects. Instead, each module runs its own `LighthouseTask` which outputs a JSON/HTML report to its local `build/` directory. The root aggregate task simply declares the output directories of the subtasks as its `@InputFiles`. Gradle handles the synchronization safely.

### 3.3 Zero-Dependency Reporting
**Problem**: Enterprise networks often block external CDNs or impose strict egress rules, rendering external CSS/JS frameworks useless.
**Solution**: The HTML report generator embeds all CSS (vanilla flexbox/grid) and SVG icons directly inline. The report is a single, portable, self-contained HTML file.

## 4. Execution Flow

1.  **Initialization**: `settings.gradle.kts` resolves the plugin. `build.gradle.kts` applies it.
2.  **Configuration**: Gradle configures the graph. `LighthousePlugin` captures the module state into `AuditContext`.
3.  **Task Graph Ready**: Gradle calculates dependencies. `lighthouseAudit` is placed in the queue.
4.  **Execution**: `LighthouseTask.execute()` begins.
5.  **Analysis**: The context is passed to all enabled `Auditor` implementations.
6.  **Scoring**: Issues are tallied; the module receives a grade.
7.  **Emission**: The HTML, SARIF, and JUnit XML files are written to disk.
8.  **Aggregation (Optional)**: If invoked at the root, `lighthouseAggregate` combines all module reports into a holistic dashboard.
