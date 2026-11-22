# Baseline system

Baselines are intended to help teams distinguish previously recorded findings from findings that appear after a baseline is created. Exact effects on reports, scores, and CI gates must be interpreted according to the implementation and plugin version.

## Recording a baseline

If the task is available in your plugin version, run:

```bash
./gradlew lighthouseRecordBaseline
```

Review the generated file before committing it. Keep the baseline in version control so CI and developers use the same reference.

## What a baseline is intended to do

A baseline stores fingerprints for findings already known to the team. On later runs, matching findings may be classified as existing/suppressed while unmatched findings are treated as new.

The fingerprint format and stability are implementation details. A change to a finding's category, title, path, or fingerprint algorithm may cause it to be reported again. Do not assume a fingerprint remains stable across every refactor or plugin upgrade.

## Important scoring distinction

Baseline filtering, current-state health scoring, report counts, and CI failure behavior are separate concepts. The intended contract is being made explicit and tested in [issue #53](https://github.com/dev-vikas-soni/gradle-lighthouse/issues/53). Until that work is complete, do not assume that recording a baseline removes the underlying finding from every score or report.

## Suggested workflow

1. Run an audit and review the findings.
2. Record a baseline only for debt the team has consciously accepted.
3. Commit the baseline.
4. Review new findings in subsequent runs.
5. Periodically remove resolved entries and reassess accepted debt.

A baseline is not a waiver of security review or a declaration that existing findings are harmless.
