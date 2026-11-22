# Repository tour

This guide points contributors to the main implementation areas. Code and tests are the source of truth when this overview differs.

## Root files

- `build.gradle.kts`: plugin build, dependencies, publication configuration.
- `action.yml`: GitHub Action metadata.
- `lighthouse-rules.yaml`: sample custom rules.
- `docs/`: user and developer documentation.

## Kotlin sources

Located under `src/main/kotlin/com/gradlelighthouse`:

- `core/`: audit data models, auditor contract, scoring, and logging.
- `auditors/`: individual diagnostic checks.
- `extension/`: user-configurable plugin properties.
- `task/`: Gradle task orchestration, aggregation, fixes, and benchmark tasks.
- `reporting/`: HTML, JSON, SARIF, and JUnit output.

## Tests

`src/test/kotlin` contains unit and Gradle TestKit tests. Read the relevant test before changing behavior, then add focused positive, negative, and regression cases.

## Example project

The `example/` directory is a small Gradle project used for manual smoke testing. Run:

```bash
./gradlew :example:lighthouseAudit
```

## A good first contribution

Choose a focused issue, describe the exact behavior and evidence, add a regression test, update the relevant user docs, and run the checks listed in [CONTRIBUTING.md](../../CONTRIBUTING.md).