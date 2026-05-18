package com.gradlelighthouse.task

import com.gradlelighthouse.core.ConsoleLogger
import com.gradlelighthouse.core.HealthScoreEngine
import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.gradle.work.DisableCachingByDefault
import java.io.File
import javax.inject.Inject

/**
 * Aggregates health reports from all sub-modules into a single Global Dashboard.
 */
@DisableCachingByDefault(because = "Aggregation should always reflect the latest state of all modules.")
abstract class LighthouseAggregateTask @Inject constructor() : DefaultTask() {

    init {
        group = "Gradle Lighthouse"
        description = "Aggregates health reports from all sub-modules into a grouped dashboard."
    }

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val moduleReportDirs: ConfigurableFileCollection

    @get:Input
    abstract val moduleDependencyGraphData: org.gradle.api.provider.ListProperty<String>

    @get:Input
    abstract val gradleVersionStr: Property<String>

    @get:Input
    abstract val pluginVersion: Property<String>

    @get:Input
    abstract val failOnDependencyCycle: Property<Boolean>

    @get:Input
    abstract val failOnLayerViolation: Property<Boolean>

    @get:Input
    abstract val minHealthScore: Property<Int>

    @get:Input
    abstract val rootDirPath: Property<String>

    @get:OutputDirectory
    abstract val reportOutputDir: DirectoryProperty

