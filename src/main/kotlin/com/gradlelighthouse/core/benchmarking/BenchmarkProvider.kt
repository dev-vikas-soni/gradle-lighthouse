package com.gradlelighthouse.core.benchmarking

import com.gradlelighthouse.core.LighthouseCategory
import com.gradlelighthouse.core.ConsoleLogger
import java.io.File
import java.net.URL

interface BenchmarkProvider {
    fun loadBenchmarks(): List<BenchmarkSnapshot>
}

/**
 * Loads benchmarks from the plugin's embedded resources.
 */
class EmbeddedBenchmarkProvider : BenchmarkProvider {
    override fun loadBenchmarks(): List<BenchmarkSnapshot> {
        val snapshots = mutableListOf<BenchmarkSnapshot>()
        val benchmarkNames = listOf("signal-android", "now-in-android")

        benchmarkNames.forEach { name ->
            val resource: URL? = javaClass.classLoader.getResource("benchmarks/$name.json")
            if (resource != null) {
                try {
                    val content = resource.readText()
                    parseSnapshot(content)?.let { snapshots.add(it) }
                } catch (e: Exception) {
                    ConsoleLogger.error("Failed to load embedded benchmark $name: ${e.message}")
                }
            }
        }
        return snapshots
    }
}

/**
 * Loads benchmarks from a specific directory on disk.
 */
class JsonBenchmarkProvider(private val directory: File) : BenchmarkProvider {
    fun getDirectoryPath(): String = directory.absolutePath

    override fun loadBenchmarks(): List<BenchmarkSnapshot> {
        if (!directory.exists() || !directory.isDirectory) return emptyList()

        return directory.listFiles { file -> file.extension == "json" }
            ?.mapNotNull { file ->
                try {
                    parseSnapshot(file.readText())
                } catch (e: Exception) {
                    ConsoleLogger.error("Failed to parse benchmark file ${file.name}: ${e.message}")
                    null
                }
            } ?: emptyList()
    }
}

/**
 * Shared simple JSON parser for BenchmarkSnapshot.
 * Avoids adding a heavy JSON library dependency like Jackson or Gson to the plugin classpath.
 */
fun parseSnapshot(json: String): BenchmarkSnapshot? {
    return try {
        val name = extractStr(json, "projectName") ?: return null
        val version = extractStr(json, "lighthouseVersion") ?: "2.3.0"
        val score = extractDouble(json, "overallScore") ?: 0.0
        val modules = extractInt(json, "moduleCount") ?: 1
        val maturity = extractStr(json, "projectMaturity") ?: "Standard"
        val date = extractStr(json, "generatedAt") ?: ""
        val personaStr = extractStr(json, "persona") ?: "MEDIUM_ANDROID_APP"
        val persona = try { ProjectPersona.valueOf(personaStr) } catch(_: Exception) { ProjectPersona.MEDIUM_ANDROID_APP }

        // Category scores parsing
        val catMap = mutableMapOf<LighthouseCategory, Double>()
        LighthouseCategory.entries.forEach { cat ->
            extractDouble(json, cat.name)?.let { catMap[cat] = it }
        }

        BenchmarkSnapshot(
            projectName = name,
            generatedAt = date,
            lighthouseVersion = version,
            overallScore = score,
            projectMaturity = maturity,
            moduleCount = modules,
            categoryScores = catMap,
            persona = persona
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

private fun extractDouble(json: String, key: String): Double? {
    val pattern = "\"$key\"\\s*:\\s*([\\d.]+)".toRegex()
    return pattern.find(json)?.groupValues?.get(1)?.toDoubleOrNull()
}
