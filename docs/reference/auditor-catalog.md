# Auditor Catalog

A complete reference of every architectural check performed by Gradle Lighthouse.

## Dependency Hygiene

### DependencyAuditor
* **Purpose**: Enforce build determinism and modern dependency standards.
* **Checks**:
  * **Dynamic Versions**: Flags `+` or `latest.release` versions (Severity: ERROR).
  * **Sunset Repositories**: Flags `jcenter()` usage (Severity: FATAL).
  * **Legacy Libraries**: Flags Guava/Commons usage in Kotlin projects (Severity: WARNING).
* **Remediation**: Use strict versions and migrate to Version Catalogs.

### UnusedDependencyAuditor
* **Purpose**: Remove binary bloat and speed up dependency resolution.
* **Logic**: Compares declared dependencies against actual `import` statements in source files.
* **Note**: Only flags direct dependencies.

### ConflictIntelligenceAuditor
* **Purpose**: Prevent silent version regressions caused by Gradle resolution.
* **Detection**: Flags when Gradle's conflict resolution makes a "major version jump" (e.g. 1.0.0 to 2.0.0).

---

## Build Performance

### BuildSpeedAuditor
* **Purpose**: Optimize the developer loop.
* **Checks**:
  * `org.gradle.caching` (Severity: FATAL if false).
  * `org.gradle.parallel` (Severity: WARNING if false).
  * `android.enableJetifier` (Severity: ERROR if true in modern projects).
  * KSP vs KAPT ROI calculation.

### ConfigCacheReadinessAuditor
* **Purpose**: Ensure compatibility with Gradle Configuration Cache.
* **Detection**: Flags usage of `allprojects`, `subprojects`, and eager task registration.

---

## Security & Compliance

### SecurityAuditor
* **Purpose**: Hardening the build chain.
* **Checks**: Hardcoded secrets, debug-signed release builds, wrapper checksums.

### PlayPolicyAuditor
* **Purpose**: Ensure Play Store compliance before the build reaches CI.
* **Checks**: Target SDK version, dangerous permissions (SMS/Call Log), and exported component flags.

---

## Architecture & Complexity

### ModuleGraphAuditor
* **Purpose**: Maintain structural integrity.
* **Detection**: Circular dependencies (Cycles), feature-to-feature coupling density.

### ModuleSizeAuditor
* **Purpose**: Detect "God Modules" that slow down incremental builds.
* **Metrics**: Lines of code (LOC), number of classes, public API surface area.

### PredictiveIntelligenceAuditor
* **Purpose**: Foresee SDK impact.
* **Logic**: Estimates binary size and startup time based on global telemetry for popular SDKs like Facebook or Google Ads.

---

## Modernization

### ModernizationAuditor
* **Purpose**: Track adoption of modern Android standards.
* **Detection**: Jetpack Compose vs. XML Layout ratio. Flags "XML Monoliths" where Compose adoption is <10%.
