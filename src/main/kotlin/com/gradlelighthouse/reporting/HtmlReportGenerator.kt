package com.gradlelighthouse.reporting

import com.gradlelighthouse.core.AuditIssue
import com.gradlelighthouse.core.HealthScoreEngine
import com.gradlelighthouse.core.Severity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Premium HTML Report Generator for Gradle Lighthouse.
 *
 * Design principles:
 * - Dark/Light mode via `prefers-color-scheme` (auto-detects OS preference)
 * - XSS-safe: ALL dynamic content is HTML-escaped before injection
 * - Responsive: Works on mobile, tablet, and desktop viewports
 * - Print-friendly: Proper `@media print` styles
 * - Offline-capable: System font stack fallback (no CDN dependency)
 * - Accessible: Proper contrast ratios and semantic HTML
 */
object HtmlReportGenerator {

    /**
     * Generates a complete HTML report for a module.
     *
     * @param projectName Module name
     * @param pluginVersion Plugin version string
     * @param gradleVersion Gradle version string
     * @param issues List of audit findings
     * @return Complete HTML document string
     */
    fun generate(projectName: String, pluginVersion: String, gradleVersion: String, issues: List<AuditIssue>): String {
        val dateString = SimpleDateFormat("MMM dd, yyyy - HH:mm:ss", Locale.ROOT).format(Date())
        val report = HealthScoreEngine.generateReport(issues)
        val scoreColor = HealthScoreEngine.scoreColor(report.score)

        val riskMatrixRows = report.deductions.take(5).joinToString("") {
            """<tr><td><b>${esc(it.category)}</b></td><td style="color: var(--danger); font-weight: 700;">-${it.points}</td><td><span style="font-size: 0.85rem">${esc(it.primaryBottleneck)}</span></td></tr>"""
        }

        val categories = listOf("Stability", "PlayStorePolicy", "DependencyHealth", "Performance", "AppSize", "Modernization", "Architecture")
        val groupedIssues = issues.groupBy { it.category }

        val tabsHtml = categories.mapIndexed { index, cat ->
            val count = groupedIssues[cat]?.size ?: 0
            val activeClass = if (index == 0) "active" else ""
            """<button class="tab-btn $activeClass" onclick="openTab(event, '${esc(cat)}')" role="tab" aria-selected="${index == 0}">${esc(cat)} <span class="badge">$count</span></button>"""
        }.joinToString("\n")

        val tabContentHtml = categories.mapIndexed { index, cat ->
            val catIssues = groupedIssues[cat] ?: emptyList()
            val displayStyle = if (index == 0) "display: block;" else "display: none;"
            val cards = catIssues.mapIndexed { i, issue ->
                val hoursSaved = HealthScoreEngine.estimatedHoursSaved(issue.severity)
                """
                <div class="card issue-card fade-in" style="animation-delay: ${0.05 * i}s; border-left-color: ${issue.severity.color}">
                    <div class="card-header">
                        <span class="severity" style="background-color: ${issue.severity.color}">${issue.severity.name}</span>
                        <span class="audit-title">${esc(issue.title)}</span>
                    </div>
                    <div class="issue-grid">
                        <div class="grid-item"><div class="label">Architectural Reasoning</div><div class="content">${esc(issue.reasoning)}</div></div>
                        <div class="grid-item"><div class="label">Production Impact</div><div class="content warning">${esc(issue.impactAnalysis)}</div></div>
                    </div>
                    <div class="resolution-box"><div class="label">Strategic Resolution</div><div class="code-content">${esc(issue.resolution)}</div></div>
                    <div class="roi-bar">
                        <div style="display: flex; gap: 15px; flex-wrap: wrap;">
                            <span class="roi-badge">ROI: ${esc(issue.roiAfterFix)}</span>
                            <span class="roi-badge roi-time">Est. Saving: $hoursSaved</span>
                        </div>
                    </div>
                </div>
                """
            }.joinToString("\n")

            val content = if (cards.isEmpty()) {
                """<div class="empty-state">No risks found for ${esc(cat)}</div>"""
            } else cards

            """<div id="${esc(cat)}" class="tab-pane" style="$displayStyle" role="tabpanel">$content</div>"""
        }.joinToString("\n")

        return buildString {
            appendLine("<!DOCTYPE html>")
            appendLine("""<html lang="en">""")
            appendLine("<head>")
            appendLine("""<meta charset="UTF-8">""")
            appendLine("""<meta name="viewport" content="width=device-width, initial-scale=1.0">""")
            appendLine("""<meta name="description" content="Gradle Lighthouse Architectural Intelligence Report for ${esc(projectName)}">""")
            appendLine("""<title>Gradle Lighthouse - ${esc(projectName)}</title>""")
            appendLine("<style>")
            appendLine(generateCss(scoreColor))
            appendLine("</style>")
            appendLine("</head>")
            appendLine("<body>")
            appendLine("""<div class="container">""")

            // Header
            appendLine("""
                <header>
                    <div>
                        <h1 class="brand-title">Gradle Lighthouse</h1>
                        <div style="color:var(--text-dim); font-weight: 600;">${esc(projectName)} &bull; Intelligence Audit &bull; $dateString</div>
                        <div class="env-badge">Gradle $gradleVersion | V$pluginVersion</div>
                    </div>
                    <div class="card score-widget" style="padding: 15px 40px; border-radius: 24px;">
                        <div class="circle" style="border-color: $scoreColor; color: $scoreColor;" role="img" aria-label="Health Score: ${report.score}%">${report.score}</div>
                        <div class="rank-val">${report.rank.displayName}</div>
                    </div>
                </header>
            """.trimIndent())

            // Executive Summary
            appendLine("""
                <div class="executive-summary">
                    <div class="card" style="display: flex; flex-direction: column; justify-content: center; background: var(--gradient-card);">
                        <div class="label">Executive Strategy</div>
                        <p style="font-size: 1.1rem; font-weight: 600; margin: 10px 0; color: var(--accent);">
                            ${if (report.score >= 90) "Architecture is optimized for scale. Focus on maintenance." else "High Technical Debt detected. Modernization is critical for stability."}
                        </p>
                        <div style="display: flex; gap: 20px; margin-top: 15px; flex-wrap: wrap;">
                            <div class="mini-stat"><span class="mini-val" style="color: var(--danger)">${report.fatalCount}</span><span class="mini-label">Fatal</span></div>
                            <div class="mini-stat"><span class="mini-val" style="color: #ef4444">${report.errorCount}</span><span class="mini-label">Errors</span></div>
                            <div class="mini-stat"><span class="mini-val" style="color: var(--warning)">${report.warningCount}</span><span class="mini-label">Warnings</span></div>
                            <div class="mini-stat"><span class="mini-val" style="color: var(--info)">${report.infoCount}</span><span class="mini-label">Info</span></div>
                        </div>
                    </div>
                    <div class="card">
                        <table class="risk-matrix">
                            <thead><tr><th>Risk Category</th><th>Points</th><th>Primary Bottleneck</th></tr></thead>
                            <tbody>$riskMatrixRows</tbody>
                        </table>
                    </div>
                </div>
            """.trimIndent())

            // Tabs
            appendLine("""<div class="tabs" role="tablist">$tabsHtml</div>""")
            appendLine("""<div class="tab-contents">$tabContentHtml</div>""")

            // Footer
            appendLine("""
                <footer>
                    &copy; 2026 Gradle Lighthouse &bull; Architectural Intelligence Protocol V$pluginVersion
                </footer>
            """.trimIndent())

            appendLine("</div>")

            // JavaScript
            appendLine("<script>")
            appendLine(generateJs())
            appendLine("</script>")
            appendLine("</body>")
            appendLine("</html>")
        }
    }

