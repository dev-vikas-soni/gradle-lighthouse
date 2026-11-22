package com.gradlelighthouse.reporting

import com.gradlelighthouse.core.AuditIssue
import com.gradlelighthouse.core.BaselineAnalysis
import com.gradlelighthouse.core.LighthouseCategory
import com.gradlelighthouse.core.Severity
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

class JunitXmlReportGeneratorBaselineTest {
    private val root = File("/tmp/lighthouse-junit-baseline")

    private fun issue(title: String, severity: Severity) = AuditIssue(
        category = LighthouseCategory.ARCHITECTURE,
        severity = severity,
        title = title,
        reasoning = "reason",
        impactAnalysis = "impact",
        resolution = "resolution",
        roiAfterFix = "roi"
    )

    @Test
    fun `baseline-only fatal is not reported as a regression failure`() {
        val existing = issue("Existing fatal", Severity.FATAL)
        val analysis = BaselineAnalysis.analyze(
            listOf(existing),
            setOf(existing.fingerprint(root)),
            root
        )
        val xml = JunitXmlReportGenerator.generate("fixture", analysis.newIssues, analysis)

        assertTrue(xml.contains("total-findings=\"1\""))
        assertTrue(xml.contains("new-findings=\"0\""))
        assertTrue(xml.contains("baselined-findings=\"1\""))
        assertTrue(xml.contains("failures=\"0\""))
    }

    @Test
    fun `new fatal regression remains a junit failure`() {
        val existing = issue("Existing fatal", Severity.FATAL)
        val fresh = issue("New fatal", Severity.FATAL)
        val analysis = BaselineAnalysis.analyze(
            listOf(existing, fresh),
            setOf(existing.fingerprint(root)),
            root
        )
        val xml = JunitXmlReportGenerator.generate("fixture", analysis.newIssues, analysis)

        assertTrue(xml.contains("total-findings=\"2\""))
        assertTrue(xml.contains("new-findings=\"1\""))
        assertTrue(xml.contains("baselined-findings=\"1\""))
        assertTrue(xml.contains("failures=\"1\""))
        assertTrue(xml.contains("New fatal"))
        assertTrue(!xml.contains("Existing fatal"))
    }
}
