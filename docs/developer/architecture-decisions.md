# Architecture decision records

This page is an index of the architecture decisions documented in the repository. Each record is a design note, not a guarantee of compatibility or performance; verify current behavior against code and tests.

- [Health Score Engine V2](../adr/001-health-score-v2.md) — scoring model and its assumptions.
- [Snapshot Architecture](adr/ADR-001-snapshot-architecture.md) — task inputs and audit context.
- [Provider API](adr/ADR-002-provider-api.md) — lazy configuration through Gradle providers.
- [String-Pipe Serialization](adr/ADR-004-string-serialization.md) — serialized task data format.

Please keep decision records factual, concise, and synchronized with the implementation. Avoid adding claims such as “100% compatible,” “industry calibrated,” or performance guarantees without reproducible evidence.