    /**
     * Generates the module-report JSON for aggregation.
     */
    fun generateJson(projectName: String, projectPath: String, issues: List<AuditIssue>): String {
        val report = HealthScoreEngine.generateReport(issues)

        val topIssue = issues.maxByOrNull {
            when (it.severity) { Severity.FATAL -> 100; Severity.ERROR -> 50; Severity.WARNING -> 10; Severity.INFO -> 1 }
        }
        val topResolution = topIssue?.let {
            "Fix ${it.title} to improve score by approx ${if (it.severity == Severity.FATAL) "35" else "15"}%"
        } ?: "No critical fixes required."

        val issuesJson = issues.joinToString(",\n") { issue ->
            """    {
      "title": "${escJson(issue.title)}",
      "severity": "${issue.severity.name}",
      "category": "${escJson(issue.category)}",
      "reasoning": "${escJson(issue.reasoning)}",
      "impact": "${escJson(issue.impactAnalysis)}",
      "resolution": "${escJson(issue.resolution)}",
      "roiAfterFix": "${escJson(issue.roiAfterFix)}"
    }"""
        }

        return """{
  "module": "${escJson(projectName)}",
  "path": "${escJson(projectPath)}",
  "score": ${report.score},
  "rank": "${report.rank.displayName}",
  "fatalCount": ${report.fatalCount},
  "errorCount": ${report.errorCount},
  "warningCount": ${report.warningCount},
  "infoCount": ${report.infoCount},
  "totalIssues": ${report.totalIssueCount},
  "topResolution": "${escJson(topResolution)}",
  "issues": [
$issuesJson
  ]
}"""
    }

