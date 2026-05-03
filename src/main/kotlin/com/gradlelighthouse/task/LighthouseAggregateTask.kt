package com.gradlelighthouse.task

import com.gradlelighthouse.core.ConsoleLogger
import com.gradlelighthouse.core.HealthScoreEngine
import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import java.io.File
import javax.inject.Inject

/**
 * Aggregates health reports from all sub-modules into a single Global Dashboard.
 */
abstract class LighthouseAggregateTask @Inject constructor() : DefaultTask() {

    init {
        group = "Gradle Lighthouse"
        description = "Aggregates health reports from all sub-modules into a grouped dashboard."
    }

    @get:InputFiles
    abstract val moduleReportDirs: ConfigurableFileCollection

    @get:Input
    abstract val gradleVersionStr: Property<String>

    @get:Input
    abstract val pluginVersion: Property<String>

    @get:OutputDirectory
    abstract val reportOutputDir: DirectoryProperty

    @TaskAction
    fun aggregate() {
        ConsoleLogger.section("📊", "[AGG]", "Aggregating Gradle Lighthouse Global Intelligence (V${pluginVersion.get()})...")

        val moduleReports = mutableListOf<ModuleReportData>()

        moduleReportDirs.files.forEach { dir ->
            val reportFile = File(dir, "module-report.json")
            if (reportFile.exists()) {
                val data = parseReportJson(reportFile.readText())
                if (data != null) moduleReports.add(data)
            }
        }

        if (moduleReports.isEmpty()) {
            ConsoleLogger.warn("No module reports found. Run `./gradlew lighthouseAudit` first.")
            return
        }

        generateGlobalReport(moduleReports)
    }

