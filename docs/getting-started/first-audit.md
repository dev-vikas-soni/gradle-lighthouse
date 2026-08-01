# Your First Audit

Running your first audit with Gradle Lighthouse is the first step toward a healthier, more maintainable project.

## Step 1: Execute the audit

Open your terminal and run:

```bash
./gradlew lighthouseAudit
```

You will see colorful output in your terminal. Lighthouse prints a high-level summary of every audited module directly to the console.

### Console Summary Example

```text
┌──────────────────────────────────────────────────────────┐
│ 🏗️  :app                                                 │
│ Score: 72/100  ·  ⭐ Standard Architect                   │
├──────────────────────────────────────────────────────────┤
│ ✅ Build caching enabled                                  │
│ ✅ Parallel execution enabled                             │
│ ❌ Dynamic Version Detected: com.squareup.okhttp3:okhttp  │
│ ⚠️  KSP ROI Engine: Save 15h/year                         │
├──────────────────────────────────────────────────────────┤
│ 8 issues: 0 fatal · 2 error · 4 warn · 2 info            │
└──────────────────────────────────────────────────────────┘
```

## Step 2: Understand the Score

Lighthouse uses a **Square Root Deduction Model**. Instead of just counting issues, it evaluates the **architectural impact** of every finding.

* **FATAL**: Critical architectural risk (e.g., circular dependency). Huge score impact.
* **ERROR**: High risk or significant technical debt.
* **WARNING**: Maintainability concern or legacy pattern.
* **INFO**: Best practice opportunity.

A module with 100% score is following all Lighthouse best practices.

## Step 3: Explore the Reports

While the console is great for a quick look, the **HTML reports** contain the deep-dive intelligence you need to fix issues.

1. Navigate to your module's build directory: `app/build/reports/lighthouse/index.html`.
2. Open it in your browser.
3. Look for the **"Path to 90"** section. This prioritized list tells you exactly which fixes will result in the largest score gains.

## Step 4: Aggregate and Visualize

If you have a multi-module project, run the aggregate task:

```bash
./gradlew lighthouseAggregate
```

Open `build/reports/lighthouse/project-dashboard.html` to see the **Galaxy Graph**. This is an interactive map of your module dependencies. It automatically detects cycles and highlights coupling "hotspots" that might be slowing down your build.

## Next Steps

* [Understanding the Report](understanding-report.md)
* [Using AI Remediation](../guides/ai-remediation.md)
* [Setting up the PR Bot](../guides/pr-bot.md)
