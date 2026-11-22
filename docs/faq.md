# Frequently asked questions

## Is Lighthouse a replacement for Detekt or Android Lint?

No. Those tools focus on source-level correctness, style, and Android-specific code checks. Lighthouse focuses on selected Gradle configuration and project/module diagnostics. They are complementary, and none replaces code review or tests.

## Does Lighthouse support Kotlin Multiplatform?

The plugin contains KMP-oriented checks, but their coverage is partial. They should not be treated as a complete analysis of all targets, source sets, or dependency relationships. See the [auditor catalog](reference/auditor-catalog.md).

## Will Lighthouse make my build slower?

The audit itself adds work to a Gradle invocation. Its overhead depends on project size, enabled checks, and environment. We do not make a universal "near-zero" overhead claim. Measure it in your own CI and share reproducible results.

## Which Gradle versions are supported?

Gradle 8+ is the documented minimum. Exact tested versions and combinations should be confirmed from release CI; see [compatibility](compatibility.md).

## What does the health score mean?

It is a heuristic derived from detected findings and configured weights. It is not a certification, industry percentile, measure of engineering quality, or prediction of production reliability. Baseline behavior also matters; see [the score model](concepts/health-score.md) and [baseline system](concepts/baseline-system.md).

## Does Lighthouse use AI to fix issues?

No. The current fix task uses deterministic, pattern-based transformations. The historical `--ai` flag and documentation wording should not be interpreted as LLM integration. Review all modifications manually.

## Are SDK impact predictions based on community measurements?

Do not interpret current SDK impact estimates as measured community benchmarks. Unless a report includes a reproducible dataset and methodology, treat such values as curated heuristics or do not rely on them for decisions.

## How do I report a false positive?

Open an issue with a minimal reproducer, expected behavior, actual finding, plugin version, Gradle/JDK versions, and AGP/Kotlin versions where applicable. False-positive reports are valuable contributions.
