# Gradle Lighthouse documentation

Gradle Lighthouse is an open-source Gradle plugin for selected Android, Kotlin, and Gradle project diagnostics. This documentation describes the current implementation where known and calls out areas that are heuristic, partial, or not yet validated.

## Getting started

- [Installation](getting-started/installation.md)
- [Quick start](getting-started/quick-start.md)
- [Configuration](getting-started/configuration.md)
- [First audit](getting-started/first-audit.md)
- [Understanding reports](getting-started/understanding-report.md)

## Concepts

- [What is Gradle Lighthouse?](concepts/what-is-gradle-lighthouse.md)
- [Audit pipeline](concepts/audit-pipeline.md)
- [Health score](concepts/health-score.md)
- [Galaxy Graph](concepts/galaxy-graph.md)
- [Baseline system](concepts/baseline-system.md)
- [Enforcement](concepts/enforcement.md)
- [Snapshot architecture](concepts/snapshot-architecture.md)

## Guides

- [CI/CD](guides/ci-cd.md)
- [GitHub Actions](guides/github-actions.md)
- [Custom rules](guides/custom-rules.md)
- [Multi-module projects](guides/multi-module.md)
- [Configuration Cache](guides/configuration-cache.md)
- [Isolated Projects](guides/isolated-projects.md)
- [Deterministic remediation](guides/ai-remediation.md) — historical filename; this is not an LLM feature.

## Reference

- [Auditor catalog and limitations](reference/auditor-catalog.md)
- [DSL reference](reference/dsl-reference.md)
- [Task reference](reference/task-reference.md)
- [Recipes](reference/recipes.md)
- [Compatibility](compatibility.md)
- [FAQ](faq.md)
- [Troubleshooting](troubleshooting.md)

## Contributing

See [CONTRIBUTING.md](../CONTRIBUTING.md), the [roadmap](../ROADMAP.md), and the [issue tracker](https://github.com/dev-vikas-soni/gradle-lighthouse/issues). Please report false positives and missing coverage with a minimal reproducer.
