# Glossary

### AuditContext
A serializable DTO that captures the "ground truth" of a module's build state.

### Auditor
A stateless diagnostic rule that analyzes the `AuditContext` and returns a list of `AuditIssues`.

### Baseline
A suppression file (`lighthouse-baseline.txt`) used to ignore historical technical debt.

### Coupling Density
The ratio of module-to-module links vs. the number of modules. Lower is better.

### Dark Module
A module that contains source code but no corresponding tests.

### Galaxy Graph
An interactive force-directed graph of the project's module dependencies.

### Health ROI
The estimated score gain divided by the engineering effort required to fix an issue.

### Square Root Deduction
The mathematical model used to ensure fair scoring across projects of different scales.

### XML Monolith
A module with a high number of legacy XML layouts and near-zero Jetpack Compose adoption.
