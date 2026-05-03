package com.gradlelighthouse.auditors

import com.gradlelighthouse.core.AuditContext
import com.gradlelighthouse.core.Auditor
import com.gradlelighthouse.core.AuditIssue
import com.gradlelighthouse.core.ConsoleLogger
import com.gradlelighthouse.core.Severity
import java.io.File

/**
 * TestCoverageAuditor: Ensures every module has test sources and coverage configuration.
 *
 * Checks for:
 * - Modules with zero test sources (dark modules)
 * - Missing JaCoCo coverage configuration
 * - Missing consumer-rules.pro in library modules
 */
class TestCoverageAuditor : Auditor {
    override val name: String = "TestCoverage"

    override fun audit(context: AuditContext): List<AuditIssue> {
        val issues = mutableListOf<AuditIssue>()
        ConsoleLogger.auditorStart(name, "🧪", "[TEST]", "Checking test coverage presence...")

        val projectDir = context.projectDir

        // 1. Check for test sources
        val testDirs = listOf(
            File(projectDir, "src/test"),
            File(projectDir, "src/androidTest"),
            File(projectDir, "src/commonTest")
        )

        val hasTestSources = testDirs.any { dir ->
            dir.exists() && dir.walkTopDown().any { it.extension == "kt" || it.extension == "java" }
        }

        if (!hasTestSources) {
            issues.add(AuditIssue(
                category = "Quality",
                severity = Severity.ERROR,
                title = "No Test Sources Found (Dark Module)",
                reasoning = "Module '${context.projectName}' has no test files in src/test, src/androidTest, or src/commonTest. This module is completely untested.",
                impactAnalysis = "Untested modules are invisible risk vectors. Bugs ship silently, regressions go undetected, and refactoring becomes dangerous. In large projects, dark modules become unmaintainable over time.",
                resolution = "Add at least unit tests for public API classes. Create src/test/kotlin/ directory and add basic tests using JUnit5 or KotlinTest. Target >60% line coverage for business logic modules.",
                roiAfterFix = "Early bug detection, safe refactoring, reduced QA cycle time. Each caught regression saves 2-8h of developer time."
            ))
        } else {
            // Count test files
            val testFileCount = testDirs.filter { it.exists() }
                .sumOf { dir -> dir.walkTopDown().count { it.extension == "kt" || it.extension == "java" } }

            // Count source files
            val srcFileCount = context.sourceSets
                .flatMap { it.kotlinDirs + it.javaDirs }
                .filter { it.exists() }
                .sumOf { dir -> dir.walkTopDown().count { it.extension == "kt" || it.extension == "java" } }

            if (srcFileCount > 0 && testFileCount.toFloat() / srcFileCount < 0.3f) {
                issues.add(AuditIssue(
                    category = "Quality",
                    severity = Severity.WARNING,
                    title = "Low Test-to-Source Ratio: ${testFileCount}/${srcFileCount} files",
                    reasoning = "Module '${context.projectName}' has only $testFileCount test files for $srcFileCount source files (ratio: ${String.format("%.0f", testFileCount.toFloat() / srcFileCount * 100)}%). Recommended minimum is 30%.",
                    impactAnalysis = "Low test coverage means most code paths are unverified. Regressions will be caught late in the QA cycle or by users.",
                    resolution = "Prioritize testing public API surfaces, business logic, and data transformations. Use code coverage tools (JaCoCo) to identify untested critical paths.",
                    roiAfterFix = "Higher confidence in releases, faster PR review cycles, reduced hotfix frequency."
                ))
            }
        }

        // 2. Check for JaCoCo plugin
        val hasJacoco = context.pluginIds.any { it.contains("jacoco", ignoreCase = true) } ||
            context.buildFileContent.contains("jacoco", ignoreCase = true)

        if (hasTestSources && !hasJacoco) {
            issues.add(AuditIssue(
                category = "Quality",
                severity = Severity.INFO,
                title = "JaCoCo Coverage Not Configured",
                reasoning = "Module has tests but no JaCoCo coverage reporting configured. Without coverage metrics, you can't enforce quality gates or track coverage trends.",
                impactAnalysis = "No visibility into which code paths are actually tested. Coverage may be high on simple classes but zero on critical business logic.",
                resolution = "Apply the 'jacoco' plugin and configure jacocoTestReport task. Set minimum coverage thresholds with jacocoTestCoverageVerification.",
                roiAfterFix = "Quantified test quality, enforceable coverage gates in CI, visibility into testing gaps."
            ))
        }

        // 3. Check for consumer-rules.pro in library modules
        val isLibrary = context.pluginIds.any { it.contains("com.android.library") }
        if (isLibrary) {
            val consumerRules = File(projectDir, "consumer-rules.pro")
            val hasConsumerRulesRef = context.buildFileContent.contains("consumerProguardFiles")

            if (!consumerRules.exists() && !hasConsumerRulesRef) {
                issues.add(AuditIssue(
                    category = "Quality",
                    severity = Severity.WARNING,
                    title = "Library Module Missing consumer-rules.pro",
                    reasoning = "Library module '${context.projectName}' has no consumer-rules.pro. If this library exposes public APIs used via reflection (e.g., serialization models), consuming apps may crash with R8 enabled.",
                    impactAnalysis = "Apps consuming this library in release mode (with R8) may experience ClassNotFoundException or missing field errors if ProGuard strips classes this library needs.",
                    resolution = "Create consumer-rules.pro with necessary keep rules for your public API. Add consumerProguardFiles(\"consumer-rules.pro\") to your android { defaultConfig { } } block.",
                    roiAfterFix = "Prevents silent production crashes in all consuming applications."
                ))
            }
        }

        return issues
    }
}

