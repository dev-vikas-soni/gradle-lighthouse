# Contributing

Contributions are welcome, especially bug reports with minimal reproductions, false-positive examples, tests, and documentation corrections.

## Development setup

```bash
git clone https://github.com/dev-vikas-soni/gradle-lighthouse.git
cd gradle-lighthouse
./gradlew test
./gradlew build
```

Check the repository's Gradle tasks if a command is unavailable in your checkout.

## Before changing an auditor

1. Read the auditor implementation and its inputs in `AuditContext`.
2. Document exactly which project metadata or files the check inspects.
3. Avoid claims about runtime behavior, build-time savings, test coverage, security guarantees, or compatibility unless the implementation measures or verifies them.
4. Add positive and negative tests, including a regression fixture for the reported problem.
5. Consider multi-module, convention-plugin, generated-source, and Kotlin Multiplatform layouts where relevant.
6. Update the auditor catalog with limitations and examples.

## Auditor design

Auditors should use the data explicitly supplied through the audit context and should not make hidden network requests. New data requirements should be added as explicit task inputs with documented provenance.

Avoid hard-coded performance ROI values. If a finding includes an estimate, explain its basis and label it as an estimate. Prefer actionable technical reasoning over claims such as "saves 15 seconds" without measurement.

## Pull request checklist

- [ ] The change has focused tests.
- [ ] Positive and negative cases are covered.
- [ ] Findings are reproducible and explain the evidence inspected.
- [ ] Documentation describes limitations and does not overstate behavior.
- [ ] Report formats remain consistent.
- [ ] Configuration Cache / Isolated Projects behavior is tested if the change affects task inputs or execution.
- [ ] No secrets, private project data, or unnecessary telemetry are introduced.
- [ ] Build scripts and generated changes are reviewed.

## Reporting issues

Include the plugin version, Gradle wrapper version, JDK, OS, AGP/Kotlin versions where applicable, steps to reproduce, expected behavior, actual behavior, and relevant logs.

## Code conventions

- Follow the [Kotlin coding conventions](https://kotlinlang.org/docs/coding-conventions.html).
- Keep auditor logic small and deterministic.
- Prefer explicit inputs and stable, testable outputs.
- Avoid mutable shared state.
