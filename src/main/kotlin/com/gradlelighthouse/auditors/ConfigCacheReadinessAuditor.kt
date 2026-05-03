package com.gradlelighthouse.auditors

import com.gradlelighthouse.core.AuditContext
import com.gradlelighthouse.core.Auditor
import com.gradlelighthouse.core.AuditIssue
import com.gradlelighthouse.core.ConsoleLogger
import com.gradlelighthouse.core.Severity

/**
 * ConfigCacheReadinessAuditor: Analyzes Configuration Cache compatibility and build performance.
 *
 * Checks for:
 * - Configuration Cache compatibility signals
 * - Eager task creation patterns (allprojects/subprojects misuse)
 * - Tasks without proper inputs/outputs declarations
 * - Non-cacheable custom task patterns
 * - Build script anti-patterns slowing configuration
 */
class ConfigCacheReadinessAuditor : Auditor {
    override val name: String = "ConfigCacheReadiness"

    override fun audit(context: AuditContext): List<AuditIssue> {
        val issues = mutableListOf<AuditIssue>()
        ConsoleLogger.auditorStart(name, "⚙️", "[CC]", "Analyzing Configuration Cache readiness...")

        val buildContent = context.buildFileContent
        val props = context.gradleProperties

        // 1. Check if configuration cache is enabled
        if (props["org.gradle.configuration-cache"] != "true" && props["org.gradle.unsafe.configuration-cache"] != "true") {
            issues.add(AuditIssue(
                category = "BuildPerformance",
                severity = Severity.ERROR,
                title = "Configuration Cache Not Enabled",
                reasoning = "The Gradle Configuration Cache is not enabled. This feature serializes the task graph after configuration and reuses it in subsequent builds, dramatically reducing configuration time.",
                impactAnalysis = "Every build invocation re-configures all projects from scratch, adding 5-30s+ in 50+ module projects. This is the single biggest ROI improvement for large codebases.",
                resolution = "Add 'org.gradle.configuration-cache=true' to gradle.properties. Fix any compatibility issues flagged by Gradle. Use '--configuration-cache-problems=warn' initially.",
                roiAfterFix = "Up to 90% faster configuration phase. For a 50-module project, this can save 10-25s per build invocation.",
                sourceFile = "gradle.properties"
            ))
        }

        // 2. Check for allprojects/subprojects blocks (config cache anti-pattern)
        if (buildContent.contains("allprojects") || buildContent.contains("subprojects")) {
            val isRoot = context.projectPath == ":" || context.projectName == context.rootDir.name
            if (isRoot) {
                issues.add(AuditIssue(
                    category = "BuildPerformance",
                    severity = Severity.ERROR,
                    title = "allprojects/subprojects Blocks Detected (Configuration Anti-Pattern)",
                    reasoning = "Root build.gradle.kts uses allprojects{} or subprojects{} blocks which force eager configuration of all modules and are incompatible with Configuration Cache and Isolated Projects.",
                    impactAnalysis = "Forces Gradle to configure every module even when only one is being built. Blocks migration to Configuration Cache and the upcoming Isolated Projects feature in Gradle 9.x.",
                    resolution = "Replace with convention plugins applied via plugins{} block in each module, or use a shared build-logic/ included build with precompiled script plugins.",
                    roiAfterFix = "Enables Configuration Cache, reduces configuration time by 50-90%, and prepares for Gradle 9.x Isolated Projects.",
                    sourceFile = context.buildFile.absolutePath
                ))
            }
        }

        // 3. Detect eager task creation patterns
        val eagerTaskPatterns = listOf(
            "tasks.create(" to "tasks.create()",
            "task(" to "task() (Groovy-style)",
            "tasks.getByName(" to "tasks.getByName()"
        )
        for ((pattern, label) in eagerTaskPatterns) {
            if (buildContent.contains(pattern)) {
                issues.add(AuditIssue(
                    category = "BuildPerformance",
                    severity = Severity.WARNING,
                    title = "Eager Task Creation Detected: $label",
                    reasoning = "Build script uses '$label' which eagerly creates/configures tasks even when they won't be executed. This slows down the configuration phase.",
                    impactAnalysis = "Every ./gradlew command pays the cost of configuring tasks that may not even run. In large projects this compounds significantly.",
                    resolution = "Replace with lazy task registration: tasks.register(\"name\") { ... } instead of tasks.create(\"name\") { ... }. Use tasks.named() instead of tasks.getByName().",
                    roiAfterFix = "Faster configuration phase; only tasks in the execution graph get configured.",
                    sourceFile = context.buildFile.absolutePath
                ))
                break // One finding is enough
            }
        }

        // 4. Check for Project access at execution time patterns
        val executionTimeProjectAccess = listOf(
            "project.configurations",
            "project.dependencies",
            "project.tasks",
            "project.extensions"
        )
        val hasCustomTaskWithProjectAccess = executionTimeProjectAccess.any { buildContent.contains(it) }
        if (hasCustomTaskWithProjectAccess && buildContent.contains("@TaskAction")) {
            issues.add(AuditIssue(
                category = "BuildPerformance",
                severity = Severity.ERROR,
                title = "Project Access in Task Action (Configuration Cache Incompatible)",
                reasoning = "Custom tasks appear to access the Project object at execution time (inside @TaskAction). This is incompatible with Configuration Cache.",
                impactAnalysis = "Configuration Cache cannot serialize these tasks, forcing full re-configuration every build. Gradle will report errors when CC is enabled.",
                resolution = "Capture all needed data as @Input properties during configuration phase. Pass data to the task action via providers, not direct Project references.",
                roiAfterFix = "Full Configuration Cache compatibility for custom build logic.",
                sourceFile = context.buildFile.absolutePath
            ))
        }

        // 5. Check for missing configuration cache problems mode
        if (props["org.gradle.configuration-cache"] == "true" && props["org.gradle.configuration-cache.problems"] == null) {
            issues.add(AuditIssue(
                category = "BuildPerformance",
                severity = Severity.INFO,
                title = "Configuration Cache Problems Mode Not Set",
                reasoning = "Configuration Cache is enabled but 'org.gradle.configuration-cache.problems' is not configured. Default is 'fail' which may break builds during migration.",
                impactAnalysis = "Any incompatible plugin or script will immediately fail the build rather than reporting warnings.",
                resolution = "During migration, set 'org.gradle.configuration-cache.problems=warn' in gradle.properties. Switch to 'fail' once all issues are resolved.",
                roiAfterFix = "Smoother incremental migration to Configuration Cache.",
                sourceFile = "gradle.properties"
            ))
        }

        // 6. Detect buildSrc usage (slows configuration)
        val buildSrcDir = java.io.File(context.rootDir, "buildSrc")
        if (buildSrcDir.exists() && buildSrcDir.isDirectory) {
            issues.add(AuditIssue(
                category = "BuildPerformance",
                severity = Severity.WARNING,
                title = "buildSrc Detected — Consider Composite Builds",
                reasoning = "buildSrc changes invalidate the entire build cache and force full re-configuration of all modules. Composite builds (build-logic/) avoid this.",
                impactAnalysis = "Any change to buildSrc (even a comment) triggers a full rebuild of the buildSrc project and invalidates all cached configuration. In CI, this means no cache hits after build-logic changes.",
                resolution = "Migrate buildSrc/ to an included build: create 'build-logic/' directory with its own settings.gradle.kts and include it via includeBuild(\"build-logic\") in settings.",
                roiAfterFix = "Granular cache invalidation — only affected convention plugins are recompiled. CI cache hit rate improves dramatically."
            ))
        }

        // 7. Check for non-transitive R class
        if (props["android.nonTransitiveRClass"] != "true") {
            val hasAndroidPlugin = context.pluginIds.any {
                it.contains("com.android.application") || it.contains("com.android.library")
            }
            if (hasAndroidPlugin) {
                issues.add(AuditIssue(
                    category = "BuildPerformance",
                    severity = Severity.WARNING,
                    title = "Non-Transitive R Class Not Enabled",
                    reasoning = "android.nonTransitiveRClass=true reduces R.class size and compilation time by not inheriting resources from dependencies into each module's R class.",
                    impactAnalysis = "Each module's R.java contains ALL transitive resources, causing massive regeneration on any resource change in any dependency. Build times scale quadratically with module count.",
                    resolution = "Add 'android.nonTransitiveRClass=true' to gradle.properties. Fix any R.drawable/R.string references that relied on transitive access.",
                    roiAfterFix = "30-50% faster incremental builds in resource-heavy multi-module projects.",
                    sourceFile = "gradle.properties"
                ))
            }
        }

        return issues
    }
}