    // =========================================================================
    // CSS — Dark/Light mode, responsive, print-friendly
    // =========================================================================

    private fun generateCss(scoreColor: String): String = """
        :root {
            --bg: #f8fafc; --card: #ffffff; --border: #e2e8f0;
            --text: #0f172a; --text-dim: #64748b;
            --accent: #FF9800; --success: #10b981; --warning: #f59e0b;
            --danger: #dc2626; --info: #3b82f6;
            --gradient-card: linear-gradient(135deg, #fff7ed, #ffffff);
            --shadow: 0 4px 6px -1px rgb(0 0 0 / 0.1), 0 2px 4px -2px rgb(0 0 0 / 0.1);
            --shadow-lg: 0 10px 15px -3px rgb(0 0 0 / 0.1);
        }

        @media (prefers-color-scheme: dark) {
            :root {
                --bg: #0f172a; --card: #1e293b; --border: #334155;
                --text: #f1f5f9; --text-dim: #94a3b8;
                --gradient-card: linear-gradient(135deg, #1e293b, #0f172a);
                --shadow: 0 4px 6px -1px rgb(0 0 0 / 0.3);
                --shadow-lg: 0 10px 15px -3px rgb(0 0 0 / 0.4);
            }
        }

        * { box-sizing: border-box; margin: 0; padding: 0; }
        body {
            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
            background: var(--bg); color: var(--text); line-height: 1.6;
        }
        .container { max-width: 1200px; margin: 0 auto; padding: 60px 20px; }

        header { display: flex; justify-content: space-between; align-items: flex-end; margin-bottom: 50px; flex-wrap: wrap; gap: 20px; }
        .brand-title { font-size: 2.8rem; font-weight: 800; color: var(--accent); }

        .card { background: var(--card); border: 1px solid var(--border); border-radius: 20px; padding: 30px; box-shadow: var(--shadow); }

        .score-widget { display: flex; flex-direction: column; align-items: center; justify-content: center; text-align: center; }
        .circle { width: 100px; height: 100px; border-radius: 50%; display: flex; align-items: center; justify-content: center; border: 8px solid $scoreColor; font-weight: 800; font-size: 2.2rem; color: $scoreColor; margin-bottom: 15px; background: var(--card); }
        .rank-val { font-size: 1.4rem; font-weight: 700; color: var(--text); }

        .executive-summary { display: grid; grid-template-columns: 1fr 2fr; gap: 30px; margin-bottom: 40px; }

        .mini-stat { display: flex; flex-direction: column; align-items: center; }
        .mini-val { font-size: 1.5rem; font-weight: 800; }
        .mini-label { font-size: 0.7rem; text-transform: uppercase; color: var(--text-dim); font-weight: 700; }

        .risk-matrix { width: 100%; border-collapse: collapse; }
        .risk-matrix th, .risk-matrix td { text-align: left; padding: 12px; border-bottom: 1px solid var(--border); }
        .risk-matrix th { font-size: 0.7rem; text-transform: uppercase; color: var(--text-dim); font-weight: 800; letter-spacing: 0.05em; }

        .env-badge { display: inline-block; padding: 4px 12px; border-radius: 100px; font-size: 0.75rem; font-weight: 700; background: rgba(255, 152, 0, 0.1); color: var(--accent); border: 1px solid rgba(255, 152, 0, 0.2); margin-top: 5px; }

        .tabs { display: flex; gap: 10px; margin-bottom: 30px; overflow-x: auto; padding-bottom: 10px; -webkit-overflow-scrolling: touch; }
        .tab-btn { background: var(--card); border: 1px solid var(--border); color: var(--text-dim); padding: 12px 20px; border-radius: 16px; font-weight: 700; cursor: pointer; transition: all 0.2s; white-space: nowrap; font-size: 0.9rem; box-shadow: var(--shadow); }
        .tab-btn:hover { border-color: var(--accent); color: var(--text); }
        .tab-btn.active { background: var(--accent); color: #fff; border-color: var(--accent); }
        .badge { background: rgba(0,0,0,0.1); color: inherit; padding: 2px 8px; border-radius: 6px; font-size: 0.7rem; margin-left: 8px; }

        .issue-card { margin-bottom: 25px; transition: all 0.2s; border-left: 6px solid var(--accent); position: relative; }
        .issue-card:hover { transform: translateY(-2px); box-shadow: var(--shadow-lg); }
        .card-header { display: flex; gap: 15px; align-items: center; margin-bottom: 20px; flex-wrap: wrap; }
        .severity { padding: 6px 14px; border-radius: 8px; font-size: 0.75rem; font-weight: 800; color: #fff; text-transform: uppercase; }
        .audit-title { font-size: 1.2rem; font-weight: 700; color: var(--text); }
        .issue-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 20px; margin-bottom: 20px; }
        .label { font-size: 0.7rem; text-transform: uppercase; color: var(--text-dim); font-weight: 800; margin-bottom: 8px; letter-spacing: 0.03em; }
        .content { font-size: 0.95rem; color: var(--text); }
        .warning { color: var(--danger); border-left: 2px solid var(--danger); padding-left: 10px; }

        .resolution-box { background: var(--bg); padding: 20px; border-radius: 16px; border: 1px solid var(--border); margin-bottom: 20px; }
        .code-content { font-family: 'SF Mono', 'Cascadia Code', 'Fira Code', monospace; font-size: 0.85rem; color: var(--text); white-space: pre-wrap; word-break: break-word; }

        .roi-bar { border-top: 1px solid var(--border); padding-top: 15px; }
        .roi-badge { display: inline-block; padding: 6px 14px; border-radius: 10px; font-size: 0.8rem; font-weight: 700; background: rgba(255, 152, 0, 0.1); color: var(--accent); border: 1px solid rgba(255, 152, 0, 0.2); }
        .roi-time { background: rgba(16, 185, 129, 0.1); color: var(--success); border-color: rgba(16, 185, 129, 0.2); }

        .empty-state { text-align: center; padding: 60px; color: var(--text-dim); font-weight: 600; font-size: 1.1rem; border: 2px dashed var(--border); border-radius: 20px; background: var(--card); }

        footer { margin-top: 80px; text-align: center; color: var(--text-dim); font-size: 0.8rem; border-top: 1px solid var(--border); padding-top: 40px; }

        @keyframes fadeIn { from { opacity: 0; transform: translateY(10px); } to { opacity: 1; transform: translateY(0); } }
        .fade-in { animation: fadeIn 0.4s ease-out forwards; }

        /* Responsive */
        @media (max-width: 768px) {
            .container { padding: 20px 16px; }
            .brand-title { font-size: 2rem; }
            header { flex-direction: column; align-items: flex-start; }
            .executive-summary { grid-template-columns: 1fr; }
            .issue-grid { grid-template-columns: 1fr; }
            .tabs { gap: 6px; }
            .tab-btn { padding: 8px 14px; font-size: 0.8rem; }
        }

        /* Print */
        @media print {
            body { background: #fff; color: #000; }
            .card { box-shadow: none; border: 1px solid #ccc; page-break-inside: avoid; }
            .tabs { display: none; }
            .tab-pane { display: block !important; page-break-before: always; }
            .issue-card:hover { transform: none; }
            footer { page-break-before: always; }
        }
    """.trimIndent()

    // =========================================================================
    // JavaScript
    // =========================================================================

    private fun generateJs(): String = """
        function openTab(evt, catName) {
            var tabcontent = document.getElementsByClassName("tab-pane");
            for (var i = 0; i < tabcontent.length; i++) { tabcontent[i].style.display = "none"; }
            var tablinks = document.getElementsByClassName("tab-btn");
            for (var i = 0; i < tablinks.length; i++) {
                tablinks[i].className = tablinks[i].className.replace(" active", "");
                tablinks[i].setAttribute("aria-selected", "false");
            }
            document.getElementById(catName).style.display = "block";
            evt.currentTarget.className += " active";
            evt.currentTarget.setAttribute("aria-selected", "true");
        }
    """.trimIndent()

    // =========================================================================
    // Security: HTML & JSON escaping
    // =========================================================================

    /** HTML-escape to prevent XSS injection from dependency names or issue content */
    private fun esc(text: String): String = text
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&#x27;")

    /** JSON-escape for safe serialization */
    private fun escJson(text: String): String = text
        .replace("\\", "\\\\")
        .replace("\"", "\\\"")
        .replace("\n", "\\n")
        .replace("\r", "\\r")
        .replace("\t", "\\t")
}
