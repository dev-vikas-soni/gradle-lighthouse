package com.gradlelighthouse.reporting

import com.gradlelighthouse.core.AuditIssue
import com.gradlelighthouse.core.Severity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * JUnit XML Report Generator for CI/CD Integration.
 *
 * Models each audit category as a "test suite" and each issue as a "test case failure".
 * This format is natively consumed by:
 * - Jenkins (Test Results tab)
 * - Bitrise (Test reports add-on)
 * - CircleCI (Test insights)
 * - TeamCity (Build statistics)
 * - GitHub Actions (with dorny/test-reporter)
 *
 * Follows the JUnit XML schema used by Apache Ant / Maven Surefire.
 */
object JunitXmlReportGenerator {

    /**
     * Generates JUnit XML from audit issues.
     *
     * @param moduleName Name of the analyzed module
     * @param issues List of audit findings
     * @return Valid JUnit XML string
     */
    fun generate(moduleName: String, issues: List<AuditIssue>): String {
        val timestamp = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.ROOT).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }.format(Date())

        val grouped = issues.groupBy { it.category }

        // Generate categories as test suites — dynamically from actual issues + known defaults
        val defaultCategories = listOf(
            "Stability", "Performance", "Modernization", "AppSize",
            "DependencyHealth", "PlayStorePolicy", "Architecture",
            "BuildPerformance", "Security", "Quality", "Complexity",
            "DependencyHygiene", "Trends"
        )
        val allCategories = (defaultCategories + grouped.keys).distinct().sorted()

        val testSuites = allCategories.map { category ->
            val categoryIssues = grouped[category] ?: emptyList()
            buildTestSuite(moduleName, category, categoryIssues, timestamp)
        }

        return buildString {
            appendLine("""<?xml version="1.0" encoding="UTF-8"?>""")
            appendLine("""<testsuites name="Gradle Lighthouse-$moduleName" tests="${issues.size + allCategories.size}" failures="${issues.count { it.severity == Severity.FATAL || it.severity == Severity.ERROR }}" errors="0" time="0">""")
            testSuites.forEach { appendLine(it) }
            appendLine("</testsuites>")
        }
    }

    private fun buildTestSuite(
        moduleName: String,
        category: String,
        issues: List<AuditIssue>,
        timestamp: String
    ): String {
        val failures = issues.count { it.severity == Severity.FATAL || it.severity == Severity.ERROR }

        return buildString {
            appendLine("""  <testsuite name="$category" package="com.gradlelighthouse.$moduleName" tests="${issues.size + 1}" failures="$failures" errors="0" skipped="0" timestamp="$timestamp">""")

            if (issues.isEmpty()) {
                // Passing test case — category is clean
                appendLine("""    <testcase name="$category Compliance" classname="com.gradlelighthouse.$moduleName.$category" time="0" />""")
            } else {
                issues.forEach { issue ->
                    val testName = escapeXml(issue.title)
                    val className = "com.gradlelighthouse.$moduleName.${issue.category}"

                    appendLine("""    <testcase name="$testName" classname="$className" time="0">""")

                    when (issue.severity) {
                        Severity.FATAL, Severity.ERROR -> {
                            appendLine("""      <failure message="${escapeXml(issue.title)}" type="${issue.severity.name}">""")
                            appendLine("Severity: ${issue.severity.name}")
                            appendLine("Reasoning: ${escapeXml(issue.reasoning)}")
                            appendLine("Impact: ${escapeXml(issue.impactAnalysis)}")
                            appendLine("Resolution: ${escapeXml(issue.resolution)}")
                            appendLine("ROI: ${escapeXml(issue.roiAfterFix)}")
                            if (issue.sourceFile != null) {
                                appendLine("File: ${escapeXml(issue.sourceFile)}")
                            }
                            appendLine("""      </failure>""")
                        }
                        Severity.WARNING, Severity.INFO -> {
                            appendLine("""      <system-out>""")
                            appendLine("[${issue.severity.name}] ${escapeXml(issue.title)}")
                            appendLine("Reasoning: ${escapeXml(issue.reasoning)}")
                            appendLine("Resolution: ${escapeXml(issue.resolution)}")
                            appendLine("""      </system-out>""")
                        }
                    }

                    appendLine("""    </testcase>""")
                }
            }

            appendLine("""  </testsuite>""")
        }
    }

    /**
     * Escapes XML special characters to prevent injection.
     */
    private fun escapeXml(text: String): String = text
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&apos;")
}
