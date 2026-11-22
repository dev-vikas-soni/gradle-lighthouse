# Historical ADR: Health Score Engine V2

**Status: Historical — implementation and documentation supersede this proposal where they differ.**

This record preserves the original V2 scoring design. It is not a statement that every formula, calibration target, category, weighting, or recommendation described below is currently implemented or validated.

## Context

The scoring model was intended to avoid a single raw finding count and instead group diagnostic findings into domains. The project later evolved; consult the current source and [health-score documentation](../concepts/health-score.md) for behavior and limitations.

## Historical design

The original proposal included category-based scoring, severity weighting, dampened deductions, weighted aggregation, weakest-link behavior, and an improvement roadmap. The proposal used example values and expected project score ranges to communicate the design.

Those example ranges and calibration statements were not a reproducible benchmark. They should not be used to compare projects, assert industry ranking, or imply production-risk prediction.

## Current interpretation

- The health score is a project-specific heuristic, not a certification or industry benchmark.
- Scores should be based on the findings the current implementation actually produces.
- Baselines classify existing findings for regression gating; they should not be mistaken for an automatically improved current-state score.
- Finding-level explanations and reproducible evidence are more important than a score alone.
- Claims about empirical calibration require a published dataset, versions, methodology, and independently reproducible results.

## Non-goals

The score does not measure developer skill, certify code quality, replace code review or security analysis, or predict production incidents.

For contribution work, prefer small changes with tests and clear evidence over tuning a score to expected values.
