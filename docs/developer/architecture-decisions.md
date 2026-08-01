# Architecture Decision Records (ADR)

The log of significant technical decisions in the Gradle Lighthouse project.

## [ADR-001] Snapshot Architecture
* **Status**: Accepted
* **Context**: We need to support Gradle Configuration Cache and Parallel Execution.
* **Decision**: De-serialize project metadata into an `AuditContext` DTO during the configuration phase.
* **Consequences**: Task actions have no access to the live `Project` object, making the engine thread-safe and cacheable.

## [ADR-002] Square Root Scoring Model
* **Status**: Accepted
* **Context**: Simple linear deductions led to negative scores in large projects with many minor warnings.
* **Decision**: Adopt the formula: `Score = 100 - K * sqrt(Impact)`.
* **Consequences**: The first few violations have the highest impact, and the score remains positive regardless of project scale.

## [ADR-003] Self-Contained HTML Reports
* **Status**: Accepted
* **Context**: Reports need to work in offline CI environments and air-gapped repositories.
* **Decision**: Inline all CSS, JS, and data into a single HTML file. No CDNs allowed.
* **Consequences**: Reporting is 100% portable but resulting files can be large (approx. 500KB - 1MB).

## [ADR-004] Regex-Based Fix Engine
* **Status**: Accepted
* **Context**: We need to perform surgical refactors like KAPT-to-KSP without adding heavy compiler dependencies.
* **Decision**: Use surgical regex patterns on the `buildFileContent` string.
* **Consequences**: Extremely fast and low-dependency, but fragile for highly non-standard build scripts. Future plans involve an AST-based migration path.

## [ADR-005] Iterative DFS for Cycle Detection
* **Status**: Accepted
* **Context**: Deep module graphs (500+ modules) can cause `StackOverflowError` with traditional recursive DFS.
* **Decision**: Implement an iterative DFS using explicit stacks and color marking (White/Gray/Black).
* **Consequences**: Highly stable memory footprint on massive graphs.
