# Quick start

Run the audit task from the Gradle project:

```bash
./gradlew lighthouseAudit
```

For a multi-module build, the aggregate task may be available when the plugin is applied/configured at the root:

```bash
./gradlew lighthouseAudit lighthouseAggregate
```

## Reports

The default report directory is under the relevant project's `build/reports/lighthouse`. Exact report names and availability depend on the task and plugin configuration. Common outputs include HTML and machine-readable reports such as JSON, SARIF, and JUnit XML.

## Fix task

The plugin also provides a fix task in supported versions:

```bash
./gradlew lighthouseFix
```

This task applies limited deterministic transformations. It is not an AI/LLM feature and may modify build files. Use a clean working tree, inspect the complete diff, and run your normal build and tests before keeping any changes. See [deterministic remediation](../guides/ai-remediation.md).

## Interpret findings carefully

Lighthouse findings are diagnostic signals, not proof of a defect. Check the relevant source/build configuration and the [auditor catalog](../reference/auditor-catalog.md) before changing your project.
