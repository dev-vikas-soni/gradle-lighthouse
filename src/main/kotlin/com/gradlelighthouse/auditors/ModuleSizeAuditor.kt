package com.gradlelighthouse.auditors

import com.gradlelighthouse.core.AuditContext
import com.gradlelighthouse.core.Auditor
import com.gradlelighthouse.core.AuditIssue
import com.gradlelighthouse.core.ConsoleLogger
import com.gradlelighthouse.core.Severity
import java.io.File

/**
 * ModuleSizeAuditor: Reports module size and complexity metrics.
 *
 * Checks for:
 * - Lines of code per module
 * - Number of public API classes
 * - Build file complexity
 * - Module splitting recommendations
 */
class ModuleSizeAuditor : Auditor {
    override val name: String = "ModuleSize"

    companion object {
        private const val LOC_WARNING_THRESHOLD = 10_000
        private const val LOC_ERROR_THRESHOLD = 25_000
        private const val PUBLIC_API_THRESHOLD = 50
        private const val BUILD_FILE_COMPLEXITY_THRESHOLD = 150
    }

    override fun audit(context: AuditContext): List<AuditIssue> {
        val issues = mutableListOf<AuditIssue>()
        ConsoleLogger.auditorStart(name, "📐", "[SIZE]", "Measuring module size and complexity...")

        // 1. Count lines of code
        var totalLoc = 0
        var publicClassCount = 0
        context.sourceSets.forEach { sourceSet ->
            val dirs = sourceSet.kotlinDirs + sourceSet.javaDirs
            dirs.forEach { dir ->
                if (dir.exists()) {
                    dir.walkTopDown()
                        .filter { it.extension == "kt" || it.extension == "java" }
                        .forEach { file ->
                            try {
                                val lines = file.readLines()
                                totalLoc += lines.count { it.isNotBlank() }
                                // Count public classes/interfaces/objects
                                publicClassCount += lines.count { line ->
                                    val trimmed = line.trim()
                                    (trimmed.startsWith("class ") || trimmed.startsWith("interface ") ||
                                        trimmed.startsWith("object ") || trimmed.startsWith("data class ") ||
                                        trimmed.startsWith("sealed class ") || trimmed.startsWith("enum class ")) &&
                                        !trimmed.startsWith("private ") && !trimmed.startsWith("internal ")
                                }
                            } catch (_: Exception) { }
                        }
                }
            }
        }

        if (totalLoc > LOC_ERROR_THRESHOLD) {
            issues.add(AuditIssue(
                category = "Complexity",
                severity = Severity.ERROR,
                title = "Oversized Module: ${totalLoc} Lines of Code",
                reasoning = "Module '${context.projectName}' has $totalLoc lines of code (threshold: $LOC_ERROR_THRESHOLD). This indicates the module has accumulated too many responsibilities and should be split.",
                impactAnalysis = "Oversized modules have long compilation times (even small changes recompile the entire module), high merge conflict rates, and resist parallelization. Developer cognitive load is high.",
                resolution = "Split the module along domain boundaries. Extract screen-specific code into feature modules. Move shared utilities into :core:* modules. Aim for modules under ${LOC_WARNING_THRESHOLD} LOC.",
                roiAfterFix = "50-70% faster incremental builds for this module. Reduced merge conflicts. Better team ownership boundaries."
            ))
        } else if (totalLoc > LOC_WARNING_THRESHOLD) {
            issues.add(AuditIssue(
                category = "Complexity",
                severity = Severity.WARNING,
                title = "Large Module: ${totalLoc} Lines of Code",
                reasoning = "Module '${context.projectName}' has $totalLoc lines of code (warning threshold: $LOC_WARNING_THRESHOLD). Consider splitting before it becomes unmanageable.",
                impactAnalysis = "Large modules are on a trajectory toward becoming monoliths. Incremental build times grow linearly with module size.",
                resolution = "Identify logical sub-domains within this module and plan extraction into smaller modules. Start with the most independent subdirectory.",
                roiAfterFix = "Proactive architecture maintenance prevents future costly refactoring."
            ))
        }

        // 2. Public API surface
        if (publicClassCount > PUBLIC_API_THRESHOLD) {
            issues.add(AuditIssue(
                category = "Complexity",
                severity = Severity.WARNING,
                title = "Large Public API Surface: $publicClassCount Classes",
                reasoning = "Module '${context.projectName}' exposes $publicClassCount public classes/interfaces (threshold: $PUBLIC_API_THRESHOLD). This indicates the module may be doing too much or has insufficient access control.",
                impactAnalysis = "Large API surfaces are hard to maintain, version, and document. Consumers couple to internal details that should be private. Breaking changes become frequent.",
                resolution = "Mark internal implementation classes as 'internal' in Kotlin. Use api vs implementation configuration to control transitive exposure. Consider a facade pattern.",
                roiAfterFix = "Smaller API surface = fewer breaking changes, easier upgrades, better encapsulation."
            ))
        }

        // 3. Build file complexity
        val buildFileLines = context.buildFileContent.lines().count { it.isNotBlank() }
        if (buildFileLines > BUILD_FILE_COMPLEXITY_THRESHOLD) {
            issues.add(AuditIssue(
                category = "Complexity",
                severity = Severity.WARNING,
                title = "Complex Build File: $buildFileLines Lines",
                reasoning = "The build.gradle.kts for '${context.projectName}' has $buildFileLines non-blank lines (threshold: $BUILD_FILE_COMPLEXITY_THRESHOLD). Complex build scripts are hard to maintain and slow to configure.",
                impactAnalysis = "Complex build files are error-prone, slow down Gradle configuration, and make it difficult to apply convention plugins or migrate to new Gradle features.",
                resolution = "Extract repetitive logic into convention plugins (build-logic/ included build). Use version catalogs for dependencies. Move custom task definitions to separate plugin classes.",
                roiAfterFix = "Maintainable build scripts, faster onboarding, easier Gradle upgrades.",
                sourceFile = context.buildFile.absolutePath
            ))
        }

        return issues
    }
}

