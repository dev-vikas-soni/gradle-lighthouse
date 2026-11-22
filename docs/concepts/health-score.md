# Health score model

The Lighthouse score is a **heuristic summary of findings**, not a direct measurement of architecture quality, maintainability, runtime reliability, security posture, or team productivity. It is useful only as a consistent signal within a project when the enabled checks and scoring implementation remain comparable.

## Scoring model

The implementation assigns weights to finding severities and derives category scores using a square-root deduction model. The exact constants and aggregation behavior are implementation details and may change between releases. Consult the source for the version you run before treating these values as a stable public contract.

A simplified description of the category calculation is:

```text
category score = 100 - (coefficient * sqrt(sum of finding weights))
```

The overall score combines category-level information. Do not compare scores across plugin versions, configurations, or unrelated repositories unless the scoring model, enabled auditors, and input data are known to be equivalent.

## What the score does not mean

- It is not an industry benchmark or percentile.
- It is not a certification or pass/fail statement about production quality.
- It does not measure actual build duration, APK size, startup time, test coverage, or security exploitability.
- It cannot account for architectural context that the available project metadata does not expose.
- A higher score does not guarantee a better architecture.

## Baselines

Baseline filtering and score calculation are distinct concerns. Until baseline semantics are fully specified and regression-tested across all report formats, interpret the score as the current implementation's summary of detected findings and consult the new/suppressed finding counts separately. See the [baseline system](baseline-system.md) and tracked [baseline scoring work](https://github.com/dev-vikas-soni/gradle-lighthouse/issues/53).

## Recommended use

Use the score as a navigation aid to identify areas for investigation. Prefer concrete findings, reproducible module relationships, and team-defined architecture rules over optimizing a numeric score.
