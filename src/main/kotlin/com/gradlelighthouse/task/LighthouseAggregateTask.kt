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
 *
 * This task reads JSON report files produced by [LighthouseTask] from each module.
 * It does NOT access `project.subprojects` during execution, making it compatible
 * with Gradle Configuration Cache and Isolated Projects.
 *
 * Report directories are wired by [com.gradlelighthouse.LighthousePlugin] using
 * [ConfigurableFileCollection] during the configuration phase.
 */
abstract class LighthouseAggregateTask @Inject constructor() : DefaultTask() {

    init {
        group = "Gradle Lighthouse"
        description = "Aggregates health reports from all sub-modules into a layered, gamified dashboard."
    }

    /** Report directories from each subproject's lighthouseAudit task */
    @get:InputFiles
    abstract val moduleReportDirs: ConfigurableFileCollection

    @get:Input
    abstract val gradleVersionStr: Property<String>

    @get:OutputDirectory
    abstract val reportOutputDir: DirectoryProperty

    @TaskAction
    fun aggregate() {
        ConsoleLogger.section("📊", "[AGG]", "Aggregating Gradle Lighthouse Global Intelligence (V${LighthouseTask.PLUGIN_VERSION})...")

        val moduleReports = mutableListOf<ModuleReportData>()

        // Read JSON reports from each module's report directory
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

    // =========================================================================
    // Report generation
    // =========================================================================

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

        val groupedReports = reports.groupBy { it.layer }.toSortedMap()

        val moduleTilesHtml = buildString {
            groupedReports.forEach { (layerName, layerReports) ->
                append("""<div class="layer-section" style="margin-bottom: 40px;">""")
                append("""<h2 class="layer-title" style="font-size: 1.4rem; font-weight: 800; color: var(--text); border-bottom: 2px solid var(--border); padding-bottom: 10px; margin-bottom: 20px;">$layerName Layer <span style="font-size: 0.9rem; color: var(--text-dim); font-weight: 600;">(${layerReports.size} modules)</span></h2>""")
                append("""<div class="health-grid">""")
                layerReports.sortedBy { it.score }.forEach { report ->
                    val color = HealthScoreEngine.scoreColor(report.score)
                    val rank = HealthScoreEngine.ArchitectRank.fromScore(report.score)
                    append("""
                        <div class="module-tile" style="border-top: 4px solid $color">
                            <div class="module-name">${esc(report.projectPath)}</div>
                            <div class="module-score" style="color: $color">${report.score}%</div>
                            <div class="module-rank">${rank.displayName}</div>
                            <div class="module-hint">${esc(report.topResolution)}</div>
                            <div class="module-stats">
                                <span class="stat-fatal">${report.fatalCount}F</span>
                                <span class="stat-error">${report.errorCount}E</span>
                                <span class="stat-warn">${report.warningCount}W</span>
                            </div>
                            <a href="${esc(report.moduleName)}-index.html" class="view-link">View Report &rarr;</a>
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
                <div class="critical-item">
                    <div class="critical-header">
                        <span class="critical-name">${esc(report.projectPath)}</span>
                        <span class="critical-score" style="color: ${HealthScoreEngine.scoreColor(report.score)}">${report.score}%</span>
                    </div>
                    <div class="critical-hint">${esc(report.topResolution)}</div>
                </div>
                """.trimIndent()
            }
            
        val scoreEnginePanel = """
            <div class="sidebar-card" style="background: var(--card); border-color: var(--border); margin-top: 30px;">
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

    // =========================================================================
    // Dashboard HTML
    // =========================================================================

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
            --shadow: 0 4px 6px -1px rgb(0 0 0 / 0.1), 0 2px 4px -2px rgb(0 0 0 / 0.1);
            --shadow-lg: 0 10px 15px -3px rgb(0 0 0 / 0.1);
        }
        @media (prefers-color-scheme: dark) {
            :root {
                --bg: #0f172a; --card: #1e293b; --border: #334155;
                --text: #f1f5f9; --text-dim: #94a3b8;
                --shadow: 0 4px 6px -1px rgb(0 0 0 / 0.3);
                --shadow-lg: 0 10px 15px -3px rgb(0 0 0 / 0.4);
            }
        }
        * { box-sizing: border-box; margin: 0; padding: 0; }
        body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; background: var(--bg); color: var(--text); line-height: 1.5; }
        .container { max-width: 1440px; margin: 0 auto; padding: 60px 40px; }

        header { display: flex; justify-content: space-between; align-items: flex-end; margin-bottom: 60px; flex-wrap: wrap; gap: 20px; }
        h1 { margin: 0; font-weight: 900; font-size: 3rem; color: var(--accent); }
        .env-badge { display: inline-flex; align-items: center; background: rgba(255,152,0,0.1); color: var(--accent); padding: 6px 14px; border-radius: 100px; font-size: 0.8rem; font-weight: 700; border: 1px solid rgba(255,152,0,0.2); }

        .elite-stats { display: grid; grid-template-columns: repeat(4, 1fr); gap: 20px; margin-bottom: 50px; }
        .stat-card { background: var(--card); border: 1px solid var(--border); padding: 30px; border-radius: 24px; box-shadow: var(--shadow); }
        .stat-val { font-size: 2.5rem; font-weight: 800; }
        .stat-label { font-size: 0.75rem; text-transform: uppercase; color: var(--text-dim); font-weight: 800; letter-spacing: 0.05em; margin-top: 5px; }

        .main-grid { display: grid; grid-template-columns: 2.5fr 1fr; gap: 40px; }
        .health-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(260px, 1fr)); gap: 25px; }

        .module-tile { background: var(--card); border: 1px solid var(--border); border-radius: 20px; padding: 25px; transition: all 0.2s; box-shadow: var(--shadow); }
        .module-tile:hover { transform: translateY(-4px); box-shadow: var(--shadow-lg); border-color: var(--accent); }
        .module-name { font-weight: 800; font-size: 1.15rem; margin-bottom: 8px; }
        .module-score { font-size: 2rem; font-weight: 900; }
        .module-rank { font-size: 0.7rem; text-transform: uppercase; color: var(--text-dim); font-weight: 800; margin-bottom: 12px; }
        .module-hint { font-size: 0.8rem; color: var(--accent); margin: 12px 0; line-height: 1.4; background: rgba(255,152,0,0.08); padding: 10px; border-radius: 10px; border-left: 3px solid var(--accent); }
        .module-stats { display: flex; gap: 8px; margin-bottom: 12px; }
        .stat-fatal, .stat-error, .stat-warn { padding: 2px 8px; border-radius: 6px; font-size: 0.7rem; font-weight: 800; }
        .stat-fatal { background: rgba(220,38,38,0.1); color: var(--danger); }
        .stat-error { background: rgba(239,68,68,0.1); color: #ef4444; }
        .stat-warn { background: rgba(245,158,11,0.1); color: var(--warning); }
        .view-link { text-decoration: none; font-weight: 800; font-size: 0.8rem; color: var(--accent); text-transform: uppercase; letter-spacing: 0.02em; }

        .sidebar-card { background: var(--card); border: 1px solid var(--border); padding: 30px; border-radius: 24px; box-shadow: var(--shadow); margin-bottom: 30px; }
        .critical-item { margin-bottom: 20px; padding-bottom: 20px; border-bottom: 1px solid var(--border); }
        .critical-item:last-child { border-bottom: none; margin-bottom: 0; padding-bottom: 0; }
        .critical-header { display: flex; justify-content: space-between; margin-bottom: 6px; }
        .critical-name { font-weight: 800; font-size: 1rem; }
        .critical-score { font-weight: 900; font-size: 1rem; }
        .critical-hint { font-size: 0.8rem; color: var(--accent); background: rgba(255,152,0,0.08); padding: 8px; border-radius: 8px; }

        footer { margin-top: 120px; text-align: center; color: var(--text-dim); font-size: 0.8rem; padding: 40px 0; border-top: 1px solid var(--border); }

        @media (max-width: 1024px) {
            .main-grid { grid-template-columns: 1fr; }
            .elite-stats { grid-template-columns: repeat(2, 1fr); }
        }
        @media (max-width: 600px) {
            .container { padding: 20px 16px; }
            h1 { font-size: 2rem; }
            .elite-stats { grid-template-columns: 1fr; }
        }
        @media print {
            body { background: #fff; color: #000; }
            .module-tile:hover { transform: none; }
            .stat-card, .module-tile, .sidebar-card { box-shadow: none; border: 1px solid #ccc; page-break-inside: avoid; }
        }
    </style>
</head>
<body>
    <div class="container">
        <header>
            <div>
                <h1>Gradle Lighthouse</h1>
                <div style="display: flex; gap: 15px; margin-top:15px; align-items: center; flex-wrap: wrap;">
                    <span class="env-badge">V${LighthouseTask.PLUGIN_VERSION}</span>
                    <span style="color: var(--text-dim); font-size: 0.95rem; font-weight: 600;">Global Intelligence Dashboard</span>
                    <span style="color: var(--border);">|</span>
                    <span style="color: var(--text-dim); font-size: 0.95rem; font-weight: 600;">Gradle $gradleVersion</span>
                </div>
            </div>
            <div style="text-align: right">
                <div class="stat-label">Project Maturity</div>
                <div style="font-size: 1.8rem; font-weight: 900; color: var(--accent)">${globalRank.displayName}</div>
            </div>
        </header>

        <div class="elite-stats">
            <div class="stat-card"><div class="stat-val" style="color: $scoreColor">$avgScore%</div><div class="stat-label">Project Health Score</div></div>
            <div class="stat-card"><div class="stat-val">$auditedCount</div><div class="stat-label">Audited Modules</div></div>
            <div class="stat-card"><div class="stat-val" style="color: var(--danger)">$totalFatal</div><div class="stat-label">Fatal Issues</div></div>
            <div class="stat-card"><div class="stat-val" style="color: #ef4444">$totalErrors</div><div class="stat-label">Errors</div></div>
        </div>

        <div class="main-grid">
            <div>
                <div style="font-weight: 800; font-size: 1.3rem; margin-bottom: 25px; color: var(--text);">Module Health Map</div>
                $moduleTilesHtml
            </div>
            <aside>
                <div class="sidebar-card" style="background: rgba(220,38,38,0.03); border-color: rgba(220,38,38,0.15);">
                    <div class="stat-label" style="color: var(--danger); margin-bottom: 20px;">Critical Risk Backlog</div>
                    ${if (criticalBacklog.isBlank()) "<div style='color: var(--success); font-weight: 600;'>No critical risks. Excellent.</div>" else criticalBacklog}
                </div>
                $scoreEnginePanel
            </aside>
        </div>

        <footer>
            &copy; 2026 Gradle Lighthouse &bull; Architectural Intelligence Protocol V${LighthouseTask.PLUGIN_VERSION}
        </footer>
    </div>
</body>
</html>
    """.trimIndent()

    // =========================================================================
    // JSON Parsing (robust, not substring-based)
    // =========================================================================

    private fun parseReportJson(json: String): ModuleReportData? {
        return try {
            val moduleName = extractJsonString(json, "module") ?: return null
            val projectPath = extractJsonString(json, "path") ?: ""
            val score = extractJsonInt(json, "score") ?: 0
            val rank = extractJsonString(json, "rank") ?: "Unknown"
            val fatalCount = extractJsonInt(json, "fatalCount") ?: 0
            val errorCount = extractJsonInt(json, "errorCount") ?: 0
            val warningCount = extractJsonInt(json, "warningCount") ?: 0
            val topResolution = extractJsonString(json, "topResolution") ?: "Run audit for details."

            ModuleReportData(moduleName, projectPath, score, rank, fatalCount, errorCount, warningCount, topResolution)
        } catch (_: Exception) {
            null
        }
    }

    /** Extract a string value from JSON by key. Simple but handles escaped quotes. */
    private fun extractJsonString(json: String, key: String): String? {
        val pattern = "\"$key\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\"".toRegex()
        return pattern.find(json)?.groupValues?.get(1)?.replace("\\\"", "\"")?.replace("\\\\", "\\")
    }

    /** Extract an integer value from JSON by key. */
    private fun extractJsonInt(json: String, key: String): Int? {
        val pattern = "\"$key\"\\s*:\\s*(\\d+)".toRegex()
        return pattern.find(json)?.groupValues?.get(1)?.toIntOrNull()
    }

    /** HTML-escape to prevent XSS */
    private fun esc(text: String): String = text
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")

    // =========================================================================
    // Data classes
    // =========================================================================

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
            return if (segments.isNotEmpty()) {
                val rawType = segments.first()
                if (rawType.isNotEmpty()) rawType.substring(0, 1).toUpperCase() + rawType.substring(1) else rawType
            } else "Root"
        }
    }
}
