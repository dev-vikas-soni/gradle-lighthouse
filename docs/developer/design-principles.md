# Design Principles

The technical "Philosophy of Lighthouse."

## 1. Absolute Decoupling (Snapshot First)
We never pass `org.gradle.api.Project` into an auditor. Auditors must remain pure logic engines that work on a data snapshot. This ensures thread-safety and compatibility with any future Gradle execution models.

## 2. Zero External Runtime Dependencies
The plugin should be as small as possible.
* **Why**: Large dependencies in build plugins can lead to version conflicts with the project being audited.
* **Practice**: Avoid heavy libraries like Jackson or SnakeYAML. Use Kotlin string manipulation or built-in Java/Gradle APIs.

## 3. High Performance by Default
Architectural guardrails should not make the developer loop noticeably slower.
* **Configuration Phase**: Zero IO. Use `Providers`.
* **Execution Phase**: Audit results are cached by Gradle automatically if inputs haven't changed.
* **Reporting**: Fast string-buffer aggregation.

## 4. Explainable Scoring
Every point lost in the health score must be traceable to a specific, fixable finding. We avoid "Black Box" algorithms. The square root formula is used to ensure the math remains fair as project scale grows.

## 5. Visual First Intelligence
A graph is worth a thousand lines of log output. We prioritize rich, interactive visualizations like the Galaxy Graph to help developers navigate complex dependency trees.

## 6. Deterministic Remediation
Lighthouse should offer fixes that are deterministic. If we can't guarantee a clean refactor, we provide a "Recipe" instead of an automated fix.
