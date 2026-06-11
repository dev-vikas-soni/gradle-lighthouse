package com.gradlelighthouse.task

import com.gradlelighthouse.core.ConsoleLogger
import com.gradlelighthouse.core.LighthouseCategory
import com.gradlelighthouse.core.scoring.CategoryScore
import com.gradlelighthouse.core.scoring.ScoringResult
import com.gradlelighthouse.LighthousePlugin
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault
import java.io.File
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject

/**
 * Exports project health data as a benchmark snapshot.
 */
@DisableCachingByDefault(because = "Benchmark export should always reflect the current state.")
abstract class LighthouseExportBenchmarkTask @Inject constructor() : DefaultTask() {

    init {
        group = "Gradle Lighthouse"
        description = "Exports the project architectural health as a JSON benchmark snapshot."
    }

    @get:Input abstract val projectName: Property<String>
    @get:Input abstract val pluginVersion: Property<String>

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val moduleReportDirs: org.gradle.api.file.ConfigurableFileCollection

    @get:OutputDirectory abstract val outputDir: DirectoryProperty

    @TaskAction
    fun export() {
        val reports = mutableListOf<LighthouseAggregateTask.ModuleReportData>()
        moduleReportDirs.files.forEach { dir ->
            val reportFile = File(dir, "module-report.json")
            if (reportFile.exists()) {
                parseReportJson(reportFile.readText())?.let { reports.add(it) }
            }
        }

        if (reports.isEmpty()) {
            ConsoleLogger.error("No module reports found. Run './gradlew lighthouseAudit' first.")
            return
        }

        val avgScore = reports.map { it.score }.average()
        val totalModules = reports.size

        // Category aggregation
        val projectCategories = mutableMapOf<String, MutableList<LighthouseAggregateTask.ModuleCategoryData>>()
        reports.forEach { report ->
            report.categoryScores.forEach { cat ->
                projectCategories.getOrPut(cat.displayName) { mutableListOf() }.add(cat)
            }
        }

        val catMap = projectCategories.map { (displayName, dataList) ->
            val avgCatScore = dataList.map { it.score }.average()
            val catEnum = LighthouseCategory.entries.find { it.displayName == displayName }
            catEnum to avgCatScore
        }.filter { it.first != null }.associate { it.first!! to it.second!! }

        val timestamp = ZonedDateTime.now().format(DateTimeFormatter.ISO_INSTANT)
        val maturity = com.gradlelighthouse.core.HealthScoreEngine.ArchitectRank.fromScore(avgScore.toInt()).displayName

        // Infer persona for the export
        val isKmp = reports.any { r -> r.issues.any { it.category == "KMP_STRUCTURE" } }
        val persona = when {
            isKmp && totalModules > 20 -> "KMP_PRODUCT"
            isKmp -> "KMP_LIBRARY"
            totalModules >= 100 -> "ENTERPRISE_ANDROID"
            totalModules >= 40 -> "LARGE_ANDROID_APP"
            totalModules >= 10 -> "MEDIUM_ANDROID_APP"
            totalModules > 1 -> "SMALL_ANDROID_APP"
            else -> "LEGACY_MONOLITH"
        }

        val json = buildString {
            appendLine("{")
            appendLine("  \"projectName\": \"${projectName.get()}\",")
            appendLine("  \"generatedAt\": \"$timestamp\",")
            appendLine("  \"lighthouseVersion\": \"${pluginVersion.get()}\",")
            appendLine("  \"overallScore\": ${String.format("%.2f", avgScore)},")
            appendLine("  \"projectMaturity\": \"$maturity\",")
            appendLine("  \"moduleCount\": $totalModules,")
            appendLine("  \"persona\": \"$persona\",")
            appendLine("  \"categoryScores\": {")
            val catEntries = catMap.entries.toList()
            catEntries.forEachIndexed { i, entry ->
                val comma = if (i < catEntries.size - 1) "," else ""
                appendLine("    \"${entry.key.name}\": ${String.format("%.2f", entry.value)}$comma")
            }
            appendLine("  }")
            appendLine("}")
        }

        val outFile = File(outputDir.get().asFile, "benchmark.json")
        outFile.writeText(json)

        ConsoleLogger.success("Benchmark snapshot exported successfully to: ${outFile.toURI()}")
    }

    private fun parseReportJson(json: String): LighthouseAggregateTask.ModuleReportData? {
        // Reuse parsing logic from Aggregate task (simplified)
        return try {
            val moduleName = extractStr(json, "module") ?: return null
            val projectPath = extractStr(json, "path") ?: ":$moduleName"
            val score = extractInt(json, "score") ?: 0

            // Minimal Category parsing
            val categoryScores = mutableListOf<LighthouseAggregateTask.ModuleCategoryData>()
            val catPattern = "\"displayName\"\\s*:\\s*\"([^\"]+)\"\\s*,\\s*\"score\"\\s*:\\s*([\\d.]+)".toRegex()
            catPattern.findAll(json).forEach { match ->
                categoryScores.add(LighthouseAggregateTask.ModuleCategoryData(
                    name = "", displayName = match.groupValues[1], score = match.groupValues[2].toDouble(),
                    grade = "", fatalCount = 0, errorCount = 0, warningCount = 0, infoCount = 0, topRisks = emptyList()
                ))
            }

            LighthouseAggregateTask.ModuleReportData(
                moduleName, projectPath, score, "", 0, 0, 0, 0, "", categoryScores, emptyList()
            )
        } catch (_: Exception) { null }
    }

    private fun extractStr(json: String, key: String): String? {
        val pattern = "\"$key\"\\s*:\\s*\"([^\"]+)\"".toRegex()
        return pattern.find(json)?.groupValues?.get(1)
    }

    private fun extractInt(json: String, key: String): Int? {
        val pattern = "\"$key\"\\s*:\\s*(\\d+)".toRegex()
        return pattern.find(json)?.groupValues?.get(1)?.toIntOrNull()
    }
}
