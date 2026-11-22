# What is Gradle Lighthouse?

Gradle Lighthouse is an open-source Gradle plugin for inspecting selected aspects of Gradle projects, with a focus on Android and Kotlin Multiplatform repositories.

It collects project and build metadata exposed to the plugin, runs configured checks, and produces findings and reports. Its purpose is to make certain structural and build-configuration concerns easier to inspect—not to provide a complete model of a software system.

## What it can help investigate

- Module dependency relationships and detected cycles.
- Selected dependency declarations and Gradle configuration practices.
- Team-defined architecture constraints supported by the rules implementation.
- Findings across modules, with report formats intended for local review and CI.

## Important limitations

Static checks only see the information they are implemented to inspect. A finding may require human validation, and the absence of a finding does not prove the project is free of a problem.

Some checks are heuristics. A score is not a quality certification; source-file ratios are not test coverage; configuration flags are not measured build performance; and estimated SDK impacts are not empirical benchmarks unless accompanied by a reproducible measurement method.

Kotlin Multiplatform analysis is partial. The plugin should not currently be described as a comprehensive KMP architecture verifier.

## Design direction

The long-term direction is deterministic architecture regression testing: make team-defined module boundaries explicit, detect changes that violate those contracts, and provide reproducible diagnostics in Gradle and CI. This is a roadmap direction, not a claim that complete base-versus-head architecture drift detection is already available.

See the [roadmap](../../ROADMAP.md), [auditor catalog](../reference/auditor-catalog.md), and [compatibility notes](../compatibility.md).
