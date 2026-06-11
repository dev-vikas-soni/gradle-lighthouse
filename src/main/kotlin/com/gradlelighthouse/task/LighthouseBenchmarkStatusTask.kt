package com.gradlelighthouse.task

import com.gradlelighthouse.core.benchmarking.BenchmarkRegistry
import com.gradlelighthouse.core.benchmarking.JsonBenchmarkProvider
import org.gradle.api.DefaultTask
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault
import java.io.File
import javax.inject.Inject

/**
 * Displays the status of loaded industry benchmarks.
 */
@DisableCachingByDefault(because = "Benchmark status should always reflect the current registry.")
abstract class LighthouseBenchmarkStatusTask @Inject constructor() : DefaultTask() {

    init {
        group = "Gradle Lighthouse"
        description = "Displays the registry of industry benchmarks currently loaded into the engine."
    }

    @get:Input
    abstract val rootDirPath: Property<String>

    @TaskAction
    fun status() {
        val rootDir = File(rootDirPath.get())
        val customBenchDir = File(rootDir, "benchmarks")
        if (customBenchDir.exists()) {
            BenchmarkRegistry.addProvider(JsonBenchmarkProvider(customBenchDir))
        }

        val benchmarks = BenchmarkRegistry.getAllBenchmarks()

        println("\n" + "═".repeat(60))
        println(" GRADLE LIGHTHOUSE BENCHMARK REGISTRY")
        println("═".repeat(60))

        if (benchmarks.isEmpty()) {
            println(" No benchmarks loaded.")
        } else {
            println(" Loaded Benchmarks:")
            benchmarks.sortedByDescending { it.overallScore }.forEach { b ->
                println(" ✓ ${b.projectName.padEnd(20)} | Score: ${b.overallScore.toInt().toString().padStart(3)} | V${b.lighthouseVersion}")
            }

            val avgScore = benchmarks.map { it.overallScore }.average()
            val highest = benchmarks.maxByOrNull { it.overallScore }
            val lowest = benchmarks.minByOrNull { it.overallScore }

            println("-".repeat(60))
            println(" Total Benchmarks: ${benchmarks.size}")
            println(" Industry Average: ${String.format("%.1f", avgScore)}")

            highest?.let {
                println(" Highest Score:    ${it.projectName} (${it.overallScore.toInt()})")
            }
            lowest?.let {
                println(" Lowest Score:     ${it.projectName} (${it.overallScore.toInt()})")
            }
        }
        println("═".repeat(60) + "\n")
    }
}
