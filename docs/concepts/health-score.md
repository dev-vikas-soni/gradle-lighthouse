# The Health Score Model

Lighthouse uses a mathematical model to quantify architectural debt. It shifts the focus from simple "violation counting" to **Architectural Risk Assessment**.

## Severity Weights

Every audit finding is assigned a severity that determines its impact:

| Severity | Weight | Rationale |
| :--- | :--- | :--- |
| **FATAL** | 32.0 | Critical failure (e.g., Circular Dependency). |
| **ERROR** | 8.0 | Significant debt or risk. |
| **WARNING**| 2.0 | Minor debt or maintainability concern. |
| **INFO** | 0.2 | Best practice opportunity. |

## Square Root Deduction Model

Lighthouse uses a **Square Root Deduction Model** for category scoring:

$$CategoryScore = 100 - (K \times \sqrt{\sum SeverityWeights})$$

* **Friction Coefficient ($K$)**: Set to **6.6** (tuned against industry benchmarks).
* **Dampening Effect**: The square root curve ensures that the first few issues have the highest impact. This prevents large projects from having negative scores and ensures that every incremental fix matters.

## The Weakest Link Principle

The overall module score is not a simple average. It uses the **Weakest Link Principle**:

$$OverallScore = \frac{WeightedAverage(Categories) + Min(CategoryScore)}{2}$$

This ensures that a module cannot be considered "Healthy" if a major domain (like Security) is failing, even if other domains are perfect.

## Rank Table

Scores are mapped to qualitative ranks to provide immediate context:

| Score Range | Rank |
| :--- | :--- |
| 95 - 100 | **ELITE** (Grandmaster Architect) |
| 85 - 94 | **STRONG** (Expert Architect) |
| 70 - 84 | **MAINTAINED** (Standard Architect) |
| 50 - 69 | **AT RISK** |
| 0 - 49 | **LEGACY** |

## Path to 90: Health ROI

Lighthouse calculates the **Potential Score Gain** for every finding. This allows developers to prioritize engineering work based on "Health ROI"—getting the largest quality improvement for the least amount of effort.