    @TaskAction
    fun aggregate() {
        ConsoleLogger.section("📊", "[AGG]", "Aggregating Gradle Lighthouse Global Intelligence (V${pluginVersion.get()})...")

        val moduleReports = mutableListOf<ModuleReportData>()
        val outputDir = reportOutputDir.get().asFile
        if (!outputDir.exists()) outputDir.mkdirs()

        moduleReportDirs.files.forEach { dir ->
            val reportFile = File(dir, "module-report.json")
            if (reportFile.exists()) {
                val data = parseReportJson(reportFile.readText())
                if (data != null) {
                    moduleReports.add(data)

                    // Expert navigation fix: Use sanitized path for unique subdirectories
                    // This prevents collisions if modules have same name (e.g. :feature:ui and :core:ui)
                    val safeFolderName = data.projectPath.replace(":", "-").trim('-').ifBlank { "root" }
                    val moduleHtmlName = "${data.moduleName}-index.html"
                    val sourceHtml = File(dir, moduleHtmlName)

                    if (sourceHtml.exists()) {
                        val targetSubDir = File(outputDir, safeFolderName)
                        targetSubDir.mkdirs()
                        sourceHtml.copyTo(File(targetSubDir, moduleHtmlName), overwrite = true)

                        // Also store the relative path for the link
                        data.relativeReportPath = "$safeFolderName/$moduleHtmlName"
                    }
                }
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

        // Parse the module dependency graph once — reused for coupling density,
        // enforcement gates, and the galaxy graph JSON below.
        val moduleGraph = mutableMapOf<String, List<String>>()
        moduleDependencyGraphData.get().forEach { line ->
            val parts = line.split("|")
            if (parts.size == 2) {
                val module = parts[0]
                val deps = parts[1].split(",").filter { it.isNotBlank() }
                moduleGraph[module] = deps
            }
        }

        // Compute coupling density and save historical trend
        val tempTotalModules = moduleGraph.size.toDouble().coerceAtLeast(1.0)
        val tempTotalLinks = moduleGraph.values.sumOf { it.size }.toDouble()
        val couplingDensity = Math.round((tempTotalLinks / tempTotalModules) * 100.0) / 100.0
        saveGlobalHistory(avgScore, totalFatal, couplingDensity)

        // Grouping logic: identifies the architectural layer (app, core, data, domain, feature, etc.)
        val groupedReports = reports.groupBy { it.layer }.toSortedMap()

        val moduleTilesHtml = buildString {
            groupedReports.forEach { (layerName, layerReports) ->
                append("""<div class="layer-section" style="margin-bottom: 50px; width: 100%;">""")
                append("""<h2 class="layer-title" style="font-size: 1.6rem; font-weight: 800; color: var(--text); border-bottom: 3px solid var(--accent); padding-bottom: 8px; margin-bottom: 25px; text-transform: uppercase; letter-spacing: 0.05em;">$layerName Layer</h2>""")
                append("""<div class="health-grid" style="display: grid; grid-template-columns: repeat(auto-fill, minmax(300px, 1fr)); gap: 24px;">""")

                // Sort modules by path within their layer
                layerReports.sortedBy { it.projectPath }.forEach { report ->
                    val color = HealthScoreEngine.scoreColor(report.score)
                    append("""
                        <div class="module-tile" style="border-top: 4px solid $color; background: var(--card); border: 1px solid var(--border); border-radius: 16px; padding: 24px; box-shadow: var(--shadow); transition: transform 0.2s;">
                            <div class="module-name" style="font-weight: 800; font-size: 1.15rem; margin-bottom: 6px; color: var(--text);">${esc(report.projectPath)}</div>
                            <div style="display: flex; justify-content: space-between; align-items: flex-end; margin-bottom: 15px;">
                                <div>
                                    <div class="module-score" style="font-size: 2rem; font-weight: 900; color: $color; line-height: 1;">${report.score}%</div>
                                    <div class="module-rank" style="font-size: 0.75rem; text-transform: uppercase; color: var(--text-dim); font-weight: 700; margin-top: 4px;">${report.rank}</div>
                                </div>
                                <div class="module-stats" style="display: flex; gap: 6px;">
                                    <span class="stat-fatal" style="background: rgba(220,38,38,0.1); color: #dc2626; padding: 2px 8px; border-radius: 6px; font-size: 0.7rem; font-weight: 800;">${report.fatalCount}F</span>
                                    <span class="stat-error" style="background: rgba(239,68,68,0.1); color: #ef4444; padding: 2px 8px; border-radius: 6px; font-size: 0.7rem; font-weight: 800;">${report.errorCount}E</span>
                                    <span class="stat-warn" style="background: rgba(245,158,11,0.1); color: #f59e0b; padding: 2px 8px; border-radius: 6px; font-size: 0.7rem; font-weight: 800;">${report.warningCount}W</span>
                                </div>
                            </div>
                            <a href="${esc(report.relativeReportPath)}" style="display: inline-block; text-decoration: none; font-weight: 800; font-size: 0.8rem; color: var(--accent); text-transform: uppercase; border: 1px solid var(--accent); padding: 8px 16px; border-radius: 8px; transition: all 0.2s;">View Full Audit &rarr;</a>
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
                <div class="stat-label" style="color: var(--text); margin-bottom: 15px; font-weight: 800; font-size: 0.8rem; text-transform: uppercase;">Scoring Engine</div>
                <div style="font-size: 0.85rem; color: var(--text-dim); line-height: 1.6;">
                    Health Score uses an exponential decay model to prioritize critical fixes without demoralizing teams:
                    <div style="background: rgba(0,0,0,0.05); padding: 12px; border-radius: 10px; font-family: 'SF Mono', monospace; font-size: 0.85rem; margin: 12px 0; color: var(--text); border: 1px solid var(--border);">
                        Score = 100 × 0.98<sup>(Total Impact)</sup>
                    </div>
                    <b>Impact Weights:</b>
                    <ul style="margin-left: 20px; margin-top: 8px; list-style-type: none;">
                        <li><span style="color: var(--danger); font-weight: 700;">Fatal: 35.0</span></li>
                        <li><span style="color: #ef4444; font-weight: 700;">Error: 15.0</span></li>
                        <li><span style="color: var(--warning); font-weight: 700;">Warning: 5.0</span></li>
                        <li><span style="color: var(--info); font-weight: 700;">Info: 1.0</span></li>
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
            gradleVersion = gradleVersionStr.get(),
            reports = reports,
            moduleDependencyGraphData = moduleDependencyGraphData,
            globalHistoryJson = getGlobalHistoryJson()
        )

        dashboardFile.writeText(htmlContent)
        ConsoleLogger.info("🏆", "[DASH]", "Global Dashboard: ${dashboardFile.toURI()}")

        // -----------------------------------------------------------------------------
        // Enterprise CI/CD Strict Enforcement Gates
        // (uses moduleGraph parsed above — no second traversal needed)
        // -----------------------------------------------------------------------------
        val graph = moduleGraph

        val pathToLayer = reports.associate { it.projectPath to it.layer }
        val layerOrder = mapOf(
            "Root" to 0,
            "App" to 1,
            "Feature" to 2,
            "Domain" to 3,
            "Data" to 4,
            "Core" to 5,
            "Shared" to 6
        )

        val layerViolations = mutableListOf<String>()
        if (failOnLayerViolation.get()) {
            graph.forEach { (src, targets) ->
                val srcLayer = pathToLayer[src] ?: return@forEach
                val srcOrder = layerOrder[srcLayer] ?: 99

                targets.forEach { tgt ->
                    val tgtLayer = pathToLayer[tgt] ?: return@forEach
                    val tgtOrder = layerOrder[tgtLayer] ?: 99

                    if (src != tgt && srcOrder > tgtOrder) {
                        layerViolations.add("🚨 Layer Boundary Leak: $src ($srcLayer) depends on $tgt ($tgtLayer)")
                    }
                }
            }
        }

        val cycles = mutableListOf<List<String>>()
        if (failOnDependencyCycle.get()) {
            // Iterative DFS using explicit colour marking to correctly handle dense graphs
            // where a subgraph is reachable via multiple entry paths.
            //
            // Three colours per node (standard algorithm from CLRS):
            //   WHITE (0) – not yet visited
            //   GRAY  (1) – currently on the recursion stack (active DFS path)
            //   BLACK (2) – fully explored; all descendants have been visited
            //
            // A back-edge WHITE→GRAY means we found a cycle.  Back-edges into BLACK nodes
            // are cross/forward edges and do NOT represent new cycles — ignoring them
            // prevents the double-reporting that occurs when the same subgraph is reached
            // from two different entry points.
            //
            // The deduplication guard (sorted normalised key) is kept as a second safety net.
            val WHITE = 0; val GRAY = 1; val BLACK = 2
            val color = mutableMapOf<String, Int>().withDefault { WHITE }
            // Explicit stack frame: Pair(node, iterator-over-its-children)
            // Using an iterative approach avoids JVM stack-overflow on very large graphs.
            graph.keys.forEach { start ->
                if (color.getValue(start) != WHITE) return@forEach

                // path tracks the current DFS ancestor chain for cycle extraction
                val path  = ArrayDeque<String>()
                // frames: each frame is (node, remaining-children-iterator)
                val frames = ArrayDeque<Pair<String, Iterator<String>>>()

                color[start] = GRAY
                path.addLast(start)
                frames.addLast(start to (graph[start] ?: emptyList<String>()).iterator())

                while (frames.isNotEmpty()) {
                    val (node, children) = frames.last()
                    var pushed = false
                    while (children.hasNext()) {
                        val dep = children.next()
                        if (dep == node) continue // skip self-loops
                        when (color.getValue(dep)) {
                            GRAY -> {
                                // Back-edge: dep is an ancestor on the current path → cycle found
                                val cycleStart = path.indexOf(dep)
                                val cycleList  = path.subList(cycleStart, path.size).toList() + dep
                                val normalized = cycleList.dropLast(1).sorted().joinToString("->")
                                if (cycles.none { it.dropLast(1).sorted().joinToString("->") == normalized }) {
                                    cycles.add(cycleList)
                                }
                            }
                            WHITE -> {
                                // Tree-edge: descend into unvisited node
                                color[dep] = GRAY
                                path.addLast(dep)
                                frames.addLast(dep to (graph[dep] ?: emptyList<String>()).iterator())
                                pushed = true
                                break
                            }
                            // BLACK: cross/forward edge — not a new cycle, skip
                        }
                    }
                    if (!pushed) {
                        // All children of this node explored — mark BLACK and pop
                        color[node] = BLACK
                        path.removeLast()
                        frames.removeLast()
                    }
                }
            }
        }

        // 4. Custom Architecture Rules (lighthouse-rules.yaml / .yml)
        // Violations are bucketed by the rule's `level` field:
        //   - "fatal"   → added to failureMessages AND triggers an immediate hard stop message
        //   - "error"   → added to failureMessages (build fails)
        //   - "warning" → logged to console but does NOT fail the build
        // Any unrecognised level is treated as "error".
        val customViolations = mutableListOf<String>()          // fatal + error → fail build
        val customWarnings   = mutableListOf<String>()          // warning      → log only
        val customRulesFile    = File(File(rootDirPath.get()), "lighthouse-rules.yaml")
        val customRulesFileYml = File(File(rootDirPath.get()), "lighthouse-rules.yml")
        val finalRulesFile = if (customRulesFile.exists()) customRulesFile else customRulesFileYml

        if (finalRulesFile.exists()) {
            val rules = parseRulesYaml(finalRulesFile)
            rules.forEach { rule ->
                val ruleViolations = if (rule.condition.contains("!->")) {
                    evaluateIsolationRule(rule, graph)
                } else if (rule.condition.contains("->")) {
                    evaluateLayerRule(rule, graph, pathToLayer)
                } else {
                    emptyList()
                }
                if (ruleViolations.isNotEmpty()) {
                    val levelIcon = when (rule.level) {
                        "fatal"   -> "💀"
                        "warning" -> "⚠️ "
                        else      -> "🚨" // error (default)
                    }
                    val formatted = ruleViolations.map { "$levelIcon [${rule.name}] $it" }
                    when (rule.level) {
                        "warning" -> customWarnings.addAll(formatted)
                        else      -> customViolations.addAll(formatted) // "fatal" and "error" both fail
                    }
                }
            }
        }

        // Emit warnings to console (never fail the build)
        if (customWarnings.isNotEmpty()) {
            ConsoleLogger.warn("⚠️  Custom Architecture Rule Warnings: ${customWarnings.size} advisory violation(s) found:")
            customWarnings.forEach { v -> ConsoleLogger.warn("      $v") }
        }

        val failureMessages = mutableListOf<String>()

        // 1. Threshold Gating (Score)
        val minScore = minHealthScore.get()
        if (minScore > 0 && avgScore < minScore) {
            failureMessages.add("   🚨 Global Health Score Gating Failed: Architectural Health Score is $avgScore%, but required minimum is $minScore%.")
        }

        // 2. Cycle Gating
        if (failOnDependencyCycle.get() && cycles.isNotEmpty()) {
            failureMessages.add("   🚨 Modular Cycle Gating Failed: ${cycles.size} circular dependency loops detected:")
            cycles.forEach { cycle ->
                failureMessages.add("      🔗 ${cycle.joinToString(" ➔ ")}")
            }
        }

        // 3. Layer Crossing Gating
        if (failOnLayerViolation.get() && layerViolations.isNotEmpty()) {
            failureMessages.add("   🚨 Architectural Layer Crossing Gating Failed: ${layerViolations.size} illegal leaks detected:")
            layerViolations.forEach { violation ->
                failureMessages.add("      $violation")
            }
        }

        // 4. Custom Rules Gating (only fatal + error levels fail the build)
        if (customViolations.isNotEmpty()) {
            val fatalCount = customViolations.count { it.startsWith("💀") }
            val errorCount = customViolations.count { it.startsWith("🚨") }
            val summary = buildString {
                if (fatalCount > 0) append("$fatalCount fatal")
                if (fatalCount > 0 && errorCount > 0) append(", ")
                if (errorCount > 0) append("$errorCount error")
            }
            failureMessages.add("   🚨 Custom Architecture Rule Gating Failed: $summary violation(s) detected:")
            customViolations.forEach { v -> failureMessages.add("      $v") }
        }

        if (failureMessages.isNotEmpty()) {
            val fullError = buildString {
                appendLine()
                appendLine("════════════════════════════════════════════════════════════")
                appendLine("❌ GRADLE LIGHTHOUSE STRICT ENFORCEMENT FAILURE")
                appendLine("════════════════════════════════════════════════════════════")
                failureMessages.forEach { appendLine(it) }
                appendLine("════════════════════════════════════════════════════════════")
                appendLine("💡 Refactor dependency structure or adjust strict settings in gradle-lighthouse plugin configuration block.")
            }
            throw org.gradle.api.GradleException(fullError)
        }
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
        gradleVersion: String,
        reports: List<ModuleReportData>,
        moduleDependencyGraphData: org.gradle.api.provider.ListProperty<String>,
        globalHistoryJson: String
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
        body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; background: var(--bg); color: var(--text); line-height: 1.5; }
        .container { max-width: 1400px; margin: 0 auto; padding: 60px 40px; }
        header { display: flex; justify-content: space-between; align-items: flex-end; margin-bottom: 60px; }
        h1 { font-weight: 900; font-size: 3.5rem; color: var(--accent); margin: 0; }
        .stat-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 24px; margin-bottom: 60px; }
        .stat-card { background: var(--card); border: 1px solid var(--border); padding: 30px; border-radius: 24px; box-shadow: var(--shadow); text-align: center; }
        .stat-val { font-size: 2.8rem; font-weight: 800; line-height: 1.2; }
        .stat-label { font-size: 0.8rem; text-transform: uppercase; color: var(--text-dim); font-weight: 800; margin-top: 8px; letter-spacing: 0.05em; }
        .main-grid { display: grid; grid-template-columns: 3fr 1fr; gap: 40px; }
        .sidebar-card { background: var(--card); border: 1px solid var(--border); padding: 30px; border-radius: 24px; box-shadow: var(--shadow); }
        footer { margin-top: 100px; text-align: center; color: var(--text-dim); font-size: 0.85rem; padding: 60px 0; border-top: 1px solid var(--border); }
        .module-tile:hover { transform: translateY(-5px); box-shadow: 0 10px 15px -3px rgba(0,0,0,0.1); border-color: var(--accent) !important; }
    </style>
</head>
<body>
    <div class="container">
        <header>
            <div>
                <h1>Gradle Lighthouse</h1>
                <div style="margin-top:12px; color: var(--text-dim); font-weight: 600; font-size: 1.1rem;">
                    Global Dashboard &bull; Gradle $gradleVersion &bull; V${pluginVersion.get()}
                </div>
            </div>
            <div style="text-align: right">
                <div class="stat-label">Project Maturity</div>
                <div style="font-size: 2rem; font-weight: 900; color: var(--accent)">${globalRank.displayName}</div>
            </div>
        </header>

        <div class="stat-grid">
            <div class="stat-card"><div class="stat-val" style="color: $scoreColor">$avgScore%</div><div class="stat-label">Health Score</div></div>
            <div class="stat-card"><div class="stat-val">$auditedCount</div><div class="stat-label">Modules</div></div>
            <div class="stat-card"><div class="stat-val" style="color: var(--danger)">$totalFatal</div><div class="stat-label">Fatals</div></div>
            <div class="stat-card"><div class="stat-val" style="color: #ef4444">$totalErrors</div><div class="stat-label">Errors</div></div>
        </div>

        <div class="main-grid">
            <div>
                <!-- HISTORICAL TREND & VELOCITY ANALYTICS -->
                <section style="background: var(--card); border: 1px solid var(--border); border-radius: 24px; padding: 30px; margin-bottom: 40px; box-shadow: var(--shadow); position: relative; overflow: hidden;">
                    <h2 style="font-size: 1.5rem; font-weight: 800; color: var(--accent); margin: 0 0 4px 0;">📈 Architectural Trend &amp; Velocity Analytics</h2>
                    <div style="font-size: 0.85rem; color: var(--text-dim); margin-bottom: 20px;">Track global health score, fatal issues, and coupling density over the last 30 builds</div>
                    <div style="position: relative; width: 100%; height: 220px; background: rgba(0,0,0,0.04); border-radius: 16px; border: 1px solid var(--border); overflow: hidden;">
                        <canvas id="historical-trend-canvas" style="width: 100%; height: 100%; display: block;"></canvas>
                    </div>
                    <div style="display: flex; gap: 20px; margin-top: 12px; font-size: 0.78rem; font-weight: 700;">
                        <span><span style="display:inline-block;width:12px;height:12px;background:#10b981;border-radius:2px;margin-right:5px;"></span>Health Score %</span>
                        <span><span style="display:inline-block;width:12px;height:12px;background:#ef4444;border-radius:2px;margin-right:5px;"></span>Total Fatals</span>
                        <span title="Coupling Density = Total module-to-module dependency edges ÷ Total modules. Measures the average number of direct dependencies each module has (average out-degree). Lower is better — aim for &lt; 3.0." style="cursor:help; border-bottom:1px dashed var(--text-dim);">
                            <span style="display:inline-block;width:12px;height:12px;background:#f59e0b;border-radius:2px;margin-right:5px;"></span>Coupling Density
                            <span style="font-size:0.65rem; color:var(--text-dim); font-weight:400; margin-left:4px;">(avg out-degree ℹ️)</span>
                        </span>
                    </div>
                </section>

                <!-- GALAXY GRAPH EXPLORER -->
                <section id="graph-section-card" style="background: var(--card); border: 1px solid var(--border); border-radius: 24px; padding: 30px; margin-bottom: 40px; box-shadow: var(--shadow); position: relative; overflow: hidden;">
                    <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px;">
                        <div>
                            <h2 style="font-size: 1.5rem; font-weight: 800; color: var(--accent); margin: 0;">🌌 Interactive Galaxy Graph</h2>
                            <div style="font-size: 0.85rem; color: var(--text-dim); margin-top: 4px;">Explore architectural coupling and orbital module relationships</div>
                        </div>
                        <div style="display: flex; gap: 10px;">
                            <button id="btn-reset-sandbox" onclick="resetSandbox()" style="display: none; background: rgba(239,68,68,0.15); color: #f87171; border: 1px solid rgba(239,68,68,0.3); padding: 6px 12px; border-radius: 8px; cursor: pointer; font-size: 0.8rem; font-weight: bold; transition: all 0.2s;">🔄 Reset Sandbox</button>
                            <label style="font-size: 0.8rem; background: rgba(255,255,255,0.06); padding: 6px 12px; border-radius: 8px; cursor: pointer; color: var(--text-dim); border: 1px solid var(--border);" id="btn-cycles">
                                <input type="checkbox" id="toggle-cycles" style="display: none;"> ⚠ Highlight Cycles
                            </label>
                            <button onclick="openFullscreenGraph()" style="background: var(--accent); color: white; border: none; padding: 6px 16px; border-radius: 8px; font-weight: 700; cursor: pointer; font-size: 0.8rem; transition: transform 0.2s;">⤢ Fullscreen Galaxy</button>
                        </div>
                    </div>

                    <div style="display: flex; gap: 20px;">
                        <!-- Inline Canvas -->
                        <div style="flex: 3; background: #0a0a0f; border-radius: 16px; height: 500px; position: relative; border: 1px solid #1e293b; overflow: hidden; box-shadow: inset 0 0 20px rgba(0,0,0,0.5);">
                            <canvas id="module-graph-canvas" style="width: 100%; height: 100%; display: block;"></canvas>
                        </div>
                        <!-- Node Details Panel -->
                        <div id="graph-node-details" style="flex: 1; background: var(--bg); border: 1px solid var(--border); border-radius: 16px; padding: 20px; font-size: 0.85rem; overflow-y: auto; max-height: 500px; box-shadow: inset 0 2px 4px rgba(0,0,0,0.02);">
                            <div style="text-align:center; padding:40px 0; color:var(--text-dim);">
                                <div style="font-size:3rem; margin-bottom: 10px;">🌌</div>
                                <div>Select a planetary module</div>
                            </div>
                        </div>
                    </div>
                </section>

                $moduleTilesHtml
            </div>
            <aside>
                <div class="sidebar-card">
                    <div class="stat-label" style="color: var(--danger); margin-bottom: 20px; font-size: 0.8rem;">Critical Risks</div>
                    ${if (criticalBacklog.isEmpty()) "<div style='color: var(--success); font-weight: 700;'>No high-risk modules found.</div>" else criticalBacklog}
                </div>
                $scoreEnginePanel
            </aside>
        </div>

        <!-- FULLSCREEN GALAXY OVERLAY -->
        <div id="graph-fullscreen-overlay" style="display: none; position: fixed; inset: 0; background: #050508; z-index: 9999; flex-direction: column; height: 100vh; max-height: 100vh; overflow: hidden;">
            <!-- Top Toolbar -->
            <div style="background: rgba(10,10,15,0.8); backdrop-filter: blur(10px); padding: 15px 30px; display: flex; justify-content: space-between; align-items: center; border-bottom: 1px solid #1e293b;">
                <div style="display: flex; align-items: center; gap: 20px;">
                    <div style="color: var(--accent); font-weight: 900; font-size: 1.2rem; display: flex; align-items: center; gap: 10px;">
                        🌌 Lighthouse Galaxy
                    </div>
                    <div id="fs-breadcrumb" style="font-size: 0.8rem; color: #64748b; font-weight: 600; padding-left: 20px; border-left: 1px solid #334155;">Galaxy View</div>
                </div>
                <div style="display: flex; gap: 15px; align-items: center;">
                    <div style="position: relative;">
                        <span style="position: absolute; left: 10px; top: 8px; font-size: 0.8rem; color: #64748b;">🔍</span>
                        <input type="text" placeholder="Find module..." onkeyup="graphSearchFilter(this.value)" style="background: rgba(255,255,255,0.05); border: 1px solid #334155; color: white; padding: 6px 12px 6px 30px; border-radius: 8px; font-size: 0.8rem; width: 200px; outline: none; transition: border-color 0.2s;">
                    </div>
                    <button onclick="exportGalaxySnapshot()" style="background: rgba(16,185,129,0.15); color: #10b981; border: 1px solid rgba(16,185,129,0.3); padding: 6px 12px; border-radius: 6px; font-size: 0.8rem; cursor: pointer; font-weight: bold;" title="Download PNG">📸 Snapshot</button>
                    <button onclick="graphZoomFit()" style="background: rgba(255,255,255,0.1); color: white; border: none; padding: 6px 12px; border-radius: 6px; font-size: 0.8rem; cursor: pointer; border: 1px solid rgba(255,255,255,0.1);" title="Fit to Screen (F)">[ F ] Center</button>
                    <button onclick="toggleLayerView()" id="btn-layer-view" style="background: rgba(255,152,0,0.15); color: #FF9800; border: 1px solid rgba(255,152,0,0.3); padding: 6px 12px; border-radius: 6px; font-size: 0.8rem; cursor: pointer; font-weight: bold;" title="Toggle Solar System Orbits (L)">[ L ] Orbit View</button>
                    <button id="fs-btn-reset-sandbox" onclick="resetSandbox()" style="display: none; background: rgba(239,68,68,0.15); color: #f87171; border: 1px solid rgba(239,68,68,0.3); padding: 6px 12px; border-radius: 6px; font-size: 0.8rem; cursor: pointer; font-weight: bold; transition: all 0.2s;">🔄 Reset Sandbox</button>
                    <button onclick="togglePerfectSimulation()" id="btn-perfect-sim" style="background: rgba(16,185,129,0.15); color: #a7f3d0; border: 1px solid rgba(16,185,129,0.3); padding: 6px 12px; border-radius: 6px; font-size: 0.8rem; cursor: pointer; font-weight: bold;" title="Simulate 100% Health & Clean dependencies">✨ Perfect Simulation</button>
                    <button onclick="closeFullscreenGraph()" style="background: rgba(239,68,68,0.2); color: #ef4444; border: 1px solid rgba(239,68,68,0.3); padding: 6px 16px; border-radius: 6px; font-size: 0.8rem; cursor: pointer; font-weight: bold;">✕ Close [ESC]</button>
                </div>
            </div>

            <div style="flex: 1; position: relative; display: flex; min-height: 0; overflow: hidden;">
                <!-- Main FS Canvas -->
                <div style="flex: 1; position: relative; overflow: hidden;">
                    <canvas id="fs-graph-canvas" style="width: 100%; height: 100%; display: block;"></canvas>

                    <!-- Minimap & Legend Widget -->
                    <div style="position: absolute; bottom: 30px; right: 30px; background: rgba(15,23,42,0.6); backdrop-filter: blur(8px); border: 1px solid #334155; border-radius: 12px; padding: 15px; box-shadow: 0 10px 25px rgba(0,0,0,0.5);">
                        <div style="font-size: 0.7rem; color: #94a3b8; font-weight: 800; margin-bottom: 8px; text-transform: uppercase;">Sector Map</div>
                        <div style="background: #000; border: 1px solid #1e293b; border-radius: 6px; width: 160px; height: 110px; margin-bottom: 15px; overflow: hidden;">
                            <canvas id="fs-minimap-canvas" width="160" height="110" style="display:block;"></canvas>
                        </div>
                        <div id="fs-layer-legend" style="display: grid; grid-template-columns: 1fr 1fr; gap: 4px;"></div>
                    </div>

                    <!-- Zoom Controls -->
                    <div style="position: absolute; bottom: 30px; left: 30px; display: flex; flex-direction: column; gap: 8px;">
                        <button onclick="fsZoomIn()" style="width: 36px; height: 36px; border-radius: 50%; background: rgba(30,41,59,0.8); backdrop-filter: blur(4px); border: 1px solid #475569; color: white; cursor: pointer; font-size: 1.2rem; display: flex; align-items: center; justify-content: center; transition: background 0.2s;">+</button>
                        <button onclick="fsZoomOut()" style="width: 36px; height: 36px; border-radius: 50%; background: rgba(30,41,59,0.8); backdrop-filter: blur(4px); border: 1px solid #475569; color: white; cursor: pointer; font-size: 1.2rem; display: flex; align-items: center; justify-content: center; transition: background 0.2s;">-</button>
                    </div>
                </div>

                <!-- Side Panel -->
                <div style="width: 360px; min-width: 360px; max-width: 360px; flex-shrink: 0; background: rgba(15,23,42,0.9); border-left: 1px solid #1e293b; display: flex; flex-direction: column; padding: 25px; max-height: 100%; overflow: hidden;">
                    <div id="fs-xp-panel" style="margin-bottom: 30px;"></div>
                    <div style="font-size: 0.8rem; color: #94a3b8; font-weight: 800; text-transform: uppercase; margin-bottom: 15px; padding-bottom: 8px; border-bottom: 1px solid #334155;">Module Intel</div>
                    <div id="fs-node-detail" style="flex: 1; overflow-y: auto;">
                        <div style="text-align:center; padding:40px 0; color:#64748b;">
                            <div style="font-size:3rem; margin-bottom:10px;">🪐</div>
                            <div>Select a planetary module</div>
                        </div>
                    </div>
                </div>
            </div>
        </div>

        <footer>
            &copy; 2026 Gradle Lighthouse &bull; Architectural Intelligence Protocol V${pluginVersion.get()}
        </footer>
    </div>

    <script>
        const globalHistory = $globalHistoryJson;
        const graphData = {
            nodes: [${reports.joinToString(",") {
                val safeId   = it.projectPath.replace("\\", "\\\\").replace("'", "\\'")
                val safeName = it.moduleName.replace("\\", "\\\\").replace("'", "\\'")
                val safeLayer = it.layer.replace("\\", "\\\\").replace("'", "\\'")
                "{ id: '$safeId', name: '$safeName', layer: '$safeLayer', score: ${it.score}, fatalCount: ${it.fatalCount}, errorCount: ${it.errorCount}, warningCount: ${it.warningCount}, infoCount: ${it.infoCount} }"
            }}],
            links: [${moduleDependencyGraphData.get().flatMap { line ->
                val parts = line.split("|", limit = 2)
                val src  = parts.getOrElse(0) { "" }.replace("\\", "\\\\").replace("'", "\\'")
                val deps = parts.getOrElse(1) { "" }.split(",").filter { it.isNotBlank() }
                deps.map { "{ source: '$src', target: '${it.replace("\\", "\\\\").replace("'", "\\'")}' }" }
            }.joinToString(",")}]
        };
    </script>
    <script>
        ${LighthouseGraphJs.SCRIPT}
    </script>
</body>
</html>
    """.trimIndent()

    private fun parseReportJson(json: String): ModuleReportData? {
        return try {
            val moduleName = extractJsonString(json, "module") ?: return null
            val projectPath = extractJsonString(json, "path") ?: ":$moduleName"
            val score = extractJsonInt(json, "score") ?: 0
            val rank = extractJsonString(json, "rank") ?: HealthScoreEngine.ArchitectRank.fromScore(score).displayName
            val fatalCount = extractJsonInt(json, "fatalCount") ?: 0
            val errorCount = extractJsonInt(json, "errorCount") ?: 0
            val warningCount = extractJsonInt(json, "warningCount") ?: 0
            val infoCount = extractJsonInt(json, "infoCount") ?: 0
            val topResolution = extractJsonString(json, "topResolution") ?: ""

            ModuleReportData(moduleName, projectPath, score, rank, fatalCount, errorCount, warningCount, infoCount, topResolution)
        } catch (e: Exception) {
            ConsoleLogger.error("Failed to parse module report: ${e.message}")
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
        val infoCount: Int,
        val topResolution: String
    ) {
        var relativeReportPath: String = ""

        val layer: String get() {
            if (projectPath.isBlank() || projectPath == ":") return "Root"
            val segments = projectPath.split(":").filter { it.isNotEmpty() }
            val raw = if (segments.isNotEmpty()) segments.first() else "Root"
            return if (raw.length > 1) {
                raw.substring(0, 1).uppercase(java.util.Locale.ROOT) + raw.substring(1)
            } else {
                raw.uppercase(java.util.Locale.ROOT)
            }
        }
    }

    // -------------------------------------------------------------------------
    // Phase B: Historical Trend Analytics
    // -------------------------------------------------------------------------

    private fun saveGlobalHistory(avgScore: Int, totalFatal: Int, couplingDensity: Double) {
        val historyDir = File(rootDirPath.get(), ".lighthouse")
        if (!historyDir.exists()) historyDir.mkdirs()
        val historyFile = File(historyDir, "global-history.json")
        val timestamp = java.time.LocalDate.now().toString()

        val existing = if (historyFile.exists()) historyFile.readText().trim() else "[]"
        val entries = existing.removeSurrounding("[", "]").trim()
        val newEntry = "{\"score\":$avgScore,\"fatalCount\":$totalFatal,\"couplingDensity\":$couplingDensity,\"timestamp\":\"$timestamp\"}"
        val allEntries = if (entries.isEmpty()) newEntry
                         else "$entries,$newEntry"
        // Keep last 30 builds
        val parts = allEntries.split("},{")
        val trimmed = if (parts.size > 30) parts.takeLast(30).joinToString("},{")
                      else allEntries
        historyFile.writeText("[$trimmed]")
    }

    private fun getGlobalHistoryJson(): String {
        val historyFile = File(rootDirPath.get(), ".lighthouse/global-history.json")
        return if (historyFile.exists()) historyFile.readText().trim() else "[]"
    }

    // -------------------------------------------------------------------------
    // Phase B: Custom Architecture Rule DSL (YAML)
    // -------------------------------------------------------------------------

    data class ArchRule(val name: String, val condition: String, val level: String)

    private fun parseRulesYaml(file: File): List<ArchRule> {
        val rules = mutableListOf<ArchRule>()
        var currentName = ""
        var currentCondition = ""
        var currentLevel = "error"
        var inRules = false

        file.readLines().forEach { rawLine ->
            val line = rawLine.trimEnd()
            val trimmed = line.trimStart()

            if (trimmed == "rules:") { inRules = true; return@forEach }
            if (!inRules) return@forEach

            when {
                trimmed.startsWith("- name:") -> {
                    if (currentName.isNotBlank() && currentCondition.isNotBlank()) {
                        rules.add(ArchRule(currentName, currentCondition, currentLevel))
                    }
                    currentName = trimmed.removePrefix("- name:").trim().trim('"')
                    currentCondition = ""
                    currentLevel = "error"
                }
                trimmed.startsWith("name:") -> {
                    currentName = trimmed.removePrefix("name:").trim().trim('"')
                }
                trimmed.startsWith("condition:") -> {
                    currentCondition = trimmed.removePrefix("condition:").trim().trim('"')
                }
                trimmed.startsWith("level:") -> {
                    currentLevel = trimmed.removePrefix("level:").trim().trim('"').lowercase()
                }
            }
        }
        if (currentName.isNotBlank() && currentCondition.isNotBlank()) {
            rules.add(ArchRule(currentName, currentCondition, currentLevel))
        }
        return rules
    }

    /**
     * Evaluates a **layering** rule whose condition is a `->` chain of layer names,
     * e.g. `"App -> Feature -> Domain -> Data -> Core -> Shared"`.
     *
     * **Scope:** Only modules whose layer names appear in the condition string are
     * evaluated.  Any module whose layer is NOT listed in the condition is silently
     * excluded from this specific rule.  This is intentional — partial rules let teams
     * define targeted constraints without having to enumerate every layer.
     *
     * **False-confidence caveat (documented):** If you write `"Feature -> Core"` this
     * rule will NOT flag an `App` module that depends on `Core`, because `App` is not
     * listed.  Use the full layer chain (`"App -> Feature -> … -> Core -> Shared"`) for
     * project-wide enforcement.  A ConsoleLogger warning is emitted whenever a rule
     * covers fewer than all known layers to alert teams of this behaviour.
     */
    private fun evaluateLayerRule(
        rule: ArchRule,
        graph: Map<String, List<String>>,
        pathToLayer: Map<String, String>
    ): List<String> {
        val layers = rule.condition.split("->").map { it.trim().lowercase() }
        val layerIndex = layers.withIndex().associate { it.value to it.index }
        val violations = mutableListOf<String>()

        // Warn if the rule covers only a subset of known codebase layers so teams are
        // aware partial rules do not protect unlisted layers.
        val knownLayers = pathToLayer.values.map { it.lowercase() }.toSet()
        val uncoveredLayers = knownLayers - layerIndex.keys
        if (uncoveredLayers.isNotEmpty()) {
            ConsoleLogger.warn(
                "[RULES] Rule \"${rule.name}\" covers layers [${layers.joinToString()}] " +
                "but the project also has layers [${uncoveredLayers.sorted().joinToString()}] " +
                "that are NOT governed by this rule.  Violations from those layers will be silently ignored. " +
                "Consider expanding the condition to include all layers for full coverage."
            )
        }

        graph.forEach { (src, targets) ->
            val srcLayerName = pathToLayer[src]?.lowercase() ?: return@forEach
            // Skip source modules whose layer is not mentioned in this rule
            val srcIdx = layerIndex[srcLayerName] ?: return@forEach

            targets.forEach { tgt ->
                val tgtLayerName = pathToLayer[tgt]?.lowercase() ?: return@forEach
                // Skip target modules whose layer is not mentioned in this rule
                val tgtIdx = layerIndex[tgtLayerName] ?: return@forEach
                if (src != tgt && srcIdx > tgtIdx) {
                    violations.add("Layer Boundary Violation: $src ($srcLayerName) -> $tgt ($tgtLayerName)")
                }
            }
        }
        return violations
    }

    /**
     * Evaluates an **isolation** rule whose condition uses the `!->` operator,
     * e.g. `":feature:* !-> :feature:*"` or `"* !-> :core:design-system:internal"`.
     *
     * **Pattern syntax:**
     * - `*`  — matches **any single path segment** (or the entire path when written alone)
     * - `prefix:*` — matches any module whose path starts with `prefix:`
     * - `:exact:path` — exact module path match
     *
     * A bare `*` as the source pattern (e.g. `"* !-> :core:design-system:internal"`)
     * explicitly means "every module in the project".  This is intentional and
     * documented behaviour, not a coincidence of the implementation.
     *
     * **Same-group exemption:** Modules that share a common two-segment group are
     * allowed to depend on each other even if both match the patterns.  For example,
     * `:feature:search:impl -> :feature:search:api` is permitted when the rule is
     * `:feature:* !-> :feature:*`, because both share the group `:feature:search`.
     */
    private fun evaluateIsolationRule(
        rule: ArchRule,
        graph: Map<String, List<String>>
    ): List<String> {
        val parts = rule.condition.split("!->").map { it.trim() }
        if (parts.size != 2) return emptyList()
        val srcPattern = parts[0]
        val tgtPattern = parts[1]
        val violations = mutableListOf<String>()

        /**
         * Returns true if [path] matches [pattern].
         *
         * Rules:
         *  - `"*"` alone → matches everything (universal wildcard)
         *  - `"prefix:*"` → path starts with `prefix:` after stripping leading colons
         *  - `"exact"` → exact equality after stripping leading colons
         */
        fun matchesPattern(path: String, pattern: String): Boolean {
            val cleanPath = path.trim(':')
            val cleanPat  = pattern.trim(':')
            return when {
                cleanPat == "*"            -> true                              // bare wildcard: match all
                cleanPat.endsWith(":*")    -> cleanPath.startsWith(cleanPat.removeSuffix("*"))
                cleanPat.endsWith("*")     -> cleanPath.startsWith(cleanPat.removeSuffix("*"))
                else                       -> cleanPath == cleanPat
            }
        }

        fun featureGroup(path: String): String {
            val segs = path.trim(':').split(":")
            return if (segs.size >= 2) segs[0] + ":" + segs[1] else path.trim(':')
        }

        graph.forEach { (src, targets) ->
            if (!matchesPattern(src, srcPattern)) return@forEach
            val srcGroup = featureGroup(src)
            targets.forEach { tgt ->
                if (!matchesPattern(tgt, tgtPattern)) return@forEach
                val tgtGroup = featureGroup(tgt)
                // Same feature sub-modules are allowed (e.g. :feature:search:impl -> :feature:search:api)
                if (srcGroup != tgtGroup) {
                    violations.add("Isolation Violation: $src must not depend on $tgt")
                }
            }
        }
        return violations
    }
}
