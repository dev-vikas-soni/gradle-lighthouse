# DSL reference

This page summarizes selected extension properties documented by the project. Property availability and defaults can change between plugin versions; verify against the source and release you use.

## Core properties

### `targetVariant`
- **Type:** `Property<String>`
- **Default:** empty string
- **Description:** Android variant selection hint, where supported by the relevant checks.

### `failOnSeverity`
- **Type:** `Property<String>`
- **Default:** `NONE`
- **Values:** `NONE`, `INFO`, `WARNING`, `ERROR`, `FATAL`
- **Description:** Severity threshold used by the audit task. Baseline filtering and score semantics should be reviewed separately; see [issue #53](https://github.com/dev-vikas-soni/gradle-lighthouse/issues/53).

### `useAi`
The extension has historically exposed a `useAi` property. It must not be described as enabling a real GenAI integration: the current fix implementation uses deterministic pattern-based changes and does not call an LLM. Treat this property as a legacy/implementation detail until its API behavior is clarified or removed.

## Aggregate gates

### `failOnDependencyCycle`
- **Type:** `Property<Boolean>`
- **Default:** `false`
- **Description:** Configure aggregate-task failure when detected dependency cycles exist.

### `failOnLayerViolation`
- **Type:** `Property<Boolean>`
- **Default:** `false`
- **Description:** Configure aggregate-task failure for supported rules violations.

### `minHealthScore`
- **Type:** `Property<Int>`
- **Default:** `0`
- **Description:** Minimum score threshold for the aggregate task. The score is heuristic; do not use it as a quality certification or cross-project benchmark.

## Auditor toggles

The extension includes toggles for dependency hygiene, Android policy, build settings, app size, stability, KMP structure, module graph, test-related checks, and predictive intelligence. Exact property names/defaults are release-specific; consult `LighthouseExtension.kt` in the tag you use.

A test-source/file ratio is not code coverage. SDK impact values are not measured predictions without a documented measurement methodology.

## Telemetry

Do not assume telemetry is enabled or disabled based solely on this page. Before enabling any telemetry-related option, inspect the release's implementation and verify its default, destination, payload, consent flow, and opt-out behavior. The project should publish those details before presenting telemetry as privacy-first or community-sourced.

## Reports and baselines

The plugin exposes report and baseline-related configuration. Baseline behavior and the relationship between current-state scores and new findings are being clarified; see [issue #53](https://github.com/dev-vikas-soni/gradle-lighthouse/issues/53).
