package com.gradlelighthouse.core.benchmarking

import com.gradlelighthouse.core.LighthouseCategory
import com.gradlelighthouse.core.scoring.ScoringResult
import java.io.File
import kotlin.math.roundToInt

/**
 * Core engine for industry benchmarking and percentile calculation.
 */
class BenchmarkEngine(
    private val customBenchmarkDir: File? = null
) {

    private val benchmarks: List<BenchmarkSnapshot> by lazy {
        if (customBenchmarkDir != null && customBenchmarkDir.exists()) {
            BenchmarkRegistry.addProvider(JsonBenchmarkProvider(customBenchmarkDir))
        }
        BenchmarkRegistry.getAllBenchmarks().sortedByDescending { it.overallScore }
    }

    /**
     * Main entry point: Performs a full benchmarking audit for the current project.
     */
    fun benchmark(result: ScoringResult, moduleCount: Int, pluginIds: Set<String>): BenchmarkResult {
        val persona = classifyProject(moduleCount, pluginIds)

        // Percentiles relative to ALL industry benchmarks
        val overallPercentile = calculatePercentile(result.overallScore, benchmarks.map { it.overallScore })

        val categoryPercentiles = LighthouseCategory.entries.associateWith { category ->
            val myScore = result.categoryScores.find { it.category == category }?.score ?: 100.0
            val benchmarkScores = benchmarks.mapNotNull { it.categoryScores[category] }
            if (benchmarkScores.isNotEmpty()) {
                calculatePercentile(myScore, benchmarkScores)
            } else {
                100 // Default if no data
            }
        }

        // Comparisons against top known projects
        val comparisons = benchmarks.take(5).map { target ->
            val catDeltas = LighthouseCategory.entries.associateWith { category ->
                val myScore = result.categoryScores.find { it.category == category }?.score ?: 100.0
                val targetScore = target.categoryScores[category] ?: target.overallScore
                myScore - targetScore
            }
            BenchmarkComparison(target.projectName, result.overallScore - target.overallScore, catDeltas)
        }

        return BenchmarkResult(
            projectPersona = persona,
            overallPercentile = overallPercentile,
            categoryPercentiles = categoryPercentiles,
            comparisons = comparisons,
            insights = generateInsights(result, persona, overallPercentile, categoryPercentiles, comparisons)
        )
    }

    private fun calculatePercentile(score: Double, dataset: List<Double>): Int {
        if (dataset.isEmpty()) return 100
        val sorted = dataset.sorted()
        val countBelow = sorted.count { it < score }
        return (100.0 * countBelow / sorted.size).roundToInt()
    }

    private fun classifyProject(moduleCount: Int, pluginIds: Set<String>): ProjectPersona {
        val isKmp = pluginIds.any { it.contains("multiplatform") }

        return when {
            isKmp && moduleCount > 20 -> ProjectPersona.KMP_PRODUCT
            isKmp -> ProjectPersona.KMP_LIBRARY
            moduleCount >= 100 -> ProjectPersona.ENTERPRISE_ANDROID
            moduleCount >= 40 -> ProjectPersona.LARGE_ANDROID_APP
            moduleCount >= 10 -> ProjectPersona.MEDIUM_ANDROID_APP
            moduleCount > 1 -> ProjectPersona.SMALL_ANDROID_APP
            else -> ProjectPersona.LEGACY_MONOLITH
        }
    }

    private fun generateInsights(
        result: ScoringResult,
        persona: ProjectPersona,
        percentile: Int,
        categoryPercentiles: Map<LighthouseCategory, Int>,
        comparisons: List<BenchmarkComparison>
    ): List<String> {
        val insights = mutableListOf<String>()

        insights.add("Your project is classified as a ${persona.displayName} based on its structural profile.")

        if (percentile >= 90) {
            insights.add("Elite Performance: You are in the top 10% of benchmarked projects globally.")
        } else {
            insights.add("Your overall health is better than $percentile% of industry projects.")
        }

        // Relative category strengths
        val bestCat = categoryPercentiles.maxByOrNull { it.value }
        if (bestCat != null && bestCat.value >= 80) {
            insights.add("${bestCat.key.displayName} is your strongest area relative to industry peers (Top ${100 - bestCat.value}%).")
        }

        val worstCat = categoryPercentiles.minByOrNull { it.value }
        if (worstCat != null && worstCat.value <= 30) {
            insights.add("${worstCat.key.displayName} is your weakest area relative to industry peers (Bottom ${worstCat.value}%).")
        }

        // Direct comparisons
        comparisons.find { it.benchmarkName.contains("Signal", ignoreCase = true) }?.let { signal ->
            if (signal.overallDelta > 0) {
                insights.add("You outperform Signal Android by ${signal.overallDelta.toInt()} points in overall health.")
            }
        }

        return insights
    }
}
