# Roadmap

This roadmap distinguishes implemented behavior from work that is still planned or needs validation. A checked item means code exists; it does **not** imply comprehensive correctness, broad compatibility, or independent validation.

The immediate goal is to make Gradle Lighthouse dependable and transparent as an open-source diagnostic tool. New feature breadth is secondary to correctness, reproducibility, and honest documentation.

## Highest priority: correctness and trust

Trackable work is in the [GitHub issue tracker](https://github.com/dev-vikas-soni/gradle-lighthouse/issues). Issues marked `priority: high` are the current focus.

1. Define baseline semantics consistently across scores, reports, trends, and CI gates.
2. Fix multi-module test-source discovery and ensure findings describe what is actually measured.
3. Publish an auditor-by-auditor methodology and limitations reference; remove unsupported claims.
4. Add positive, negative, and regression fixtures for every high-impact check.
5. Establish and publish a tested Gradle / JDK / AGP / Kotlin compatibility matrix.
6. Verify Configuration Cache and Isolated Projects behavior with reproducible TestKit coverage before claiming support.
7. Document telemetry behavior precisely, including defaults, payload, destination, and opt-out. Do not collect or transmit data without clear disclosure and consent.

## Planned product direction

### Architecture regression testing

- [ ] Define explicit architecture contracts for module dependencies and layers.
- [ ] Compare base and head module graphs to identify newly introduced edges, cycles, and rule violations.
- [ ] Add stable finding identifiers and explain their stability limits.
- [ ] Produce machine-readable diagnostics suitable for CI and coding-agent workflows.
- [ ] Validate results against real Android repositories with maintainers reviewing false positives.

### Android and Kotlin Multiplatform diagnostics

- [ ] Improve KMP target and source-set discovery across real project layouts.
- [ ] Validate source-set dependency analysis for Android, iOS, and common targets.
- [ ] Add version-aware Android Gradle Plugin and Kotlin Multiplatform migration checks, backed by official compatibility data and fixtures.
- [ ] Clearly distinguish static configuration heuristics from measured build or runtime performance.

### Reliability and developer experience

- [ ] Expand TestKit coverage for supported Gradle versions and multi-project builds.
- [ ] Test report consistency across console, HTML, JSON, SARIF, and JUnit.
- [ ] Measure audit overhead on representative projects and publish the method and results.
- [ ] Improve actionable explanations while avoiding unsupported time-saved, binary-size, or startup-time estimates.

## Deferred / not currently promised

These are not capabilities users should rely on today:

- A real LLM-powered remediation engine.
- Community-derived or statistically valid SDK impact predictions.
- Industry percentile rankings or representative benchmark comparisons.
- Complete Android Studio / IDE integration.
- Cloud dashboards, cross-repository monitoring, or standalone CLI.
- Comprehensive bytecode, APK, runtime, or production behavior analysis.

They may be reconsidered only when there is a clear user need, a defensible implementation, tests, and transparent methodology.

## Contributing

Help prioritize through [issues](https://github.com/dev-vikas-soni/gradle-lighthouse/issues) and [discussions](https://github.com/dev-vikas-soni/gradle-lighthouse/discussions). Please challenge inaccurate findings and documentation; reproducible counterexamples are especially valuable.
