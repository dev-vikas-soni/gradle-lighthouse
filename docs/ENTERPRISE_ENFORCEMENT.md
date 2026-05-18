# Enterprise CI/CD Enforcement

> Gradle Lighthouse v2.2.2+ | Task: `lighthouseAggregate`

This guide covers the aggregate enforcement gates, custom YAML rules, and CI pipeline integration for teams using Gradle Lighthouse as an architectural guardrail.

---

## Table of Contents

1. [Aggregate gates](#1-aggregate-gates)
2. [DSL configuration](#2-dsl-configuration)
3. [Scope and execution model](#3-scope-and-execution-model)
4. [Custom YAML rules](#4-custom-yaml-rules)
5. [Failure output](#5-failure-output)
6. [Pipeline integration](#6-pipeline-integration)
7. [Rollout advice](#7-rollout-advice)

---

## 1. Aggregate gates

Gradle Lighthouse exposes three aggregate gates. They all run during `lighthouseAggregate` and all default to off.

| Gate | Purpose | Recommended for |
|------|---------|-----------------|
| `failOnDependencyCycle` | fail the build when the global module graph contains cycles | any multi-module codebase |
| `failOnLayerViolation` | fail the build when a lower layer depends on a higher layer | repos with established architectural layering |
| `minHealthScore` | fail when the aggregate average health score drops below a configured baseline | teams ratcheting quality over time |

### Enforced layer order

The built-in layer gate assumes the following order:

```text
Root → App → Feature → Domain → Data → Core → Shared
```

A lower-level layer must not depend on a higher-level one. Example:

```text
:core:network -> :feature:login
```

is treated as a violation.

---

## 2. DSL configuration

Configure the gates in the root project's `build.gradle.kts`:

```kotlin
lighthouse {
    failOnDependencyCycle.set(true)
    failOnLayerViolation.set(true)
    minHealthScore.set(85)
}
```

You can adopt them gradually. A practical rollout is:
1. enable reporting first
2. turn on warnings and dashboards
3. set `minHealthScore` conservatively
4. enable cycle and layer gates once the team is ready to keep them on

---

## 3. Scope and execution model

These gates only execute during `lighthouseAggregate` because that task is the only one that has the complete project-wide module graph.

| Scenario | `failOnDependencyCycle` | `failOnLayerViolation` | `minHealthScore` |
|----------|-------------------------|------------------------|------------------|
| `./gradlew lighthouseAudit` | ❌ | ❌ | ❌ |
| `./gradlew lighthouseAggregate` in a multi-module repo | ✅ | ✅ | ✅ |
| single-module project | typically not meaningful | typically not meaningful | not very useful |

For module-local gating, use:

```kotlin
lighthouse {
    failOnSeverity.set("ERROR")
}
```

That gate is evaluated by `lighthouseAudit`, not by the aggregate task.

---

## 4. Custom YAML rules

Create a `lighthouse-rules.yaml` file at the repository root.

### Minimal example

```yaml
rules:
  - name: "Feature Isolation"
    condition: ":feature:* !-> :feature:*"
    level: "error"

  - name: "Core Purity"
    condition: ":core:* !-> :feature:*"
    level: "fatal"

  - name: "Standard Layering"
    condition: "App -> Feature -> Domain -> Data -> Core"
    level: "error"
```

### Supported rule constructs

| Construct | Example | Meaning |
|-----------|---------|---------|
| Layering rule | `App -> Feature -> Domain -> Data -> Core` | defines permitted dependency direction across layers |
| Isolation rule | `:feature:* !-> :feature:*` | forbids edges matching both sides |
| Wildcard | `*` | wildcard support in path matching |

### Supported levels

| Level | Behavior |
|-------|----------|
| `warning` | logged to console, does not fail the build |
| `error` | added to aggregate failure output and fails the build |
| `fatal` | added to aggregate failure output and fails the build |

Use `warning`, `error`, or `fatal` only. Other values are treated like build-failing errors by the current implementation.

### Important implementation notes

- Rules are evaluated during `lighthouseAggregate`
- `warning` rules do not fail the build
- `error` and `fatal` rules fail the build
- layering rules only govern the layers explicitly named in the condition
- isolation rules support patterns like `:feature:*`, `:core:*`, and `*`

---

## 5. Failure output

When one or more gates fail, the build throws a `GradleException` with structured output similar to:

```text
════════════════════════════════════════════════════════════
❌ GRADLE LIGHTHOUSE STRICT ENFORCEMENT FAILURE
════════════════════════════════════════════════════════════
   🚨 Global Health Score Gating Failed: Architectural Health Score is 72%, but required minimum is 85%.
   🚨 Modular Cycle Gating Failed: 1 circular dependency loops detected:
      🔗 :feature:cart ➔ :feature:checkout ➔ :feature:cart
   🚨 Architectural Layer Crossing Gating Failed: 1 illegal leaks detected:
      🚨 Layer Boundary Leak: :core:network (Core) depends on :feature:login (Feature)
════════════════════════════════════════════════════════════
💡 Refactor dependency structure or adjust strict settings in gradle-lighthouse plugin configuration block.
```

Use the aggregate HTML dashboard alongside the failure output when you want to inspect graph structure and experiment with sandbox fixes before changing production code.

---

## 6. Pipeline integration

### GitHub Actions

```yaml
name: Lighthouse Architectural Audit

on:
  pull_request:
    branches: [ main, develop ]

permissions:
  security-events: write
  pull-requests: write

jobs:
  audit:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: '17'

      - name: Run Gradle Lighthouse
        uses: dev-vikas-soni/gradle-lighthouse@v2.2.2
        with:
          fail-on-severity: 'ERROR'
          upload-sarif: 'true'
          comment-on-pr: 'true'

      - name: Upload HTML reports
        if: always()
        uses: actions/upload-artifact@v4
        with:
          name: lighthouse-reports
          path: '**/build/reports/lighthouse/'
```

### GitLab CI

```yaml
lighthouse:
  stage: test
  image: eclipse-temurin:17-jdk
  script:
    - ./gradlew lighthouseAudit lighthouseAggregate
  artifacts:
    when: always
    paths:
      - '**/build/reports/lighthouse/'
```

### Jenkins

```groovy
stage('Gradle Lighthouse') {
    steps {
        sh './gradlew lighthouseAudit lighthouseAggregate --no-daemon --stacktrace'
    }
    post {
        always {
            archiveArtifacts artifacts: '**/build/reports/lighthouse/**', fingerprint: true
            junit testResults: '**/build/reports/lighthouse/*-report.xml', allowEmptyResults: true
        }
    }
}
```

---

## 7. Rollout advice

Recommended enforcement rollout for larger teams:

1. **Observe first**
   - enable reports
   - share the aggregate dashboard
   - identify common violations without failing builds

2. **Set a floor**
   - introduce a moderate `minHealthScore`
   - avoid setting it so high that the team immediately disables it

3. **Protect hard boundaries**
   - turn on cycle detection
   - turn on layer violation checks if your repo naming maps cleanly to layers

4. **Add team-specific rules**
   - encode cross-feature isolation and core purity in `lighthouse-rules.yaml`
   - start new rules as `warning` first, then promote to `error` or `fatal`

The goal is not maximum strictness on day one. The goal is a gate that teams keep enabled.
