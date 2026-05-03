package com.gradlelighthouse.auditors

import com.gradlelighthouse.core.AuditContext
import com.gradlelighthouse.core.Auditor
import com.gradlelighthouse.core.AuditIssue
import com.gradlelighthouse.core.ConsoleLogger
import com.gradlelighthouse.core.Severity

/**
 * BuildSpeedAuditor: Identifies compilation bottlenecks that waste developer time.
 *
 * Checks for:
 * - KAPT usage when KSP alternatives exist (with ROI calculation)
 * - Disabled Gradle build caching
 * - Disabled parallel execution
 * - Enabled Jetifier (legacy AndroidX bridge)
 */
class BuildSpeedAuditor : Auditor {
    override val name: String = "BuildSpeed"

    private val kspSupportedLibs = listOf("room", "hilt", "dagger", "moshi", "glide", "epoxy")

    override fun audit(context: AuditContext): List<AuditIssue> {
        val issues = mutableListOf<AuditIssue>()
        ConsoleLogger.auditorStart(name, "⚡", "[PERF]", "Analyzing compilation bottlenecks...")

        // 1. KAPT to KSP migration opportunity
        val hasKaptPlugin = context.pluginIds.any { it.contains("kapt", ignoreCase = true) }
        val kaptDeps = context.dependencies
            .filter { it.configurationName.contains("kapt", ignoreCase = true) }
            .distinctBy { it.coordinate }

        if (hasKaptPlugin || kaptDeps.isNotEmpty()) {
            val migratableCount = kaptDeps.count { dep ->
                kspSupportedLibs.any { dep.name.lowercase(java.util.Locale.ROOT).contains(it) }
            }

            val timeSavedPerBuild = migratableCount * 15 // ~15s per clean build cycle
            val yearlyHoursSaved = (timeSavedPerBuild * 10 * 250) / 3600 // 10 builds/day, 250 days/year

            issues.add(AuditIssue(
                category = "Performance",
                severity = Severity.ERROR,
                title = "KSP ROI Engine: Save ${yearlyHoursSaved}h/year",
                reasoning = "Found $migratableCount libraries using legacy KAPT that support modern KSP processing. Detected KAPT dependencies: ${kaptDeps.joinToString { it.notation }}.",
                impactAnalysis = "KAPT adds substantial overhead (approx. +10s to +40s) to every build cycle due to the requirement for generating Java Stubs. This stalls the CPU and invalidates build caches more frequently than KSP.",
                resolution = "Migrate annotation processors like Room and Dagger/Hilt to KSP (Kotlin Symbol Processing). Add the 'com.google.devtools.ksp' plugin and replace 'kapt' with 'ksp' in the dependencies block.",
                roiAfterFix = "Estimated build speed boost of ~${timeSavedPerBuild}s per developer cycle. Combined with caching, this saves approximately ${yearlyHoursSaved} hours of developer waiting time per year."
            ))
        }

        // 2. Gradle properties checks
        val props = context.gradleProperties

        if (props["org.gradle.caching"] != "true") {
            issues.add(AuditIssue(
                category = "Performance",
                severity = Severity.FATAL,
                title = "Build Caching is Disabled",
                reasoning = "The property 'org.gradle.caching' is not set to true in gradle.properties.",
                impactAnalysis = "Disabled caching prevents Gradle from reusing previous task outputs across branch switches or clean builds. This forces every developer to perform a 'Full Compile' from scratch for every build.",
                resolution = "Set 'org.gradle.caching=true' in your root gradle.properties file.",
                roiAfterFix = "Up to 90% faster rebuilds when switching between feature branches or pulling latest changes."
            ))
        }

        if (props["org.gradle.parallel"] != "true") {
            issues.add(AuditIssue(
                category = "Performance",
                severity = Severity.WARNING,
                title = "Parallel Execution Disabled",
                reasoning = "Gradle executes multi-module builds sequentially by default unless 'org.gradle.parallel' is enabled.",
                impactAnalysis = "Multi-module projects build one module at a time, significantly under-utilizing modern multi-core CPUs and causing much longer overall build times.",
                resolution = "Set 'org.gradle.parallel=true' to allow Gradle to build independent modules concurrently.",
                roiAfterFix = "Significantly faster completion of 'assemble' and 'check' tasks across the entire module graph."
            ))
        }

        if (props["android.enableJetifier"] == "true") {
            issues.add(AuditIssue(
                category = "Performance",
                severity = Severity.ERROR,
                title = "Jetifier is still Enabled",
                reasoning = "Jetifier is an expensive transformation tool that scans and rewrites every dependency during build-time for AndroidX compatibility.",
                impactAnalysis = "Jetifier adds massive overhead to Gradle sync and build times, especially for modern projects that already use AndroidX libraries. In large multi-module projects, this can add several minutes to initial syncs.",
                resolution = "Identify non-AndroidX libraries, update them to modern versions, and set 'android.enableJetifier=false'.",
                roiAfterFix = "Faster Gradle sync times and significantly reduced disk I/O overhead during every build initiation."
            ))
        }

        // 3. Configuration on Demand check
        if (props["org.gradle.configureondemand"] == "true") {
            issues.add(AuditIssue(
                category = "Performance",
                severity = Severity.WARNING,
                title = "Configuration on Demand is Enabled (Deprecated)",
                reasoning = "The property 'org.gradle.configureondemand' is set to true. This feature is deprecated in Gradle 8.x and may cause incorrect build results.",
                impactAnalysis = "Configuration on Demand can skip configuring modules that are actually needed, leading to subtle and hard-to-reproduce build failures, especially in CI/CD environments.",
                resolution = "Remove 'org.gradle.configureondemand=true' from gradle.properties. Use Gradle Configuration Cache instead for modern build performance.",
                roiAfterFix = "Reliable build configuration across all environments and compatibility with Gradle 9.x."
            ))
        }

        // 4. Unnecessary BuildConfig generation
        val isLibrary = context.pluginIds.any { it.contains("com.android.library") }
        val isApp = context.pluginIds.any { it.contains("com.android.application") }
        if (isLibrary || isApp) {
            val hasBuildConfigUsage = hasBuildConfigImport(context)
            val disablesBuildConfig = context.buildFileContent.contains("buildConfig = false") ||
                context.buildFileContent.contains("buildConfig.set(false)") ||
                context.buildFileContent.contains("generateBuildConfig = false") ||
                context.buildFileContent.contains("buildFeatures") && context.buildFileContent.contains("buildConfig = false")

            if (!hasBuildConfigUsage && !disablesBuildConfig && isLibrary) {
                issues.add(AuditIssue(
                    category = "Performance",
                    severity = Severity.WARNING,
                    title = "Unnecessary BuildConfig Generation",
                    reasoning = "Library module '${context.projectName}' generates BuildConfig.java by default but no source files import it. This creates unnecessary compilation work for every build.",
                    impactAnalysis = "Every module generates and compiles BuildConfig.java even if unused. In 50+ module projects this adds seconds to clean builds and pollutes the classpath.",
                    resolution = "Disable BuildConfig generation in your android {} block:\n\nbuildFeatures {\n    buildConfig = false\n}",
                    roiAfterFix = "Fewer generated sources, faster compilation, smaller classpath.",
                    sourceFile = context.buildFile.absolutePath
                ))
            }
        }

        // 5. kotlin-android-extensions deprecation check

        if (context.buildFileContent.contains("kotlin-android-extensions")) {
            issues.add(AuditIssue(
                category = "Performance",
                severity = Severity.ERROR,
                title = "Deprecated kotlin-android-extensions Plugin Detected",
                reasoning = "The kotlin-android-extensions plugin is deprecated since Kotlin 1.8 and removed in newer versions. Synthetic view bindings are no longer maintained.",
                impactAnalysis = "This plugin generates synthetic accessor code that increases compile time and breaks with Compose. It blocks Kotlin version upgrades past 1.8.",
                resolution = "Replace synthetic imports with View Binding:\n1. Remove id(\"kotlin-android-extensions\") from plugins\n2. Enable viewBinding { enabled = true } in buildFeatures\n3. Replace kotlinx.android.synthetic imports with binding.viewId",
                roiAfterFix = "Unblocks Kotlin version upgrades. View Binding is compile-time safe and faster.",
                sourceFile = context.buildFile.absolutePath
            ))
        }

        return issues
    }

    private fun hasBuildConfigImport(context: AuditContext): Boolean {
        return context.sourceSets.any { sourceSet ->
            (sourceSet.kotlinDirs + sourceSet.javaDirs).any { dir ->
                dir.exists() && dir.walkTopDown().any { file ->
                    (file.extension == "kt" || file.extension == "java") &&
                        try { file.readText().contains("BuildConfig") } catch (_: Exception) { false }
                }
            }
        }
    }
}
