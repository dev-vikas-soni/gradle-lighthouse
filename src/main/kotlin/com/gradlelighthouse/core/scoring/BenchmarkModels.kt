package com.gradlelighthouse.core.scoring

import com.gradlelighthouse.core.LighthouseCategory
import java.io.Serializable

/**
 * Historical and peer-group benchmark data for a specific metric.
 */
data class MetricDistribution(
    val p50: Double,
    val p90: Double,
    val p95: Double,
    val average: Double
) : Serializable

/**
 * Encapsulates the reference scores for the "Android Architecture Index".
 */
data class ArchitectureIndex(
    val overallDistribution: MetricDistribution,
    val categoryDistributions: Map<LighthouseCategory, MetricDistribution>,
    val referenceProjects: List<ReferenceProject>
) : Serializable

data class ReferenceProject(
    val name: String,
    val overallScore: Double,
    val version: String
) : Serializable

/**
 * A snapshot of a project's health compared against benchmarks.
 */
data class BenchmarkComparison(
    val percentile: Int,
    val diffFromAverage: Double,
    val peerGroup: String // e.g. "KMP", "Large Android App", "Library"
) : Serializable
