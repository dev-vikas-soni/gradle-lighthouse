# Architectural Enforcement

Lighthouse provides multiple ways to turn architectural insights into hard build-time guardrails.

## Local Enforcement (Per Module)

The most granular way to enforce standards is through the `failOnSeverity` property.

```kotlin
lighthouse {
    // Fail the task if any issue with ERROR severity or higher is found
    failOnSeverity.set("ERROR")
}
```

This gate runs during `lighthouseAudit`. It is ideal for enforcing "Local Hygiene" (e.g., no hardcoded versions, no heavy utility libraries).

## Global Enforcement (The Aggregate Task)

The `lighthouseAggregate` task can enforce constraints on the **entire project graph**. These gates are critical for maintaining large-scale architectural boundaries.

```kotlin
lighthouse {
    // Prevent cycles in the module graph
    failOnDependencyCycle.set(true)

    // Enforce layer order (App -> Feature -> Core)
    failOnLayerViolation.set(true)

    // Prevent overall health score from dropping
    minHealthScore.set(85)
}
```

## Custom YAML Rules

For team-specific constraints, Lighthouse supports a `lighthouse-rules.yaml` engine. This allows you to define custom isolation and layering rules without writing any Gradle script code.

```yaml
rules:
  - name: "Feature Isolation"
    condition: ":feature:* !-> :feature:*"
    level: "error"
    description: "Sibling features must not depend on each other directly."
```

## The PR Bot: Collaborative Enforcement

Strict build failures can sometimes be frustrating for developers. The **Lighthouse PR Bot** provides a more collaborative approach by posting score deltas and cycle warnings as comments in GitHub Pull Requests.

This allows reviewers to make an informed decision: "The score dropped by 2 points because we added a necessary library. I'm okay with merging this."

## Summary of Gates

| Level | Gate | Target | Best For |
| :--- | :--- | :--- | :--- |
| **Local** | `failOnSeverity` | Module | Code hygiene & library usage |
| **Global** | `failOnDependencyCycle` | Graph | Preventing structural rot |
| **Global** | `failOnLayerViolation` | Graph | Enforcing layering architecture |
| **Custom** | `lighthouse-rules.yaml` | Logic | Team-specific modularity rules |
