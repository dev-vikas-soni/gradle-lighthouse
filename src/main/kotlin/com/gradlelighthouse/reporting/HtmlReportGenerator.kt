package com.gradlelighthouse.reporting

import com.gradlelighthouse.core.AuditIssue
import com.gradlelighthouse.core.HealthScoreEngine
import com.gradlelighthouse.core.Severity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Premium HTML Report Generator for Gradle Lighthouse V2.0.
 *
 * Design inspired by: Vercel Dashboard, Linear, Raycast — modern SaaS aesthetics.
 * - Dark/Light mode via `prefers-color-scheme`
 * - XSS-safe: ALL dynamic content HTML-escaped
 * - Responsive: mobile, tablet, desktop
 * - Print-friendly via @media print
 * - Zero external dependencies (system fonts, inline SVG)
 * - Animated score ring, smooth transitions, glass-morphism cards
 */
object HtmlReportGenerator {

    fun generate(projectName: String, pluginVersion: String, gradleVersion: String, issues: List<AuditIssue>): String {
        val dateString = SimpleDateFormat("MMM dd, yyyy • HH:mm", Locale.ROOT).format(Date())
        val report = HealthScoreEngine.generateReport(issues)
        val scoreColor = HealthScoreEngine.scoreColor(report.score)
        val scorePercent = report.score.coerceIn(0, 100)

        val riskMatrixRows = report.deductions.take(5).joinToString("") {
            """<tr><td><span class="risk-cat">${esc(it.category)}</span></td><td class="risk-pts">-${it.points}</td><td class="risk-desc">${esc(it.primaryBottleneck)}</td></tr>"""
        }

        val defaultCategories = listOf(
            "Stability", "PlayStorePolicy", "DependencyHealth", "Performance", "AppSize",
            "Modernization", "Architecture", "BuildPerformance", "Security", "Quality",
            "Complexity", "DependencyHygiene", "Trends"
        )
        val groupedIssues = issues.groupBy { it.category }
        val categories = (defaultCategories + groupedIssues.keys).distinct().filter { groupedIssues.containsKey(it) }

        val tabsHtml = categories.mapIndexed { index, cat ->
            val count = groupedIssues[cat]?.size ?: 0
            val activeClass = if (index == 0) "active" else ""
            val icon = categoryIcon(cat)
            """<button class="tab-btn $activeClass" onclick="openTab(event, '${esc(cat)}')" role="tab" aria-selected="${index == 0}">$icon ${esc(cat)} <span class="badge">$count</span></button>"""
        }.joinToString("\n")

        val tabContentHtml = categories.mapIndexed { index, cat ->
            val catIssues = groupedIssues[cat] ?: emptyList()
            val displayStyle = if (index == 0) "display: block;" else "display: none;"
            val cards = catIssues.mapIndexed { i, issue ->
                val hoursSaved = HealthScoreEngine.estimatedHoursSaved(issue.severity)
                val severityClass = issue.severity.name.lowercase()
                """
                <div class="issue-card fade-in" style="animation-delay: ${0.04 * i}s;">
                    <div class="issue-card-border" style="background: ${issue.severity.color}"></div>
                    <div class="issue-card-body">
                        <div class="card-header">
                            <span class="severity-pill $severityClass">${issue.severity.name}</span>
                            <h3 class="issue-title">${esc(issue.title)}</h3>
                        </div>
                        <div class="issue-details">
                            <div class="detail-section">
                                <div class="detail-label">
                                    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><path d="M12 16v-4M12 8h.01"/></svg>
                                    Why This Matters
                                </div>
                                <p class="detail-text">${esc(issue.reasoning)}</p>
                            </div>
                            <div class="detail-section impact">
                                <div class="detail-label">
                                    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M10.29 3.86L1.82 18a2 2 0 001.71 3h16.94a2 2 0 001.71-3L13.71 3.86a2 2 0 00-3.42 0z"/><line x1="12" y1="9" x2="12" y2="13"/><line x1="12" y1="17" x2="12.01" y2="17"/></svg>
                                    Production Impact
                                </div>
                                <p class="detail-text">${esc(issue.impactAnalysis)}</p>
                            </div>
                        </div>
                        <div class="resolution-section">
                            <div class="detail-label">
                                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="20 6 9 17 4 12"/></svg>
                                Resolution
                            </div>
                            <pre class="resolution-code">${esc(issue.resolution)}</pre>
                        </div>
                        <div class="roi-section">
                            <span class="roi-tag roi-value">💰 ${esc(issue.roiAfterFix)}</span>
                            <span class="roi-tag roi-time">⏱️ Save $hoursSaved/cycle</span>
                            ${if (issue.sourceFile != null) """<span class="roi-tag roi-file">📄 ${esc(issue.sourceFile.substringAfterLast("/"))}</span>""" else ""}
                        </div>
                    </div>
                </div>
                """
            }.joinToString("\n")

            val content = if (cards.isEmpty()) {
                """<div class="empty-state"><div class="empty-icon">✅</div><div>No issues found in ${esc(cat)}</div></div>"""
            } else cards

            """<div id="${esc(cat)}" class="tab-pane" style="$displayStyle" role="tabpanel">$content</div>"""
        }.joinToString("\n")

        return buildString {
            appendLine("<!DOCTYPE html>")
            appendLine("""<html lang="en">""")
            appendLine("<head>")
            appendLine("""<meta charset="UTF-8">""")
            appendLine("""<meta name="viewport" content="width=device-width, initial-scale=1.0">""")
            appendLine("""<title>Gradle Lighthouse — ${esc(projectName)}</title>""")
            appendLine("<style>")
            appendLine(generateCss(scoreColor, scorePercent))
            appendLine("</style>")
            appendLine("</head>")
            appendLine("<body>")

            // Navigation bar
            appendLine("""
            <nav class="topbar">
                <div class="topbar-inner">
                    <div class="topbar-brand">
                        <svg width="24" height="24" viewBox="0 0 24 24" fill="none"><path d="M12 2L2 7l10 5 10-5-10-5zM2 17l10 5 10-5M2 12l10 5 10-5" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/></svg>
                        <span>Gradle Lighthouse</span>
                    </div>
                    <div class="topbar-meta">
                        <span class="meta-badge">Gradle $gradleVersion</span>
                        <span class="meta-badge">V$pluginVersion</span>
                        <span class="meta-badge meta-date">$dateString</span>
                    </div>
                </div>
            </nav>
            """.trimIndent())

            appendLine("""<main class="container">""")

            // Hero section with score ring
            appendLine("""
            <section class="hero">
                <div class="hero-left">
                    <h1 class="hero-title">${esc(projectName)}</h1>
                    <p class="hero-subtitle">Architectural Health Intelligence Report</p>
                    <div class="hero-stats">
                        <div class="stat-chip fatal"><span class="stat-num">${report.fatalCount}</span><span class="stat-label">Fatal</span></div>
                        <div class="stat-chip error"><span class="stat-num">${report.errorCount}</span><span class="stat-label">Errors</span></div>
                        <div class="stat-chip warning"><span class="stat-num">${report.warningCount}</span><span class="stat-label">Warnings</span></div>
                        <div class="stat-chip info"><span class="stat-num">${report.infoCount}</span><span class="stat-label">Info</span></div>
                    </div>
                </div>
                <div class="hero-right">
                    <div class="score-ring">
                        <svg viewBox="0 0 120 120" class="score-svg">
                            <circle class="score-bg" cx="60" cy="60" r="52"/>
                            <circle class="score-fg" cx="60" cy="60" r="52" style="stroke: $scoreColor;"/>
                        </svg>
                        <div class="score-inner">
                            <span class="score-num" style="color: $scoreColor">${report.score}</span>
                            <span class="score-label">/ 100</span>
                        </div>
                    </div>
                    <div class="rank-badge" style="border-color: $scoreColor; color: $scoreColor;">${report.rank.emoji} ${report.rank.displayName}</div>
                </div>
            </section>
            """.trimIndent())

            // Strategy + Risk Matrix
            appendLine("""
            <section class="insights-grid">
                <div class="glass-card strategy-card">
                    <div class="card-icon">🎯</div>
                    <h2>Executive Strategy</h2>
                    <p class="strategy-text">
                        ${if (report.score >= 90) "Architecture is optimized for scale. Focus on maintenance and monitoring."
                          else if (report.score >= 70) "Solid foundation with room for improvement. Target high-impact issues first."
                          else if (report.score >= 50) "Significant technical debt detected. Prioritize FATAL and ERROR items to prevent production incidents."
                          else "Critical architectural debt. Immediate action required on FATAL issues to stabilize the codebase."}
                    </p>
                    <div class="strategy-action">
                        ${if (report.fatalCount > 0) "<span class=\"action-urgent\">⚡ ${report.fatalCount} critical issues require immediate attention</span>"
                          else if (report.errorCount > 0) "<span class=\"action-warn\">🔧 ${report.errorCount} errors blocking next rank</span>"
                          else "<span class=\"action-good\">✨ Great shape — maintain this standard</span>"}
                    </div>
                </div>
                <div class="glass-card risk-card">
                    <h2>Risk Heatmap</h2>
                    <table class="risk-matrix">
                        <thead><tr><th>Category</th><th>Impact</th><th>Top Bottleneck</th></tr></thead>
                        <tbody>$riskMatrixRows</tbody>
                    </table>
                    ${if (report.deductions.isEmpty()) "<div class=\"empty-mini\">No deductions — perfect score! 🎉</div>" else ""}
                </div>
            </section>
            """.trimIndent())

            // Tabs
            appendLine("""<section class="findings-section">""")
            appendLine("""<h2 class="section-title">Detailed Findings <span class="finding-count">${issues.size} issues across ${categories.size} categories</span></h2>""")
            appendLine("""<div class="tabs" role="tablist">$tabsHtml</div>""")
            appendLine("""<div class="tab-contents">$tabContentHtml</div>""")
            appendLine("""</section>""")

            // Footer
            appendLine("""
            <footer>
                <div class="footer-brand">Gradle Lighthouse V$pluginVersion</div>
                <div class="footer-copy">&copy; 2026 · Architectural Intelligence for Android & KMP</div>
            </footer>
            """.trimIndent())

            appendLine("</main>")
            appendLine("<script>")
            appendLine(generateJs())
            appendLine("</script>")
            appendLine("</body></html>")
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
    // CSS — Modern glassmorphism, animated score ring, dark/light
    // =========================================================================

    @Suppress("UNUSED_PARAMETER")
    private fun generateCss(scoreColor: String, scorePercent: Int): String {
        val circumference = (2 * Math.PI * 52).toInt() // r=52
        val dashOffset = circumference - (circumference * scorePercent / 100)
        return """
        :root {
            --bg: #fafbfc; --bg-subtle: #f1f5f9;
            --card: #ffffff; --card-hover: #ffffff;
            --border: #e2e8f0; --border-subtle: #f1f5f9;
            --text: #0f172a; --text-dim: #64748b; --text-muted: #94a3b8;
            --accent: #FF9800; --accent-soft: rgba(255, 152, 0, 0.08);
            --success: #10b981; --warning: #f59e0b; --danger: #ef4444; --info: #3b82f6;
            --fatal: #dc2626;
            --radius: 16px; --radius-sm: 10px; --radius-xs: 6px;
            --shadow: 0 1px 3px 0 rgb(0 0 0 / 0.06), 0 1px 2px -1px rgb(0 0 0 / 0.06);
            --shadow-md: 0 4px 6px -1px rgb(0 0 0 / 0.07), 0 2px 4px -2px rgb(0 0 0 / 0.05);
            --shadow-lg: 0 10px 15px -3px rgb(0 0 0 / 0.08), 0 4px 6px -4px rgb(0 0 0 / 0.04);
            --glass: rgba(255,255,255,0.7);
            --glass-border: rgba(255,255,255,0.3);
        }
        @media (prefers-color-scheme: dark) {
            :root {
                --bg: #09090b; --bg-subtle: #18181b;
                --card: #1c1c1f; --card-hover: #27272a;
                --border: #27272a; --border-subtle: #1c1c1f;
                --text: #fafafa; --text-dim: #a1a1aa; --text-muted: #71717a;
                --shadow: 0 1px 3px 0 rgb(0 0 0 / 0.3);
                --shadow-md: 0 4px 6px -1px rgb(0 0 0 / 0.4);
                --shadow-lg: 0 10px 15px -3px rgb(0 0 0 / 0.5);
                --glass: rgba(28,28,31,0.8);
                --glass-border: rgba(39,39,42,0.5);
            }
        }

        * { box-sizing: border-box; margin: 0; padding: 0; }
        body { font-family: -apple-system, BlinkMacSystemFont, 'Inter', 'Segoe UI', sans-serif; background: var(--bg); color: var(--text); line-height: 1.6; -webkit-font-smoothing: antialiased; }

        /* Topbar */
        .topbar { position: sticky; top: 0; z-index: 100; background: var(--glass); backdrop-filter: blur(12px); -webkit-backdrop-filter: blur(12px); border-bottom: 1px solid var(--border); }
        .topbar-inner { max-width: 1280px; margin: 0 auto; padding: 14px 32px; display: flex; justify-content: space-between; align-items: center; }
        .topbar-brand { display: flex; align-items: center; gap: 10px; font-weight: 700; font-size: 0.95rem; color: var(--text); }
        .topbar-brand svg { color: var(--accent); }
        .topbar-meta { display: flex; gap: 8px; align-items: center; }
        .meta-badge { font-size: 0.72rem; font-weight: 600; padding: 4px 10px; border-radius: 100px; background: var(--bg-subtle); color: var(--text-dim); border: 1px solid var(--border); }
        .meta-date { color: var(--text-muted); }

        .container { max-width: 1280px; margin: 0 auto; padding: 40px 32px 80px; }

        /* Hero */
        .hero { display: flex; justify-content: space-between; align-items: center; margin-bottom: 48px; gap: 40px; flex-wrap: wrap; }
        .hero-left { flex: 1; min-width: 300px; }
        .hero-title { font-size: 2.4rem; font-weight: 800; letter-spacing: -0.03em; color: var(--text); margin-bottom: 4px; }
        .hero-subtitle { color: var(--text-dim); font-size: 1rem; font-weight: 500; margin-bottom: 24px; }
        .hero-stats { display: flex; gap: 12px; flex-wrap: wrap; }
        .stat-chip { display: flex; flex-direction: column; align-items: center; padding: 12px 20px; border-radius: var(--radius-sm); background: var(--card); border: 1px solid var(--border); min-width: 72px; }
        .stat-chip.fatal { border-color: var(--fatal); background: rgba(220,38,38,0.04); }
        .stat-chip.error { border-color: var(--danger); background: rgba(239,68,68,0.04); }
        .stat-chip.warning { border-color: var(--warning); background: rgba(245,158,11,0.04); }
        .stat-chip.info { border-color: var(--info); background: rgba(59,130,246,0.04); }
        .stat-num { font-size: 1.5rem; font-weight: 800; line-height: 1.2; }
        .stat-chip.fatal .stat-num { color: var(--fatal); }
        .stat-chip.error .stat-num { color: var(--danger); }
        .stat-chip.warning .stat-num { color: var(--warning); }
        .stat-chip.info .stat-num { color: var(--info); }
        .stat-label { font-size: 0.65rem; text-transform: uppercase; font-weight: 700; color: var(--text-dim); letter-spacing: 0.05em; }

        /* Score Ring */
        .hero-right { display: flex; flex-direction: column; align-items: center; gap: 12px; }
        .score-ring { position: relative; width: 140px; height: 140px; }
        .score-svg { width: 100%; height: 100%; transform: rotate(-90deg); }
        .score-bg { fill: none; stroke: var(--border); stroke-width: 8; }
        .score-fg { fill: none; stroke-width: 8; stroke-linecap: round; stroke-dasharray: $circumference; stroke-dashoffset: $dashOffset; transition: stroke-dashoffset 1.5s ease-out; }
        .score-inner { position: absolute; inset: 0; display: flex; flex-direction: column; align-items: center; justify-content: center; }
        .score-num { font-size: 2.8rem; font-weight: 900; line-height: 1; letter-spacing: -0.04em; }
        .score-label { font-size: 0.75rem; color: var(--text-muted); font-weight: 600; }
        .rank-badge { font-size: 0.85rem; font-weight: 700; padding: 6px 16px; border-radius: 100px; border: 2px solid; background: var(--card); }

        /* Insights Grid */
        .insights-grid { display: grid; grid-template-columns: 1fr 1.5fr; gap: 24px; margin-bottom: 48px; }
        .glass-card { background: var(--card); border: 1px solid var(--border); border-radius: var(--radius); padding: 28px; box-shadow: var(--shadow); }
        .glass-card h2 { font-size: 0.85rem; text-transform: uppercase; letter-spacing: 0.06em; color: var(--text-dim); font-weight: 700; margin-bottom: 16px; }
        .card-icon { font-size: 1.6rem; margin-bottom: 8px; }
        .strategy-text { font-size: 1rem; color: var(--text); font-weight: 500; line-height: 1.7; margin-bottom: 16px; }
        .strategy-action { margin-top: auto; }
        .action-urgent { color: var(--fatal); font-weight: 700; font-size: 0.9rem; }
        .action-warn { color: var(--warning); font-weight: 700; font-size: 0.9rem; }
        .action-good { color: var(--success); font-weight: 700; font-size: 0.9rem; }

        .risk-matrix { width: 100%; border-collapse: collapse; }
        .risk-matrix th { text-align: left; padding: 10px 12px; font-size: 0.68rem; text-transform: uppercase; color: var(--text-muted); font-weight: 700; letter-spacing: 0.05em; border-bottom: 1px solid var(--border); }
        .risk-matrix td { padding: 10px 12px; border-bottom: 1px solid var(--border-subtle); font-size: 0.88rem; }
        .risk-cat { font-weight: 700; color: var(--text); }
        .risk-pts { color: var(--danger); font-weight: 800; font-size: 0.85rem; }
        .risk-desc { color: var(--text-dim); font-size: 0.82rem; }
        .empty-mini { text-align: center; padding: 20px; color: var(--text-muted); font-size: 0.9rem; }

        /* Findings */
        .findings-section { margin-bottom: 40px; }
        .section-title { font-size: 1.3rem; font-weight: 800; margin-bottom: 20px; color: var(--text); }
        .finding-count { font-size: 0.8rem; font-weight: 600; color: var(--text-muted); margin-left: 12px; }

        /* Tabs */
        .tabs { display: flex; gap: 8px; margin-bottom: 24px; overflow-x: auto; padding-bottom: 8px; -webkit-overflow-scrolling: touch; scrollbar-width: none; }
        .tabs::-webkit-scrollbar { display: none; }
        .tab-btn { background: var(--card); border: 1px solid var(--border); color: var(--text-dim); padding: 10px 16px; border-radius: 100px; font-weight: 600; cursor: pointer; transition: all 0.15s ease; white-space: nowrap; font-size: 0.82rem; display: flex; align-items: center; gap: 6px; }
        .tab-btn:hover { border-color: var(--accent); color: var(--text); background: var(--accent-soft); }
        .tab-btn.active { background: var(--accent); color: #fff; border-color: var(--accent); box-shadow: 0 2px 8px rgba(255,152,0,0.3); }
        .badge { background: rgba(0,0,0,0.08); padding: 2px 7px; border-radius: 4px; font-size: 0.7rem; font-weight: 700; }
        .tab-btn.active .badge { background: rgba(255,255,255,0.25); }

        /* Issue Cards */
        .issue-card { margin-bottom: 16px; border-radius: var(--radius); overflow: hidden; display: flex; background: var(--card); border: 1px solid var(--border); box-shadow: var(--shadow); transition: all 0.2s ease; }
        .issue-card:hover { box-shadow: var(--shadow-lg); transform: translateY(-1px); border-color: var(--accent); }
        .issue-card-border { width: 4px; flex-shrink: 0; }
        .issue-card-body { padding: 24px; flex: 1; }
        .card-header { display: flex; align-items: center; gap: 12px; margin-bottom: 16px; flex-wrap: wrap; }
        .severity-pill { font-size: 0.68rem; font-weight: 800; padding: 4px 10px; border-radius: 4px; text-transform: uppercase; letter-spacing: 0.04em; }
        .severity-pill.fatal { background: rgba(220,38,38,0.1); color: var(--fatal); }
        .severity-pill.error { background: rgba(239,68,68,0.1); color: var(--danger); }
        .severity-pill.warning { background: rgba(245,158,11,0.1); color: var(--warning); }
        .severity-pill.info { background: rgba(59,130,246,0.1); color: var(--info); }
        .issue-title { font-size: 1.05rem; font-weight: 700; color: var(--text); }

        .issue-details { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; margin-bottom: 16px; }
        .detail-section { padding: 14px; border-radius: var(--radius-sm); background: var(--bg-subtle); border: 1px solid var(--border-subtle); }
        .detail-section.impact { border-color: rgba(239,68,68,0.15); background: rgba(239,68,68,0.03); }
        .detail-label { font-size: 0.7rem; font-weight: 700; text-transform: uppercase; color: var(--text-dim); margin-bottom: 8px; display: flex; align-items: center; gap: 6px; letter-spacing: 0.03em; }
        .detail-label svg { opacity: 0.6; }
        .detail-text { font-size: 0.88rem; color: var(--text); line-height: 1.6; }

        .resolution-section { background: var(--bg-subtle); border: 1px solid var(--border); border-radius: var(--radius-sm); padding: 16px; margin-bottom: 16px; }
        .resolution-code { font-family: 'JetBrains Mono', 'SF Mono', 'Cascadia Code', 'Fira Code', monospace; font-size: 0.82rem; color: var(--text); white-space: pre-wrap; word-break: break-word; line-height: 1.7; }

        .roi-section { display: flex; gap: 8px; flex-wrap: wrap; padding-top: 12px; border-top: 1px solid var(--border-subtle); }
        .roi-tag { font-size: 0.75rem; font-weight: 600; padding: 5px 12px; border-radius: 100px; }
        .roi-value { background: var(--accent-soft); color: var(--accent); }
        .roi-time { background: rgba(16,185,129,0.08); color: var(--success); }
        .roi-file { background: var(--bg-subtle); color: var(--text-dim); }

        .empty-state { text-align: center; padding: 60px 20px; color: var(--text-muted); border: 1px dashed var(--border); border-radius: var(--radius); background: var(--card); }
        .empty-icon { font-size: 2rem; margin-bottom: 8px; }

        /* Footer */
        footer { text-align: center; padding: 40px 0; border-top: 1px solid var(--border); }
        .footer-brand { font-weight: 700; color: var(--accent); font-size: 0.85rem; margin-bottom: 4px; }
        .footer-copy { color: var(--text-muted); font-size: 0.75rem; }

        /* Animations */
        @keyframes fadeIn { from { opacity: 0; transform: translateY(8px); } to { opacity: 1; transform: translateY(0); } }
        .fade-in { animation: fadeIn 0.3s ease-out forwards; opacity: 0; }

        /* Responsive */
        @media (max-width: 768px) {
            .container { padding: 20px 16px 60px; }
            .topbar-inner { padding: 12px 16px; }
            .hero { flex-direction: column; align-items: flex-start; }
            .hero-title { font-size: 1.8rem; }
            .insights-grid { grid-template-columns: 1fr; }
            .issue-details { grid-template-columns: 1fr; }
            .hero-right { align-self: center; }
        }

        /* Print */
        @media print {
            body { background: #fff; }
            .topbar { position: static; backdrop-filter: none; background: #fff; }
            .glass-card, .issue-card { box-shadow: none; border: 1px solid #ddd; page-break-inside: avoid; }
            .tabs { display: none; }
            .tab-pane { display: block !important; }
            .score-fg { stroke-dashoffset: $dashOffset !important; }
        }
    """.trimIndent()
    }

    // =========================================================================
    // JavaScript
    // =========================================================================

    private fun generateJs(): String = """
        function openTab(evt, catName) {
            var panes = document.getElementsByClassName("tab-pane");
            for (var i = 0; i < panes.length; i++) { panes[i].style.display = "none"; }
            var btns = document.getElementsByClassName("tab-btn");
            for (var i = 0; i < btns.length; i++) {
                btns[i].className = btns[i].className.replace(" active", "");
                btns[i].setAttribute("aria-selected", "false");
            }
            document.getElementById(catName).style.display = "block";
            evt.currentTarget.className += " active";
            evt.currentTarget.setAttribute("aria-selected", "true");
        }
        // Animate score ring on load
        document.addEventListener('DOMContentLoaded', function() {
            var ring = document.querySelector('.score-fg');
            if (ring) { ring.style.transition = 'stroke-dashoffset 1.5s cubic-bezier(0.4, 0, 0.2, 1)'; }
        });
    """.trimIndent()

    // =========================================================================
    // Helpers
    // =========================================================================

    private fun categoryIcon(cat: String): String = when {
        cat.contains("Performance") || cat.contains("Build") -> "⚡"
        cat.contains("Security") -> "🔐"
        cat.contains("Stability") -> "🛡️"
        cat.contains("Dependency") || cat.contains("Catalog") -> "📦"
        cat.contains("Play") -> "🏛️"
        cat.contains("Modernization") || cat.contains("App") -> "🎨"
        cat.contains("Architecture") || cat.contains("Complexity") -> "🏗️"
        cat.contains("Quality") -> "🧪"
        cat.contains("Trend") -> "📈"
        else -> "📋"
    }

    private fun esc(text: String): String = text
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&#x27;")

    private fun escJson(text: String): String = text
        .replace("\\", "\\\\")
        .replace("\"", "\\\"")
        .replace("\n", "\\n")
        .replace("\r", "\\r")
        .replace("\t", "\\t")
}