    private fun generateGlobalReport(reports: List<ModuleReportData>) {
        val outputDir = reportOutputDir.get().asFile
        if (!outputDir.exists()) outputDir.mkdirs()

        val dashboardFile = File(outputDir, "project-dashboard.html")
        val avgScore = reports.map { it.score }.average().toInt().coerceAtLeast(0)
        val totalFatal = reports.sumOf { it.fatalCount }
        val totalErrors = reports.sumOf { it.errorCount }
        val auditedCount = reports.size
        val globalRank = HealthScoreEngine.ArchitectRank.fromScore(avgScore)
        val scoreColor = HealthScoreEngine.scoreColor(avgScore)

        // Grouping logic: identifies the architectural layer (app, core, data, domain, etc.)
        val groupedReports = reports.groupBy { it.layer }.toSortedMap()

        val moduleTilesHtml = buildString {
            groupedReports.forEach { (layerName, layerReports) ->
                append("""<div class="layer-section" style="margin-bottom: 40px; width: 100%;">""")
                append("""<h2 class="layer-title" style="font-size: 1.5rem; font-weight: 800; color: var(--text); border-bottom: 2px solid var(--border); padding-bottom: 10px; margin-bottom: 20px; text-transform: capitalize;">$layerName</h2>""")
                append("""<div class="health-grid" style="display: grid; grid-template-columns: repeat(auto-fill, minmax(280px, 1fr)); gap: 20px;">""")
                layerReports.sortedBy { it.score }.forEach { report ->
                    val color = HealthScoreEngine.scoreColor(report.score)
                    append("""
                        <div class="module-tile" style="border-top: 4px solid $color; background: var(--card); border: 1px solid var(--border); border-radius: 16px; padding: 20px; box-shadow: var(--shadow);">
                            <div class="module-name" style="font-weight: 800; font-size: 1.1rem; margin-bottom: 5px;">${esc(report.projectPath)}</div>
                            <div class="module-score" style="font-size: 1.8rem; font-weight: 900; color: $color;">${report.score}%</div>
                            <div class="module-rank" style="font-size: 0.75rem; text-transform: uppercase; color: var(--text-dim); font-weight: 700; margin-bottom: 10px;">${report.rank}</div>
                            <div class="module-stats" style="display: flex; gap: 8px; margin-bottom: 15px;">
                                <span class="stat-fatal" style="background: rgba(220,38,38,0.1); color: #dc2626; padding: 2px 8px; border-radius: 6px; font-size: 0.7rem; font-weight: 800;">${report.fatalCount}F</span>
                                <span class="stat-error" style="background: rgba(239,68,68,0.1); color: #ef4444; padding: 2px 8px; border-radius: 6px; font-size: 0.7rem; font-weight: 800;">${report.errorCount}E</span>
                                <span class="stat-warn" style="background: rgba(245,158,11,0.1); color: #f59e0b; padding: 2px 8px; border-radius: 6px; font-size: 0.7rem; font-weight: 800;">${report.warningCount}W</span>
                            </div>
                            <a href="${esc(report.moduleName)}-index.html" style="text-decoration: none; font-weight: 800; font-size: 0.8rem; color: var(--accent); text-transform: uppercase;">View Report &rarr;</a>
                        </div>
                    """.trimIndent())
                }
                append("</div></div>")
            }
        }

        val criticalBacklog = reports.filter { it.fatalCount > 0 || it.score < 70 }
            .sortedBy { it.score }
            .take(5)
            .joinToString("") { report ->
                """
                <div class="critical-item" style="margin-bottom: 15px; padding-bottom: 15px; border-bottom: 1px solid var(--border);">
                    <div class="critical-header" style="display: flex; justify-content: space-between;">
                        <span class="critical-name" style="font-weight: 800;">${esc(report.projectPath)}</span>
                        <span class="critical-score" style="font-weight: 900; color: ${HealthScoreEngine.scoreColor(report.score)}">${report.score}%</span>
                    </div>
                </div>
                """.trimIndent()
            }

        val scoreEnginePanel = """
            <div class="sidebar-card" style="background: var(--card); border: 1px solid var(--border); padding: 30px; border-radius: 24px; box-shadow: var(--shadow); margin-top: 30px;">
                <div class="stat-label" style="color: var(--text); margin-bottom: 15px;">Scoring Engine</div>
                <div style="font-size: 0.85rem; color: var(--text-dim); line-height: 1.6;">
                    Health Score uses an exponential decay model to prioritize critical fixes without demoralizing teams:
                    <div style="background: rgba(0,0,0,0.05); padding: 10px; border-radius: 8px; font-family: monospace; font-size: 0.8rem; margin: 10px 0; color: var(--text);">
                        Score = 100 × 0.98<sup>(Total Impact)</sup>
                    </div>
                    <b>Impact Weights:</b>
                    <ul style="margin-left: 20px; margin-top: 5px;">
                        <li><span style="color: var(--danger)">Fatal: 35.0</span></li>
                        <li><span style="color: #ef4444">Error: 15.0</span></li>
                        <li><span style="color: var(--warning)">Warning: 5.0</span></li>
                        <li><span style="color: var(--info)">Info: 1.0</span></li>
                    </ul>
                </div>
            </div>
        """.trimIndent()

        val htmlContent = buildDashboardHtml(
            avgScore = avgScore,
            scoreColor = scoreColor,
            globalRank = globalRank,
            auditedCount = auditedCount,
            totalFatal = totalFatal,
            totalErrors = totalErrors,
            moduleTilesHtml = moduleTilesHtml,
            criticalBacklog = criticalBacklog,
            scoreEnginePanel = scoreEnginePanel,
            gradleVersion = gradleVersionStr.get()
        )

        dashboardFile.writeText(htmlContent)
        ConsoleLogger.info("🏆", "[DASH]", "Global Dashboard: ${dashboardFile.toURI()}")
    }

