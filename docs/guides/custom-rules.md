# Custom Architecture Rules

Lighthouse allows you to encode your team's architectural conventions into a `lighthouse-rules.yaml` file. These rules are enforced during the aggregation phase.

## Configuration

Create a file named `lighthouse-rules.yaml` in your root project directory.

## Rule Types

### 1. Isolation Rules (`A !-> B`)
Prevents a group of modules from depending on another group.

```yaml
rules:
  - name: "Feature Isolation"
    condition: ":feature:* !-> :feature:*"
    level: "error"
    description: "Features must use :domain or :core for shared logic."
```

* **Same-group exemption**: `:feature:search:ui` can still depend on `:feature:search:api`. Sibling modules in the same nested group are exempt by default.

### 2. Layering Rules (`A -> B -> C`)
Enforces a strict top-down dependency flow across multiple layers.

```yaml
rules:
  - name: "Strict Layering"
    condition: "App -> Feature -> Domain -> Data -> Core"
    level: "fatal"
    description: "Ensures Core cannot depend on App or Feature."
```

* **Partial Coverage**: If you have a module in a layer NOT mentioned in the rule, it is silently ignored for that rule.

## Pattern Syntax

* `*`: Matches every module (universal wildcard).
* `:prefix:*`: Matches any module whose path starts with the prefix.
* `:exact:path`: Exact module path match.

## Severity Levels

* `fatal`: Fails the build with a 💀 icon.
* `error`: Fails the build with a 🚨 icon (default).
* `warning`: Logs to the console with a ⚠️ icon but does NOT fail the build.

## Real-world Example

```yaml
rules:
  - name: "Restricted Design System"
    condition: "* !-> :core:design-system:internal"
    level: "fatal"
    description: "Only use the public API of the design system."

  - name: "No Direct Data Access"
    condition: ":feature:*:ui !-> :feature:*:data"
    level: "warning"
    description: "Advisory: Use Domain layer repositories in UI modules."
```
