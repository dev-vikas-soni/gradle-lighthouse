package com.gradlelighthouse.core

import java.util.Locale

/**
 * Cross-platform console logger that safely handles emoji and Unicode output.
 * Supports colorful box-drawing for screenshot-worthy terminal dashboards.
 */
object ConsoleLogger {

    private val supportsUnicode: Boolean by lazy {
        val isWindowsTerminal = System.getenv("WT_SESSION") != null
        val isVsCodeTerminal = System.getenv("TERM_PROGRAM") == "vscode"
        val isUnixLike = System.getProperty("os.name")?.lowercase(Locale.ROOT)?.let { osName ->
            osName.contains("mac") || osName.contains("linux") || osName.contains("nix")
        } ?: false
        val hasCiUtf8 = System.getenv("CI") != null && System.getenv("LANG")?.contains("UTF-8") == true
        isWindowsTerminal || isVsCodeTerminal || isUnixLike || hasCiUtf8
    }

    // ANSI color codes
    private const val RESET = "\u001B[0m"
    private const val BOLD = "\u001B[1m"
    private const val DIM = "\u001B[2m"
    private const val RED = "\u001B[31m"
    private const val GREEN = "\u001B[32m"
    private const val YELLOW = "\u001B[33m"
    private const val BLUE = "\u001B[34m"
    private const val CYAN = "\u001B[36m"
    private const val WHITE = "\u001B[37m"
    private const val BG_RED = "\u001B[41m"
    private const val BG_GREEN = "\u001B[42m"
    private const val BG_YELLOW = "\u001B[43m"
    private const val BG_BLUE = "\u001B[44m"

    fun info(emoji: String, fallback: String, message: String) {
        println("${prefix(emoji, fallback)} $message")
    }

    fun success(message: String) { info("✅", "[OK]", message) }
    fun warn(message: String) { info("⚠️", "[WARN]", message) }
    fun error(message: String) { info("🛑", "[ERROR]", message) }

    fun section(emoji: String, fallback: String, title: String) {
        println("")
        println("${BOLD}${CYAN}${"═".repeat(60)}${RESET}")
        println("${BOLD}${prefix(emoji, fallback)} $title${RESET}")
        println("${BOLD}${CYAN}${"═".repeat(60)}${RESET}")
    }

    fun auditorStart(auditorName: String, emoji: String, fallback: String, action: String) {
        info(emoji, fallback, "${DIM}[$auditorName]${RESET} $action")
    }

    fun rule() { println("${CYAN}${"═".repeat(60)}${RESET}") }

    /**
     * Prints the screenshot-worthy terminal dashboard box.
     *
     * ┌─────────────────────────────────────────────────┐
     * │  🏗️  Gradle Lighthouse — Score: 72/100 (+8)     │
     * │  Rank: Standard → Expert 🎯                     │
     * ├─────────────────────────────────────────────────┤
     * │  ✅ Build caching enabled                        │
     * │  ⚠️  3 unused dependencies found                 │
     * │  ❌ Configuration cache not compatible (2 tasks) │
     * │  💡 Fix 2 errors to reach Expert rank            │
     * └─────────────────────────────────────────────────┘
     */
    fun printDashboard(
        @Suppress("UNUSED_PARAMETER") moduleName: String,
        score: Int,
        previousScore: Int?,
        rank: HealthScoreEngine.ArchitectRank,
        fatalCount: Int,
        errorCount: Int,
        warningCount: Int,
        infoCount: Int,
        topIssues: List<String>,
        passedChecks: List<String>
    ) {
        val width = 58
        val scoreColor = when {
            score >= 90 -> GREEN
            score >= 70 -> YELLOW
            score >= 50 -> YELLOW
            else -> RED
        }
        val deltaStr = if (previousScore != null) {
            val d = score - previousScore
            when {
                d > 0 -> " ${GREEN}(+${d})${RESET}"
                d < 0 -> " ${RED}(${d})${RESET}"
                else -> " (±0)"
            }
        } else ""

        val nextRank = HealthScoreEngine.ArchitectRank.values()
            .filter { it.minScore > score }
            .minByOrNull { it.minScore }

        val nextRankStr = if (nextRank != null) " → ${nextRank.displayName} 🎯" else " 🏆"

        println("")
        println("${BOLD}${CYAN}┌${"─".repeat(width)}┐${RESET}")
        printBoxLine("🏗️  Gradle Lighthouse — Score: ${scoreColor}${BOLD}$score/100${RESET}$deltaStr", width)
        printBoxLine("Rank: ${BOLD}${rank.displayName}${RESET}$nextRankStr", width)
        println("${CYAN}├${"─".repeat(width)}┤${RESET}")

        // Passed checks (max 3)
        passedChecks.take(3).forEach { check ->
            printBoxLine("${GREEN}✅ $check${RESET}", width)
        }

        // Top issues (max 5)
        topIssues.take(5).forEach { issue ->
            printBoxLine(issue, width)
        }

        // Summary line
        println("${CYAN}├${"─".repeat(width)}┤${RESET}")
        val totalIssues = fatalCount + errorCount + warningCount + infoCount
        val summaryParts = mutableListOf<String>()
        if (fatalCount > 0) summaryParts.add("${RED}${BOLD}${fatalCount} fatal${RESET}")
        if (errorCount > 0) summaryParts.add("${RED}${errorCount} error${RESET}")
        if (warningCount > 0) summaryParts.add("${YELLOW}${warningCount} warn${RESET}")
        if (infoCount > 0) summaryParts.add("${BLUE}${infoCount} info${RESET}")

        if (summaryParts.isEmpty()) {
            printBoxLine("${GREEN}${BOLD}Perfect score! No issues found.${RESET}", width)
        } else {
            printBoxLine("${BOLD}${totalIssues} issues:${RESET} ${summaryParts.joinToString(" · ")}", width)
        }

        if (nextRank != null && (fatalCount + errorCount) > 0) {
            printBoxLine("${CYAN}💡 Fix ${fatalCount + errorCount} issues to unlock ${nextRank.displayName}${RESET}", width)
        }

        println("${BOLD}${CYAN}└${"─".repeat(width)}┘${RESET}")
        println("")
    }

    private fun printBoxLine(content: String, width: Int) {
        // Strip ANSI codes to calculate visible length
        val visible = content.replace(Regex("\u001B\\[[;\\d]*m"), "")
        val padding = (width - 2 - visible.length).coerceAtLeast(0)
        println("${CYAN}│${RESET} $content${" ".repeat(padding)} ${CYAN}│${RESET}")
    }

    private fun prefix(emoji: String, fallback: String): String =
        if (supportsUnicode) emoji else fallback
}
