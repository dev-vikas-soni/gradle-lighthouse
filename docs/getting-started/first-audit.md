# Your first audit

## 1. Run the audit

From the root of your Gradle build:

```bash
./gradlew lighthouseAudit
```

For a multi-module build, you may also run the aggregate task if it is configured:

```bash
./gradlew lighthouseAggregate
```

## 2. Review findings

The console output summarizes findings detected by enabled auditors. Open the generated report under the relevant project's `build/reports/lighthouse` directory.

For each finding, ask:
- Does the check inspect the relevant part of this build?
- Is the finding accurate for this project's conventions?
- Is the suggested resolution valid for the versions and plugins in use?
- Is the impact a measured value or a heuristic estimate?

Severity and score are prioritization aids, not proof of production risk or project quality. A score of 100 means only that the enabled checks did not deduct points under the current scoring model; it does not certify the project.

## 3. Explore aggregate output

If the aggregate task is available, inspect the generated project report and module dependency visualization. Treat it as a view of the relationships captured by the plugin, not a complete representation of every dependency or runtime relationship.

## 4. Validate before acting

Do not automatically apply a recommendation just because it appears in a report. Review build files, run the normal build and tests, and measure performance changes in your own environment.

## Next steps

- [Understanding reports](understanding-report.md)
- [Auditor catalog and limitations](../reference/auditor-catalog.md)
- [Deterministic remediation](../guides/ai-remediation.md)
- [Roadmap](../../ROADMAP.md)
