# Migration Guide

## Upgrading to v2.3.2

### From v2.3.1
* **Maintenance**: Internal reliability and documentation hardening.

### From v2.2.x
* **GenAI Remediation**: The `lighthouseFix` task now supports an `--ai` flag. No DSL changes required, but we recommend trying it on a clean branch.
* **Predictive Intelligence**: New `enablePredictiveIntelligence` property is `true` by default. If you use a custom firewall, you may need to whitelist `telemetry.gradle-lighthouse.dev`.

### From v2.0.x
* **DSL Refactor**: Many boolean toggles were renamed for consistency (e.g., `enableSecurity` -> `enableSecurityCheck`).
* **Scoring Shift**: v2.0 introduced the **Category Model**. You may see a +/- 5% shift in your health score due to the new weighting math.

### From v1.x (Breaking Changes)
* **Plugin ID**: Changed from `com.gradlelighthouse.plugin` to `io.github.dev-vikas-soni.lighthouse`.
* **Task Names**: `depAudit` is now `lighthouseAudit`.
* **DSL Block**: `lighthouseAudit { }` is now `lighthouse { }`.
