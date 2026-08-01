# DSL Reference

The `lighthouse { }` block allows you to customize auditing behavior and enforcement gates.

## Core Properties

### `targetVariant`
* **Type**: `Property<String>`
* **Default**: `""`
* **Description**: The Android build variant to target. If empty, all standard configurations are scanned.

### `failOnSeverity`
* **Type**: `Property<String>`
* **Default**: `"NONE"`
* **Valid Values**: `NONE`, `INFO`, `WARNING`, `ERROR`, `FATAL`
* **Description**: Threshold to fail the `lighthouseAudit` task.

### `useAi`
* **Type**: `Property<Boolean>`
* **Default**: `false`
* **Description**: Enable complex **GenAI Remediation** refactorings in `lighthouseFix`.
* **Recommendation**: Keep as `false` in CI; use locally on a clean Git branch for refactors.

## Aggregate Gates (Root Project Only)

### `failOnDependencyCycle`
* **Type**: `Property<Boolean>`
* **Default**: `false`
* **Description**: Fail `lighthouseAggregate` if circular dependencies exist.

### `failOnLayerViolation`
* **Type**: `Property<Boolean>`
* **Default**: `false`
* **Description**: Fail `lighthouseAggregate` if `lighthouse-rules.yaml` layering rules are broken.

### `minHealthScore`
* **Type**: `Property<Int>`
* **Default**: `0`
* **Description**: Fail `lighthouseAggregate` if the average project score is below this number.

## Auditor Toggles

| Property | Default | Description |
| :--- | :--- | :--- |
| `enableDependencyHealth` | `true` | Unused deps, dynamic versions, JCenter. |
| `enablePlayPolicy` | `true` | AndroidManifest Play Store compliance. |
| `enableCatalogMigration` | `true` | Migration to Version Catalogs. |
| `enableBuildSpeed` | `true` | KAPT/KSP, caching, parallel flags. |
| `enableAppSize` | `true` | Asset bloat, shrinking rules. |
| `enableStabilityCheck` | `true` | R8 reflection hazards. |
| `enableKmpCheck` | `true` | Kotlin Multiplatform structure. |
| `enableModuleGraphCheck` | `true` | Cycle and coupling analysis. |
| `enableTestCoverageCheck` | `true` | Missing tests (Dark modules). |
| `enablePredictiveIntelligence`| `true` | Proactive SDK cost estimation. |

## CI/CD Integration

### `enableTelemetry`
* **Type**: `Property<Boolean>`
* **Default**: `false`
* **Description**: Opt-in to sharing anonymized SDK performance data.

### `baseReportDir`
* **Type**: `DirectoryProperty`
* **Default**: `(unset)`
* **Description**: Path to previous reports for PR Bot delta calculation.

### `enableSarifReport`
* **Type**: `Property<Boolean>`
* **Default**: `true`
* **Description**: Generate SARIF v2.1.0 for security tools.
