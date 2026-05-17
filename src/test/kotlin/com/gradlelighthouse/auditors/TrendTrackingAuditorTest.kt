package com.gradlelighthouse.auditors

import com.gradlelighthouse.core.AuditContext
import com.gradlelighthouse.core.Severity
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class TrendTrackingAuditorTest {

    @TempDir
    lateinit var tempDir: File

    private lateinit var auditor: TrendTrackingAuditor

    @BeforeEach
    fun setup() {
        auditor = TrendTrackingAuditor()
    }

    @Test
    fun `audit returns baseline issue when no history exists`() {
        val context = createMockContext(currentScore = 80)
        val issues = auditor.audit(context)

        assertEquals(1, issues.size)
        assertEquals("First Audit Run — Baseline Established", issues[0].title)
        assertEquals(Severity.INFO, issues[0].severity)
    }

    @Test
    fun `audit returns error when score degrades significantly`() {
        // Prepare history with score 90
        auditor.saveScore(tempDir, "test-project", 90)

        val context = createMockContext(currentScore = 80) // 10 point drop
        val issues = auditor.audit(context)

        assertTrue(issues.any { it.title.contains("Health Score Degraded") })
        assertTrue(issues.any { it.severity == Severity.ERROR })
    }

    @Test
    fun `audit returns info when score improves significantly`() {
        // Prepare history with score 70
        auditor.saveScore(tempDir, "test-project", 70)

        val context = createMockContext(currentScore = 80) // 10 point increase
        val issues = auditor.audit(context)

        assertTrue(issues.any { it.title.contains("Health Score Improved") })
        assertTrue(issues.any { it.severity == Severity.INFO })
    }

    @Test
    fun `audit returns no issues when score change is minor`() {
        auditor.saveScore(tempDir, "test-project", 82)

        val context = createMockContext(currentScore = 80) // 2 point drop
        val issues = auditor.audit(context)

        assertTrue(issues.isEmpty())
    }

    @Test
    fun `saveScore and getPreviousScore work correctly`() {
        auditor.saveScore(tempDir, "test-project", 75)
        assertEquals(75, auditor.getPreviousScore(tempDir, "test-project"))

        auditor.saveScore(tempDir, "test-project", 85)
        assertEquals(85, auditor.getPreviousScore(tempDir, "test-project"))
    }

    private fun createMockContext(currentScore: Int?): AuditContext {
        return AuditContext(
            projectName = "test-project",
            projectPath = ":test-project",
            projectDir = File(tempDir, "project"),
            rootDir = tempDir,
            buildFile = File(tempDir, "build.gradle.kts"),
            buildFileContent = "",
            gradleVersion = "8.12",
            pluginIds = emptySet(),
            dependencies = emptyList(),
            resolvedDependencies = emptyList(),
            repositories = emptyList(),
            gradleProperties = emptyMap(),
            sourceSets = emptyList(),
            hasVersionCatalog = false,
            currentScore = currentScore
        )
    }
}
