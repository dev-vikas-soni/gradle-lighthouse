# Quick Start

Get up and running with your first architectural audit in under 60 seconds.

## 1. Run your first audit

Execute the `lighthouseAudit` task on any module:

 ```bash
 ./gradlew :app:lighthouseAudit
 ```

## 2. Aggregating results

In multi-module projects, run the `lighthouseAggregate` task to generate a global dashboard and dependency graph:

 ```bash
 ./gradlew lighthouseAudit lighthouseAggregate
 ```

## 3. View the reports

After the tasks finish, you can find the results in your `build/reports` directory.

### Per-Module Dashboard
High-level overview of a single module's health.
`path/to/module/build/reports/lighthouse/index.html`

### Project Global Dashboard
Aggregated view of all modules, including the **Galaxy Graph**.
`build/reports/lighthouse/project-dashboard.html`

### CI/CD Reports
SARIF and JUnit XML formats are also generated for integration with tools like GitHub Security and Jenkins.
`build/reports/lighthouse/module-report.sarif`
`build/reports/lighthouse/module-report.xml`

## 4. Automatic Fixes

Found issues? Lighthouse can fix deterministic problems (like `gradle.properties` optimizations) automatically:

 ```bash
 ./gradlew lighthouseFix
 ```

For complex refactorings like KSP migration, use the **GenAI Remediation** mode:

 ```bash
 ./gradlew lighthouseFix --ai=true
 ```
