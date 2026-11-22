# Migration guide

## Upgrading to v2.3.2

Review the release notes and run the audit on a clean working tree. Compare reports before enabling CI enforcement.

### From v2.3.1

The v2.3.2 release notes describe aggregation and task-configuration maintenance. Verify behavior against your own build before relying on it in CI.

### From v2.2.x

Earlier documentation described `lighthouseFix --ai` as GenAI remediation. That description was inaccurate: the fix implementation is deterministic/pattern-based and does not represent an LLM integration. Do not assume it can safely perform arbitrary multi-file refactoring.

Earlier documentation also described SDK intelligence and telemetry in terms that could imply community-measured predictions. Do not rely on SDK impact values as empirical measurements without a published dataset and methodology. Review the actual telemetry setting and implementation for the release you run; do not infer data collection behavior from this migration guide.

### From v2.0.x

The v2.0 release changed the scoring model and some extension property names. Check the [DSL reference](reference/dsl-reference.md) for the property names available in your installed release. Scores from different plugin versions or configurations should not be compared as if they were calibrated on the same scale.

### From v1.x

- Plugin ID changed from `com.gradlelighthouse.plugin` to `io.github.dev-vikas-soni.lighthouse`.
- Task naming changed from `depAudit` to `lighthouseAudit`.
- The extension block is `lighthouse { }`.

Always consult the release-specific changelog and verify the resulting build.
