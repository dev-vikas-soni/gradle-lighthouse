package com.gradlelighthouse.auditors

import com.gradlelighthouse.core.AuditContext
import com.gradlelighthouse.core.Auditor
import com.gradlelighthouse.core.AuditIssue
import com.gradlelighthouse.core.ConsoleLogger
import com.gradlelighthouse.core.Severity

/**
 * KmpStructureAuditor: Health checks specific to Kotlin Multiplatform projects.
 *
 * Detects:
 * - Heavy platform-specific dependencies leaking into commonMain
 * - Unbalanced source set distribution (too much in androidMain, too little shared)
 * - Missing expect/actual that will cause runtime failures on non-Android targets
 *
 * This auditor only activates when KMP source sets are detected.
 */
class KmpStructureAuditor : Auditor {
    override val name: String = "KmpStructure"

    override fun audit(context: AuditContext): List<AuditIssue> {
        val issues = mutableListOf<AuditIssue>()

        // Only activate if KMP source sets are detected
        val kmpSourceSets = context.sourceSets.filter {
            it.name.contains("Main") && it.name != "main"
        }
        if (kmpSourceSets.isEmpty()) return issues

        ConsoleLogger.auditorStart(name, "🌐", "[KMP]", "Analyzing Kotlin Multiplatform structure...")

        val commonMain = context.sourceSets.find { it.name == "commonMain" }
        val androidMain = context.sourceSets.find { it.name == "androidMain" }

        // 1. Check if commonMain exists
        if (commonMain == null) {
            issues.add(AuditIssue(
                category = "Architecture",
                severity = Severity.ERROR,
                title = "Missing commonMain Source Set",
                reasoning = "A Kotlin Multiplatform project was detected but no 'commonMain' source set was found. All shared business logic should reside in commonMain.",
                impactAnalysis = "Without commonMain, code sharing across platforms is impossible. All logic is duplicated per platform, defeating the purpose of KMP and multiplying maintenance costs linearly with each target.",
                resolution = "Create a 'src/commonMain/kotlin' directory and move shared business logic, data models, and use cases into it. Use 'expect/actual' for platform-specific implementations.",
                roiAfterFix = "True code sharing across Android, iOS, Desktop, and Web targets with a single codebase."
            ))
            return issues
        }

        // 2. Measure code sharing ratio
        val commonKtCount = commonMain.kotlinDirs.sumOf { dir ->
            if (dir.exists()) dir.walkTopDown().filter { it.extension == "kt" }.count() else 0
        }
        val androidKtCount = androidMain?.kotlinDirs?.sumOf { dir ->
            if (dir.exists()) dir.walkTopDown().filter { it.extension == "kt" }.count() else 0
        } ?: 0

        val totalKt = commonKtCount + androidKtCount
        if (totalKt > 0) {
            val shareRatio = (commonKtCount * 100) / totalKt

            if (shareRatio < 30 && totalKt > 10) {
                issues.add(AuditIssue(
                    category = "Architecture",
                    severity = Severity.WARNING,
                    title = "Low Code Sharing: commonMain has only $shareRatio% of Kotlin files",
                    reasoning = "Only $commonKtCount out of $totalKt Kotlin files are in commonMain. The majority of code ($androidKtCount files) is platform-specific in androidMain.",
                    impactAnalysis = "Low sharing ratio means the KMP migration benefit is minimal. Platform-specific code still dominates, resulting in near-zero ROI on the multiplatform investment.",
                    resolution = "Audit androidMain for business logic, data models, and repository patterns that can be extracted to commonMain using expect/actual declarations.",
                    roiAfterFix = "Higher code reuse, reduced total lines of code, and consistent behavior across all targets."
                ))
            }
        }

        // 3. Check for platform-heavy dependencies in common configurations
        val commonDeps = context.dependencies.filter {
            it.configurationName.contains("commonMain", ignoreCase = true)
        }
        val androidOnlyGroups = listOf("com.android", "androidx.", "com.google.android")
        val leakedDeps = commonDeps.filter { dep ->
            androidOnlyGroups.any { dep.group.startsWith(it) }
        }

        if (leakedDeps.isNotEmpty()) {
            issues.add(AuditIssue(
                category = "Architecture",
                severity = Severity.ERROR,
                title = "Android Dependencies Leaked into commonMain (${leakedDeps.size})",
                reasoning = "The following Android-only dependencies were found in commonMain configurations: ${leakedDeps.joinToString { it.notation }}.",
                impactAnalysis = "commonMain code must be platform-agnostic. These dependencies will cause compilation failures on iOS, Desktop, and Web targets, blocking multiplatform builds entirely.",
                resolution = "Move Android-specific dependencies to the 'androidMain' source set. Use expect/actual to abstract platform-specific behavior.",
                roiAfterFix = "Clean compilation across all KMP targets and a correct dependency graph."
            ))
        }

        return issues
    }
}