    private fun buildDashboardHtml(
        avgScore: Int,
        scoreColor: String,
        globalRank: HealthScoreEngine.ArchitectRank,
        auditedCount: Int,
        totalFatal: Int,
        totalErrors: Int,
        moduleTilesHtml: String,
        criticalBacklog: String,
        scoreEnginePanel: String,
        gradleVersion: String
    ): String = """
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Gradle Lighthouse - Global Dashboard</title>
    <style>
        :root {
            --bg: #f8fafc; --card: #ffffff; --border: #e2e8f0;
            --text: #0f172a; --text-dim: #64748b;
            --accent: #FF9800; --success: #10b981; --warning: #f59e0b; --danger: #dc2626;
            --shadow: 0 4px 6px -1px rgb(0 0 0 / 0.1);
        }
        @media (prefers-color-scheme: dark) {
            :root {
                --bg: #0f172a; --card: #1e293b; --border: #334155;
                --text: #f1f5f9; --text-dim: #94a3b8;
            }
        }
        * { box-sizing: border-box; margin: 0; padding: 0; }
        body { font-family: -apple-system, system-ui, sans-serif; background: var(--bg); color: var(--text); line-height: 1.5; }
        .container { max-width: 1400px; margin: 0 auto; padding: 60px 40px; }
        header { display: flex; justify-content: space-between; align-items: flex-end; margin-bottom: 60px; }
        h1 { font-weight: 900; font-size: 3rem; color: var(--accent); }
        .stat-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 20px; margin-bottom: 50px; }
        .stat-card { background: var(--card); border: 1px solid var(--border); padding: 30px; border-radius: 24px; box-shadow: var(--shadow); }
        .stat-val { font-size: 2.5rem; font-weight: 800; }
        .stat-label { font-size: 0.75rem; text-transform: uppercase; color: var(--text-dim); font-weight: 800; margin-top: 5px; }
        .main-grid { display: grid; grid-template-columns: 3fr 1fr; gap: 40px; }
        .sidebar-card { background: var(--card); border: 1px solid var(--border); padding: 30px; border-radius: 24px; box-shadow: var(--shadow); }
        footer { margin-top: 100px; text-align: center; color: var(--text-dim); font-size: 0.8rem; padding: 40px 0; border-top: 1px solid var(--border); }
    </style>
</head>
<body>
    <div class="container">
        <header>
            <div>
                <h1>Gradle Lighthouse</h1>
                <div style="margin-top:10px; color: var(--text-dim); font-weight: 600;">Global Dashboard &bull; Gradle $gradleVersion &bull; V${pluginVersion.get()}</div>
            </div>
            <div style="text-align: right">
                <div class="stat-label">Project Maturity</div>
                <div style="font-size: 1.8rem; font-weight: 900; color: var(--accent)">${globalRank.displayName}</div>
            </div>
        </header>

        <div class="stat-grid">
            <div class="stat-card"><div class="stat-val" style="color: $scoreColor">$avgScore%</div><div class="stat-label">Health Score</div></div>
            <div class="stat-card"><div class="stat-val">$auditedCount</div><div class="stat-label">Modules</div></div>
            <div class="stat-card"><div class="stat-val" style="color: var(--danger)">$totalFatal</div><div class="stat-label">Fatals</div></div>
            <div class="stat-card"><div class="stat-val" style="color: #ef4444">$totalErrors</div><div class="stat-label">Errors</div></div>
        </div>

        <div class="main-grid">
            <div>$moduleTilesHtml</div>
            <aside>
                <div class="sidebar-card">
                    <div class="stat-label" style="color: var(--danger); margin-bottom: 20px;">Critical Risks</div>
                    $criticalBacklog
                </div>
                $scoreEnginePanel
            </aside>
        </div>

        <footer>
            &copy; 2026 Gradle Lighthouse Protocol V${pluginVersion.get()}
        </footer>
    </div>
</body>
</html>
    """.trimIndent()

    private fun parseReportJson(json: String): ModuleReportData? {
        return try {
            val moduleName = extractJsonString(json, "module") ?: return null
            val projectPath = extractJsonString(json, "path") ?: ""
            val score = extractJsonInt(json, "score") ?: 0
            val rank = extractJsonString(json, "rank") ?: "Unknown"
            val fatalCount = extractJsonInt(json, "fatalCount") ?: 0
            val errorCount = extractJsonInt(json, "errorCount") ?: 0
            val warningCount = extractJsonInt(json, "warningCount") ?: 0
            val topResolution = extractJsonString(json, "topResolution") ?: ""

            ModuleReportData(moduleName, projectPath, score, rank, fatalCount, errorCount, warningCount, topResolution)
        } catch (_: Exception) {
            null
        }
    }

    private fun extractJsonString(json: String, key: String): String? {
        val pattern = "\"$key\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\"".toRegex()
        return pattern.find(json)?.groupValues?.get(1)?.replace("\\\"", "\"")?.replace("\\\\", "\\")
    }

    private fun extractJsonInt(json: String, key: String): Int? {
        val pattern = "\"$key\"\\s*:\\s*(\\d+)".toRegex()
        return pattern.find(json)?.groupValues?.get(1)?.toIntOrNull()
    }

    private fun esc(text: String): String = text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;")

    data class ModuleReportData(
        val moduleName: String,
        val projectPath: String,
        val score: Int,
        val rank: String,
        val fatalCount: Int,
        val errorCount: Int,
        val warningCount: Int,
        val topResolution: String
    ) {
        val layer: String get() {
            if (projectPath.isBlank() || projectPath == ":") return "Root"
            val segments = projectPath.split(":").filter { it.isNotEmpty() }
            return if (segments.isNotEmpty()) segments.first() else "Root"
        }
    }
}
