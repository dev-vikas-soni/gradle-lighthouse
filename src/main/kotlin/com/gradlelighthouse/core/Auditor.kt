package com.gradlelighthouse.core

import java.io.Serializable

/**
 * Severity levels for audit findings.
 *
 * Each level carries a CSS color for HTML reports, an emoji for rich terminals,
 * and an ASCII fallback for Windows cmd.exe / non-UTF8 environments.
 */
enum class Severity(
    val color: String,
    val icon: String,
    val asciiIcon: String
) {
    INFO("#3b82f6", "ℹ️", "[INFO]"),
    WARNING("#f59e0b", "⚠️", "[WARN]"),
    ERROR("#ef4444", "🛑", "[ERR]"),
    FATAL("#dc2626", "💀", "[FATAL]")
}

/**
 * A structured audit finding with full context for reporting.
 *
 * Every field is designed to be directly renderable in HTML, SARIF, and JUnit XML
 * without additional transformation. All text content MUST be plain text — HTML
 * escaping is handled by the report generators, not by auditors.
 */
data class AuditIssue(
    /** Top-level category (e.g., "Stability", "Performance", "Modernization") */
    val category: String,

    /** Severity level determining score impact and CI gate behavior */
    val severity: Severity,

    /** Concise, human-readable title (max ~80 chars recommended) */
    val title: String,

    /** Technical explanation of WHY this is a problem */
    val reasoning: String,

    /** What happens in production if this is NOT fixed */
    val impactAnalysis: String,

    /** Step-by-step fix instructions (code snippets welcome as plain text) */
    val resolution: String,

    /** Quantified benefit after applying the fix */
    val roiAfterFix: String,

    /** Optional: the file path where this issue was detected */
    val sourceFile: String? = null,

    /** Optional: line number in the source file */
    val sourceLine: Int? = null
) : Serializable {
    companion object {
        private const val serialVersionUID = 1L
    }
}

/**
 * Contract for all Gradle Lighthouse auditors.
 *
 * Auditors receive an [AuditContext] (a serializable project snapshot) instead of
 * a Gradle [org.gradle.api.Project] to ensure Configuration Cache compatibility.
 *
 * ## Implementation Guidelines
 * - Auditors MUST be stateless — all data comes from [AuditContext]
 * - Auditors MUST NOT access the filesystem outside of paths provided in [AuditContext]
 * - Auditors SHOULD handle missing files/data gracefully (return empty list)
 * - Auditors MUST return plain-text content in [AuditIssue] fields (no HTML)
 */
interface Auditor {

    /** Human-readable name displayed in console logs and reports */
    val name: String

    /**
     * Executes the audit against the provided project snapshot.
     *
     * @param context Serializable snapshot of the project state
     * @return List of findings, empty if the module passes all checks
     */
    fun audit(context: AuditContext): List<AuditIssue>
}
