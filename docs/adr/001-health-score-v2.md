# ADR-001: Health Score Engine V2

## Status

Accepted

## Date

2026-06

---

# Context

Gradle Lighthouse originally used a global exponential decay scoring model.

```text
Score = 100 × decayFactor^(TotalImpact)
```

While simple, the model produced several undesirable outcomes:

* Large repositories collapsed toward zero.
* Mature projects were penalized purely because of scale.
* Scores lacked actionable insights.
* Developers could not identify which area was affecting the score.
* A single overall score provided little architectural context.

Examples:

* Signal Android could receive extremely low scores despite being a mature production application.
* Firefox Android could collapse into the minimum score range.
* Improvements often produced little visible score movement.

The scoring system needed to evolve from a finding counter into an architectural health model.

---

# Decision

Gradle Lighthouse V2 adopts a Category-Based Weighted Health Model.

Instead of calculating a single score from all findings, Lighthouse now:

1. Classifies findings into architectural domains.
2. Calculates an independent score for each category.
3. Aggregates category scores using weighted averages.
4. Applies weakest-link protection.
5. Generates actionable improvement recommendations.

---

# Core Principles

## 1. Architectural Health Over Finding Count

The goal is not to count violations.

The goal is to measure architectural health.

Two projects with the same number of findings may have very different risk profiles.

Example:

* 20 modernization warnings
* 1 security vulnerability

The security issue must have significantly higher impact.

---

## 2. Explainability

Every score deduction must be traceable.

Developers should understand:

* what reduced the score
* why it matters
* how to improve

The score must never feel like a black box.

---

## 3. Fairness For Large Repositories

Large repositories naturally contain more findings.

The scoring model should:

* penalize risk
* avoid score collapse
* preserve meaningful differentiation

The objective is not to reward size, but to avoid punishing scale.

---

# Health Categories

Lighthouse evaluates the following domains:

| Category           | Purpose                                      |
| ------------------ | -------------------------------------------- |
| Architecture       | Dependency boundaries, modularity, coupling  |
| Security           | Security vulnerabilities and unsafe patterns |
| Performance        | Runtime performance risks                    |
| Build Performance  | Build speed and Gradle inefficiencies        |
| Complexity         | Maintainability and code complexity          |
| Quality            | General code quality                         |
| Modernization      | Adoption of modern Android tooling           |
| Dependency Hygiene | Dependency management quality                |
| App Size           | Binary size and resource footprint           |

Each category is scored independently.

---

# Severity Model

| Severity | Weight |
| -------- | ------ |
| FATAL    | 32     |
| ERROR    | 8      |
| WARNING  | 2      |
| INFO     | 0.2    |

Rationale:

* Fatal findings represent critical architectural failures (32 = 4 Errors).
* Errors represent significant technical debt.
* Warnings represent maintainability concerns.
* Info findings represent best-practice opportunities.

---

# Category Score Calculation

Each category produces a score between 0 and 100.

Raw impact is calculated as:

RawImpact = Σ SeverityWeights

To prevent score collapse, Lighthouse applies a dampened deduction curve.

Current implementation:

```text
PointsLost = K × √RawImpact
```

Where:

```text
K = 6.6
```

Category score:

```text
CategoryScore = 100 - PointsLost
```

### Hard-Ceiling Floor
To ensure critical architectural integrity, a "Hard-Ceiling" rule is applied:
If a category contains **3 or more FATAL issues**, the category score is automatically set to **0**, regardless of other findings.

---

# Why Square Root Dampening?

Several scoring models were evaluated:

* Linear
* Exponential
* Logistic
* Rational
* Square Root

The square root model was selected because it provides:

### High sensitivity near perfection

Small regressions matter when pursuing Elite scores.

### Reduced collapse at scale

Large repositories retain score differentiation.

### Better face validity

Expected outcomes remain believable:

| Error Count (at K=6.6) | Approx Score |
| ---------------------- | ------------ |
| 1                      | 81           |
| 5                      | 58           |
| 10                     | 41           |

*(Note: Values updated for tuned friction coefficient K=6.6)*

---

# Overall Score Aggregation

Category scores are combined using weighted aggregation.

Example weights:

| Category           | Weight |
| ------------------ | ------ |
| Architecture       | 20.0   |
| Security           | 20.0   |
| Performance        | 15.0   |
| Build Performance  | 15.0   |
| Complexity         | 10.0   |
| Quality            | 5.0    |
| Modernization      | 10.0   |
| Dependency Hygiene | 3.0    |
| App Size           | 2.0    |

---

# Weakest Link Principle

Architectural health is limited by the weakest critical domain.

A project cannot be considered healthy if a major category is failing.

Rule:

```text
OverallScore = (WeightedAverage + MinCategoryScore) / 2
```

This ensures that:
1. The project must maintain high standards across all domains to achieve a top-tier score.
2. A severe failure in one area (e.g., Security) significantly impacts the project's overall health score, even if other areas are perfect.

---

# Path To 90

Lighthouse provides an improvement roadmap.

For each deduction:

1. Determine category impact.
2. Estimate score recovery.
3. Rank improvements by expected gain.

Output example:

```text
Path To 90

1. Resolve circular dependencies (+4.8)
2. Migrate kapt processors to KSP (+3.1)
3. Remove unused dependencies (+2.4)
```

The goal is to transform findings into actionable engineering work.

---

# Calibration Targets

The scoring model is calibrated against well-known Android projects.

Expected ranges:

| Project         | Expected Score |
| --------------- | -------------- |
| Now in Android  | 95-100         |
| Signal Android  | 80-88          |
| Firefox Android | 40-55          |

These projects act as reference personas for score validation.

---

# Confidence Model (Future)

A score is only meaningful if sufficient auditors are active.

Planned:

```text
Confidence =
ActiveAuditors /
AvailableAuditors
```

Example:

```text
Score: 82
Confidence: 94%
```

This communicates trust in the result.

---

# Non-Goals

The health score is NOT intended to:

* Measure developer skill.
* Replace code review.
* Replace security audits.
* Predict production incidents with certainty.

The score is an architectural guidance metric.

---

# Outcome

Health Score Engine V2 transforms Lighthouse from:

"Finding Counter"

into:

"Architecture Intelligence Platform"

The score now reflects architectural risk, maintainability, modernization, and engineering health while remaining explainable, actionable, and scalable across repositories of different sizes.
