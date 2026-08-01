# GitHub Actions Guide

Lighthouse provides a first-class composite action for easy integration into GitHub workflows.

## Minimal Workflow

Add the following step to your `.github/workflows/ci.yml`:

```yaml
jobs:
  audit:
    runs-on: ubuntu-latest
    permissions:
      security-events: write # Required for SARIF upload
      pull-requests: write   # Required for PR Bot
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'temurin'

      - name: Run Gradle Lighthouse
        uses: dev-vikas-soni/gradle-lighthouse@v2.3.2
        with:
          fail-on-severity: 'FATAL'
          upload-sarif: 'true'
          comment-on-pr: 'true'
```

## Input Parameters

| Input | Description | Default |
| :--- | :--- | :--- |
| `gradle-args` | Additional arguments passed to Gradle. | `""` |
| `fail-on-severity` | Minimum severity to fail the build. | `"NONE"` |
| `upload-sarif` | Upload results to GitHub Security tab. | `"true"` |
| `comment-on-pr` | Post the health summary to the PR. | `"true"` |
| `base-report-path` | Path to store/download the main reports. | `".lighthouse/base-reports"` |

## Advanced: Delta Analysis Setup

To enable score deltas in your PR comments, you must upload the Lighthouse reports on your main branch.

### 1. Main Branch Workflow (Upload)
```yaml
# main.yml
steps:
  - uses: dev-vikas-soni/gradle-lighthouse@v2.3.2

  - name: Upload Main Reports
    uses: actions/upload-artifact@v4
    with:
      name: lighthouse-reports
      path: '**/build/reports/lighthouse/module-report.json'
```

### 2. Pull Request Workflow (Compare)
The Lighthouse action will automatically try to find and download the `lighthouse-reports` artifact from the latest successful run on your base branch (e.g., `main`) to calculate the delta.

## Security Note

Uploading SARIF reports requires the `security-events: write` permission. Posting PR comments requires the `pull-requests: write` permission.
