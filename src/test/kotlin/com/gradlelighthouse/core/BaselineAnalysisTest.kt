package com.gradlelighthouse.core

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.io.File

class BaselineAnalysisTest {
    private val root = File("/tmp/lighthouse-fixture")

    private fun issue(title: String, sourceFile: String? = null) = AuditIssue(
        category = LighthouseCategory.ARCHITECTURE,
        severity = Severity.WARNING,
        title = title,
        reasoning = "reason",
        impactAnalysis = "impact",
        resolution = "resolution",
        roiAfterFix = "roi",
        sourceFile = sourceFile
    )

    @Test
    fun `empty baseline classifies every finding as new`() {
        val findings = listOf(issue("A"), issue("B"))
        val result = BaselineAnalysis.analyze(findings, emptySet(), root)
        assertEquals(2, result.newIssueCount)
        assertEquals(0, result.baselineIssueCount)
        assertEquals(0, result.staleBaselineCount)
    }

    @Test
    fun `matching fingerprints classify findings as accepted baseline debt`() {
        val existing = issue("Existing")
        val fresh = issue("New")
        val result = BaselineAnalysis.analyze(
            listOf(existing, fresh),
            setOf(existing.fingerprint(root)),
            root
        )
        assertEquals(listOf(existing), result.baselinedIssues)
        assertEquals(listOf(fresh), result.newIssues)
        assertEquals(2, result.totalIssueCount)
        assertEquals(0, result.staleBaselineCount)
    }

    @Test
    fun `stale baseline fingerprints are reported when finding no longer exists`() {
        val result = BaselineAnalysis.analyze(
            listOf(issue("Current")),
            setOf(issue("Removed").fingerprint(root)),
            root
        )
        assertEquals(1, result.newIssueCount)
        assertEquals(0, result.baselineIssueCount)
        assertEquals(1, result.staleBaselineCount)
    }

    @Test
    fun `duplicate baseline fingerprints are counted once as stale or matched`() {
        val current = issue("Current")
        val result = BaselineAnalysis.analyze(
            listOf(current),
            setOf(current.fingerprint(root), current.fingerprint(root)),
            root
        )
        assertEquals(0, result.staleBaselineCount)
        assertEquals(1, result.baselineIssueCount)
    }
}
