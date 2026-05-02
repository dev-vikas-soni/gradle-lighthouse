package com.gradlelighthouse.extension

import org.gradle.api.provider.Property

/**
 * The configuration extension for the Gradle Lighthouse plugin.
 *
 * Allows developers to opt-in or out of specific health checks and configure
 * CI/CD integration behavior (report formats, build gate severity).
 *
 * Example usage in `build.gradle.kts`:
 * ```kotlin
 * lighthouse {
 *     enableDependencyHealth.set(true)
 *     enablePlayPolicy.set(true)
 *     enableModernizationCheck.set(true)
 *     enableKmpCheck.set(true)
 *     failOnSeverity.set("FATAL")  // Fail the build on FATAL issues
 * }
 * ```
 */
abstract class LighthouseExtension {

    // ==========================
    // Core Toggles
    // ==========================

    /** 
     * Target a specific variant for auditing (e.g., "release", "productionRelease"). 
     * If set, only dependencies matching this variant's configuration will be audited.
     * Default: "" (audits all standard configurations like implementation, releaseImplementation, etc.)
     */
    abstract val targetVariant: Property<String>

    /** Enables analysis of unused dependencies, leaked APIs, and dynamic versions. Default: true */
    abstract val enableDependencyHealth: Property<Boolean>

    /** Enables checking AndroidManifest.xml compliance against Google Play rules. Default: true */
    abstract val enablePlayPolicy: Property<Boolean>

    /** Scans for legacy hardcoded dependency strings (TOML migration). Default: true */
    abstract val enableCatalogMigration: Property<Boolean>

    /** Enables KAPT/KSP analysis, caching, parallel, and Jetifier checks. Default: true */
    abstract val enableBuildSpeed: Property<Boolean>

    /** Enables checking for R8 minification, resource shrinking, and asset bloat. Default: true */
    abstract val enableAppSize: Property<Boolean>

    /** Enables R8/ProGuard reflection hazard detection and Manifest compliance. Default: true */
    abstract val enableStabilityCheck: Property<Boolean>

    /** Enables transitive dependency version jump detection. Default: true */
    abstract val enableConflictCheck: Property<Boolean>

    /** Enables Compose vs XML ratio, startup TTI killers, and KSP migration ROI. Default: true */
    abstract val enableModernizationCheck: Property<Boolean>

    /** Enables Kotlin Multiplatform project structure checks. Default: true */
    abstract val enableKmpCheck: Property<Boolean>

    // ==========================
    // CI/CD Integration
    // ==========================

    /**
     * Minimum severity level that will fail the Gradle build.
     * Valid values: "NONE", "INFO", "WARNING", "ERROR", "FATAL"
     * Default: "NONE" (never fail)
     *
     * Set to "FATAL" to gate CI builds on critical issues only.
     * Set to "ERROR" for stricter enforcement.
     */
    abstract val failOnSeverity: Property<String>

    /**
     * Enable SARIF report output for GitHub Security / GitLab SAST integration.
     * Default: true
     */
    abstract val enableSarifReport: Property<Boolean>

    /**
     * Enable JUnit XML report output for Jenkins / Bitrise / CircleCI integration.
     * Default: true
     */
    abstract val enableJunitXmlReport: Property<Boolean>

    init {
        // Safe conventions (defaults) for all properties
        targetVariant.convention("")
        enableDependencyHealth.convention(true)
        enablePlayPolicy.convention(true)
        enableCatalogMigration.convention(true)
        enableBuildSpeed.convention(true)
        enableAppSize.convention(true)
        enableStabilityCheck.convention(true)
        enableConflictCheck.convention(true)
        enableModernizationCheck.convention(true)
        enableKmpCheck.convention(true)
        failOnSeverity.convention("NONE")
        enableSarifReport.convention(true)
        enableJunitXmlReport.convention(true)
    }
}
