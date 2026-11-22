# Auditor catalog

This catalog describes the intent of selected checks. It is not a guarantee that every possible issue in a category is detected. Detection is limited to the inputs and patterns implemented by each auditor. Verify important findings manually and consult source/tests for the exact behavior of the release you use.

## Dependency and build configuration

### DependencyAuditor
Inspects selected dependency declarations and repository configuration patterns, such as dynamic versions and legacy repository usage. It does not prove dependency safety or reproducibility.

### UnusedDependencyAuditor
Uses source/import and dependency information available to the plugin to identify possible unused direct dependencies. Import-based analysis can miss reflection, generated code, service loading, dependency injection, and other indirect usage. Treat results as candidates for review, not safe-to-delete instructions.

### ConflictIntelligenceAuditor
Reports selected dependency version-resolution patterns. It is not a full dependency vulnerability or compatibility analysis.

### BuildSpeedAuditor
Checks selected Gradle build settings and plugin/dependency patterns. It does not profile task execution or guarantee a specific build-time improvement. Any estimated savings should be treated as heuristic unless backed by measurements from your build.

### ConfigCacheReadinessAuditor
Checks selected configuration patterns that may be relevant to Configuration Cache readiness. A clean result does not certify compatibility; validate with Gradle's own Configuration Cache execution and the exact Gradle version in use.

## Security and Android policy

### SecurityAuditor
Checks selected build and project configuration patterns. It is not a substitute for secret scanning, dependency vulnerability scanning, signing review, or a security audit.

### PlayPolicyAuditor
Checks selected Android manifest or SDK-related patterns. It does not certify Google Play policy compliance; policy requirements change and require product-specific review.

## Module architecture and complexity

### ModuleGraphAuditor
Builds a module dependency representation from the data available to the plugin and detects cycles in that graph. The result is limited to relationships captured by the implementation.

### ModuleSizeAuditor
Uses available source/project metadata to report size-related signals. Such signals are not direct measurements of incremental build cost or maintainability.

## Kotlin Multiplatform

KMP-oriented checks exist, but target/source-set discovery and dependency analysis are partial. Do not assume all target hierarchies, custom source sets, convention plugins, or platform-specific dependency paths are covered. Improvements and fixtures are tracked in [issue #55](https://github.com/dev-vikas-soni/gradle-lighthouse/issues/55).

## SDK impact and predictive estimates

Any SDK impact numbers must be treated as estimates, not measured binary-size, method-count, or startup-time results, unless the output documents the exact measurement procedure, environment, and dataset. Do not describe these values as community-derived benchmarks without evidence of a sufficiently large, representative, consented dataset.

## Test-related checks

A source-file ratio or presence of test directories is not test coverage. Test discovery in multi-module repositories requires careful validation; do not interpret a "no tests" finding as proof that a repository has no tests until the relevant module paths have been inspected.

## Health score

The score is a heuristic summary of findings. It is not an industry ranking, quality certification, or measure of runtime behavior. See [health score model](../concepts/health-score.md).

## Contribute

Please submit minimal reproductions for missed findings and false positives. Every high-impact rule should eventually have positive and negative fixtures.
