# Understanding the Report

Lighthouse reports are designed to be actionable, providing not just what is wrong, but why it matters and how to fix it.

## The Scorecard

At the top of every report is the **Architectural Scorecard**.

* **Score**: 0-100 representing overall structural health.
* **Rank**: Qualitative grade (Legacy, At Risk, Maintained, Strong, Elite).
* **Category Breakdown**: Scores for Security, Performance, App Size, etc. This helps you identify if your technical debt is concentrated in a specific domain.

## Architecture Health Breakdown

This radar or bar chart shows your strengths and weaknesses.

* **Security**: Hardcoded secrets, dangerous permissions, signing safety.
* **Build Performance**: KAPT usage, caching settings, parallel execution.
* **Modernization**: Compose adoption, legacy plugin usage.
* **Dependency Hygiene**: Dynamic versions, unused dependencies, JCenter usage.

## Path to 90

This is the most important section for developers. It is a prioritized list of improvements ranked by their **Score ROI**.

Instead of fixing issues alphabetically, follow this list to get the maximum health benefit for the minimum engineering effort.

## Finding Details

Every audit finding includes:

* **Reasoning**: Technical explanation of the issue.
* **Impact Analysis**: What happens in production or CI if this isn't fixed.
* **Resolution**: Step-by-step instructions (often with code snippets).
* **ROI after fix**: The quantified benefit (e.g., "Save 10s per build").

## Interactive Galaxy Graph (Aggregate Report)

In the project global dashboard, the Galaxy Graph provides:

* **Node Size**: Represents module size/complexity.
* **Link Thickness**: Represents coupling density.
* **Red Glow**: Indicates a circular dependency.
* **Layers**: Modules are grouped into orbital rings based on their architectural layer (App, Feature, Domain, Data, Core).

### Sandbox Mode
You can click any dependency in the Galaxy Graph and "cut" it to simulate a refactor. Lighthouse will instantly update the cycle count and health scores, allowing you to prove the value of a refactor before touching a single line of code.
