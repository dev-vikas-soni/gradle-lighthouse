package com.gradlelighthouse.auditors

import com.gradlelighthouse.core.AuditContext
import com.gradlelighthouse.core.Auditor
import com.gradlelighthouse.core.AuditIssue
import com.gradlelighthouse.core.ConsoleLogger
import com.gradlelighthouse.core.Severity
import java.io.File

/**
 * ModernizationAuditor: Tracks the evolution of the codebase from XML to Jetpack Compose.
 *
 * Jetpack Compose is the modern standard for Android UI. High XML counts in new modules
 * represent technical debt that increases maintenance costs.
 *
 * Scans both `src/main/kotlin` AND `src/main/java` directories to avoid false negatives
 * in projects that mix Java and Kotlin. Also supports KMP source sets (commonMain, androidMain).
 */
class ModernizationAuditor : Auditor {
    override val name: String = "Modernization"

    override fun audit(context: AuditContext): List<AuditIssue> {
        val issues = mutableListOf<AuditIssue>()
        ConsoleLogger.auditorStart(name, "🎨", "[MOD]", "Scanning Compose vs XML modernization ratio...")

        var xmlCount = 0
        var composeCount = 0

        // Scan ALL source sets (supports KMP: commonMain, androidMain, etc.)
        context.sourceSets.forEach { sourceSet ->
            // Count XML layouts from resource directories
            sourceSet.resDirs.forEach { resDir ->
                val layoutDir = File(resDir, "layout")
                if (layoutDir.exists()) {
                    xmlCount += layoutDir.walkTopDown().filter { it.extension == "xml" }.count()
                }
            }

            // Count @Composable files from BOTH kotlin AND java directories
            val allSourceDirs = sourceSet.kotlinDirs + sourceSet.javaDirs
            allSourceDirs.forEach { srcDir ->
                if (srcDir.exists()) {
                    composeCount += srcDir.walkTopDown()
                        .filter { it.extension == "kt" || it.extension == "java" }
                        .count { file ->
                            try {
                                file.readText().contains("@Composable")
                            } catch (_: Exception) {
                                false
                            }
                        }
                }
            }
        }

        val totalScreens = xmlCount + composeCount
        if (totalScreens == 0) return issues

        val ratio = (composeCount * 100) / totalScreens

        val severity = when {
            ratio >= 80 -> Severity.INFO
            ratio >= 40 -> Severity.WARNING
            else -> Severity.ERROR
        }

        issues.add(AuditIssue(
            category = "Modernization",
            severity = severity,
            title = "Modernization Index: $ratio%",
            reasoning = "Your module is $ratio% modern (Compose) and ${100 - ratio}% legacy (XML). Based on current industry benchmarks, modules with <40% Compose adoption are considered 'Legacy Debt'.",
            impactAnalysis = "Lower Compose ratios lead to slower development cycles, increased binary size due to View-system overhead, and fragmented UI testing frameworks. XML-based UIs are 3x more prone to memory leaks in complex lists.",
            resolution = "Priority: Convert the most frequently changed XML layouts into Jetpack Compose. Start with list items (RecyclerView to LazyColumn) to see immediate performance gains.",
            roiAfterFix = "Estimated 40% reduction in UI boilerplate code and 15% better screen transition performance."
        ))

        if (xmlCount > 20 && ratio < 10) {
            issues.add(AuditIssue(
                category = "Modernization",
                severity = Severity.ERROR,
                title = "High XML Technical Debt",
                reasoning = "Detected $xmlCount XML layouts in a module with near-zero Compose adoption. This module is an 'XML Monolith'.",
                impactAnalysis = "This module is a 'Legacy Hotspot'. New features will take 2x longer to implement due to the inherent complexity of the View system and manual state management.",
                resolution = "Halt further XML development in this module. Implement all new UI screens in Compose and use ComposeView to bridge them into existing fragments.",
                roiAfterFix = "Elimination of View-based lifecycle bugs and significantly faster feature delivery."
            ))
        }

        return issues
    }
}
