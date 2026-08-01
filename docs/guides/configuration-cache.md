# Configuration Cache Guide

The **Configuration Cache** is the most significant build speed improvement in recent Gradle versions. It serializes the task graph after the first run and reuses it for subsequent builds.

## 100% Compatibility
Gradle Lighthouse is built from day one to be 100% compatible with the Configuration Cache.

### How it works
All task inputs in Lighthouse are **CC-Safe**:
* Primitives (`String`, `Boolean`, `Int`)
* Serializable DTOs (`AuditContext`)
* Gradle File objects (`DirectoryProperty`, `RegularFileProperty`)

We strictly avoid capturing `org.gradle.api.Project`, `Configuration`, or `Task` references inside our task actions.

## Troubleshooting Cache Misses
If you see "Configuration cache state could not be cached" while using Lighthouse, the cause is usually **external** to our plugin.

### Common causes:
1. **Capturing Project in a Closure**: If you have a custom task that uses `project` inside its action.
2. **Third-party Plugins**: Ensure all other applied plugins are also CC-ready.

## Performance Comparison
In a 50-module project:

| Run | Time | Configuration Status |
| :--- | :--- | :--- |
| **First Run** | 15s | Configuring projects... |
| **Second Run** | 0.8s | Reusing configuration cache. |

Lighthouse ensures that your architectural guardrails never slow down your daily developer loop.
