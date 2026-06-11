# Scoring Model

The Lighthouse Scoring Model (V2) is designed to provide a fair, explainable, and actionable measure of architectural health. It shifts focus from simple "violation counting" to architectural risk assessment.

---

## Severity Weights

Every audit finding is assigned a severity that determines its impact on the score:

| Severity | Weight | Rationale |
|----------|--------|-----------|
| **FATAL** | 32.0 | Critical failure (e.g., Circular Dependency). 1 Fatal = 4 Errors. |
| **ERROR** | 8.0 | Significant debt or risk. |
| **WARNING**| 2.0 | Minor debt or maintainability concern. |
| **INFO**    | 0.2 | Best practice opportunity. |

---

## Category Scoring

Findings are grouped into domains (e.g., Security, Build Performance). Each category produces an independent score (0-100) using a **Square Root Deduction Model**:

$$CategoryScore = 100 - (K \times \sqrt{\sum SeverityWeights})$$

*   **Friction Coefficient ($K$)**: Set to **6.6** (tuned against industry benchmarks).
*   **Dampening Effect**: The square root curve ensures that the first few issues have the highest impact, while very large projects do not collapse toward zero purely due to scale.

### Hard-Ceiling Floor
To ensure architectural integrity, a **Hard-Ceiling** rule is applied: If a category contains **3 or more FATAL issues**, that category score is automatically floored to **0**.

---

## Overall Health Score

The overall score is not a simple average. It uses the **Weakest Link Principle**:

$$OverallScore = \frac{WeightedAverage(Categories) + Min(CategoryScore)}{2}$$

This ensures that a project cannot be considered "Healthy" if a major domain (like Security) is failing, even if other domains are perfect.

---

## Health Grades

Scores are mapped to qualitative grades to provide immediate context:

| Score Range | Grade |
|-------------|-------|
| 95 - 100 | **ELITE** |
| 80 - 94 | **STRONG** |
| 60 - 79 | **MAINTAINED** |
| 40 - 59 | **AT RISK** |
| 0 - 39 | **LEGACY** |

---

## Path To 90

Lighthouse generates an automated improvement roadmap. For every deduction, the engine estimates the **Potential Score Gain** if the issue is resolved. This allows developers to prioritize engineering work based on "Health ROI."

---

## Why this model exists?

Transparency and trust are the primary goals. By exposing the math behind every deduction and providing a category-level breakdown, Lighthouse ensures that the score is never a "black box," but a tool for driving structural improvements.
