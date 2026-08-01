# ADR-001: Snapshot Architecture

## Context
Standard Gradle plugin development often involves accessing the `Project` object inside task actions. However, with the introduction of the Configuration Cache and Isolated Projects, direct access to `Project` at execution time is deprecated or forbidden. We need a way to run deep architectural analysis without violating these constraints.

## Decision
We decided to adopt a **Snapshot Architecture**.

1. **Configuration Phase**: We capture all project metadata (dependencies, properties, paths) using lazy Gradle `Provider` APIs.
2. **Execution Phase**: We reconstruct a stateless DTO called `AuditContext` from these inputs.
3. **Auditors**: Every diagnostic rule (`Auditor`) is a pure function that takes the `AuditContext` and returns a list of issues.

## Consequences
* **Positive**: 100% compatibility with Configuration Cache. Tasks can run in parallel across modules safely. Fast configuration since no heavy work is done during sync.
* **Negative**: All data needed by auditors must be identified and captured during the configuration phase. Adding a new type of check may require updating the snapshot logic.

## Alternatives Considered
* **Eager Analysis**: Running the analysis during configuration. *Rejected* because it significantly slows down IDE sync.
* **Lazy Project Access**: Using `Project` but with guarded access. *Rejected* because it's fundamentally incompatible with future Gradle versions.
