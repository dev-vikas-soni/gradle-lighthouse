# Task reference

Task availability and outputs depend on plugin version and project configuration. Confirm the tasks in your build with `./gradlew tasks --all`.

## `lighthouseAudit`

Runs enabled diagnostics for the project/module where the task is registered. It can produce console output and configured reports. The auditor catalog describes the scope and limitations of the checks.

## `lighthouseAggregate`

Aggregates available module reports into a project-level report/visualization. It can evaluate configured aggregate gates. Run module audits first where required by your build configuration.

## `lighthouseFix`

Applies a limited set of deterministic, pattern-based transformations. It may modify build files. Use a clean working tree, inspect the diff, and run your normal validation. It is not an LLM-powered feature; the historical `--ai` option should not be understood as GenAI integration.

## `lighthouseRecordBaseline`

Records current findings in a baseline file. The relationship between baseline filtering, scores, report counts, and CI gates is being clarified and regression-tested in [issue #53](https://github.com/dev-vikas-soni/gradle-lighthouse/issues/53).

## Benchmark-related tasks

Benchmark snapshot/status tasks may exist in the plugin. Embedded snapshots or percentile outputs should not be interpreted as statistically representative industry benchmarks. Review the dataset size, provenance, version compatibility, and methodology before using such values.

## Common CLI options

### Fail on severity

```bash
./gradlew lighthouseAudit -Plighthouse.failOnSeverity=ERROR
```

### Base reports

```bash
./gradlew lighthouseAggregate -Plighthouse.baseReportDir=build/base-reports
```

Confirm option names and behavior against the exact plugin version in use.
