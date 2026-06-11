package com.gradlelighthouse.core.benchmarking

import com.gradlelighthouse.core.scoring.ScoringResult
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class BenchmarkEngineTest {

    private val engine = BenchmarkEngine()

    @Test
    fun `percentile calculation is accurate`() {
        val result = engine.benchmark(
            result = createMockResult(85.0),
            moduleCount = 50,
            pluginIds = setOf("com.android.application")
        )

        // Based on current resources: nowinandroid (72.83), Signal (40.95)
        // 85 is better than both (2/2 = 100%)
        assertEquals(100, result.overallPercentile)
    }

    @Test
    fun `project classification handles KMP`() {
        val result = engine.benchmark(
            result = createMockResult(90.0),
            moduleCount = 5,
            pluginIds = setOf("org.jetbrains.kotlin.multiplatform")
        )

        assertEquals(ProjectPersona.KMP_LIBRARY, result.projectPersona)
    }

    @Test
    fun `project classification handles enterprise apps`() {
        val result = engine.benchmark(
            result = createMockResult(70.0),
            moduleCount = 150,
            pluginIds = setOf("com.android.application")
        )

        assertEquals(ProjectPersona.ENTERPRISE_ANDROID, result.projectPersona)
    }

    @Test
    fun `comparisons contain industry giants`() {
        val result = engine.benchmark(
            result = createMockResult(72.0),
            moduleCount = 40,
            pluginIds = emptySet()
        )

        // The comparison uses "contains" check for Signal
        val signal = result.comparisons.find { it.benchmarkName.contains("Signal", ignoreCase = true) }
        assertTrue(signal != null)
        // Signal score is 40.95. Delta: 72 - 40.95 = 31.05
        assertEquals(31.05, signal!!.overallDelta, 0.01)
    }

    private fun createMockResult(score: Double) = ScoringResult(
        overallScore = score,
        categoryScores = emptyList(),
        improvements = emptyList()
    )
}
