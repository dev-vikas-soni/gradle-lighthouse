# CI/CD Integration

Gradle Lighthouse is designed to be the "Architectural Quality Gate" in your CI/CD pipeline.

## Standard Pipeline Sequence

A typical Lighthouse CI run should consist of three tasks:

1. **`lighthouseAudit`**: Scans all individual modules.
2. **`lighthouseAggregate`**: Performs graph-wide enforcement and generates the PR summary.
3. **Artifact Upload**: Uploads the HTML reports and SARIF files for persistence.

```bash
./gradlew lighthouseAudit lighthouseAggregate --no-daemon --stacktrace
```

## Reporting Formats

### 1. HTML Dashboard
Visual dashboard for human review. Upload this as a CI artifact.
`**/build/reports/lighthouse/index.html`

### 2. SARIF (Static Analysis Results Interchange Format)
The industry standard for security and quality reporting. Supported by:
* GitHub Security / Code Scanning
* GitLab SAST
* Azure DevOps

### 3. JUnit XML
Standard test report format. Allows you to see Lighthouse findings in your CI "Tests" tab.
`**/build/reports/lighthouse/module-report.xml`

## Enforcement Gates

We recommend starting with `failOnSeverity.set("FATAL")`. This will only block your pipeline for critical issues like instant-crashes on Android 12+ or circular dependencies.

Once your codebase is clean, move to `failOnSeverity.set("ERROR")` to prevent new technical debt.

## Integration Examples

* [GitHub Actions Guide](github-actions.md)
* [PR Bot & Delta Analysis](pr-bot.md)
