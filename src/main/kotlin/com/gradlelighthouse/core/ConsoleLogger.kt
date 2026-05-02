package com.gradlelighthouse.core

import java.util.Locale

/**
 * Cross-platform console logger that safely handles emoji and Unicode output.
 */
object ConsoleLogger {

    private val supportsUnicode: Boolean by lazy {
        val isWindowsTerminal = System.getenv("WT_SESSION") != null
        val isVsCodeTerminal = System.getenv("TERM_PROGRAM") == "vscode"
        val isUnixLike = System.getProperty("os.name")?.toLowerCase(Locale.ROOT)?.let { osName ->
            osName.contains("mac") || osName.contains("linux") || osName.contains("nix")
        } ?: false
        val hasCiUtf8 = System.getenv("CI") != null && System.getenv("LANG")?.contains("UTF-8") == true
        isWindowsTerminal || isVsCodeTerminal || isUnixLike || hasCiUtf8
    }

    fun info(emoji: String, fallback: String, message: String) {
        println("${prefix(emoji, fallback)} $message")
    }

    fun success(message: String) { info("✅", "[OK]", message) }
    fun warn(message: String) { info("⚠️", "[WARN]", message) }
    fun error(message: String) { info("🛑", "[ERROR]", message) }

    fun section(emoji: String, fallback: String, title: String) {
        println("")
        println("=".repeat(60))
        println("${prefix(emoji, fallback)} $title")
        println("=".repeat(60))
    }

    fun auditorStart(auditorName: String, emoji: String, fallback: String, action: String) {
        info(emoji, fallback, "[$auditorName] $action")
    }

    fun rule() { println("=".repeat(60)) }

    private fun prefix(emoji: String, fallback: String): String =
        if (supportsUnicode) emoji else fallback
}
