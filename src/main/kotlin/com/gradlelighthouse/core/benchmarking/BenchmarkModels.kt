package com.gradlelighthouse.core.benchmarking

import com.gradlelighthouse.core.LighthouseCategory
import java.io.Serializable

/**
 * A point-in-time architectural health snapshot for a project.
 * Generated automatically by lighthouseExportBenchmark.
 */
data class BenchmarkSnapshot(
    val projectName: String,
    val generatedAt: String,
    val lighthouseVersion: String,
    val overallScore: Double,
    val projectMaturity: String,
    val moduleCount: Int,
    val categoryScores: Map<LighthouseCategory, Double>,
    val persona: ProjectPersona = ProjectPersona.MEDIUM_ANDROID_APP
) : Serializable {
    companion object {
        private const val serialVersionUID = 1L
    }
}

/**
 * Result of comparing a project against a specific benchmark.
 */
data class BenchmarkComparison(
    val benchmarkName: String,
    val overallDelta: Double,
    val categoryDeltas: Map<LighthouseCategory, Double>
) : Serializable {
    companion object {
        private const val serialVersionUID = 1L
    }
}

/**
 * Project classifications for peer-group comparison.
 */
enum class ProjectPersona(val displayName: String) {
    SMALL_ANDROID_APP("Small Android App"),
    MEDIUM_ANDROID_APP("Medium Android App"),
    LARGE_ANDROID_APP("Large Android App"),
    ENTERPRISE_ANDROID("Enterprise Android"),
    KMP_LIBRARY("KMP Library"),
    KMP_PRODUCT("KMP Product"),
    LEGACY_MONOLITH("Legacy Monolith"),
    MODULAR_MONOLITH("Modular Monolith")
}

/**
 * Complete benchmarking result for a project.
 */
data class BenchmarkResult(
    val projectPersona: ProjectPersona,
    val overallPercentile: Int,
    val categoryPercentiles: Map<LighthouseCategory, Int>,
    val comparisons: List<BenchmarkComparison>,
    val insights: List<String>
) : Serializable {
    companion object {
        private const val serialVersionUID = 1L
    }
}
