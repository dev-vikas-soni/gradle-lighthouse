package com.gradlelighthouse.auditors

import com.gradlelighthouse.core.AuditContext
import com.gradlelighthouse.core.Auditor
import com.gradlelighthouse.core.AuditIssue
import com.gradlelighthouse.core.ConsoleLogger
import com.gradlelighthouse.core.Severity

/**
 * ConflictIntelligenceAuditor: Detects binary incompatibility risks in the dependency graph.
 *
 * Gradle silently resolves conflicts by picking the highest version. However, if
 * Library A (v1) is forced to v2 by a transitive dependency, your app code
 * written for v1 may crash at runtime with NoSuchMethodError if v2 contains breaking changes.
 *
 * Uses pre-captured [ResolvedDependencySnapshot] data for Configuration Cache compatibility.
 */
class ConflictIntelligenceAuditor : Auditor {
    override val name: String = "ConflictIntelligence"

    override fun audit(context: AuditContext): List<AuditIssue> {
        val issues = mutableListOf<AuditIssue>()
        ConsoleLogger.auditorStart(name, "🛡️", "[SHIELD]", "Analyzing dependency graph for silent version jumps...")

        if (context.resolvedDependencies.isEmpty()) return issues

        context.resolvedDependencies.forEach { resolved ->
            if (isMajorVersionJump(resolved.requestedNotation, resolved.selectedVersion)) {
                issues.add(AuditIssue(
                    category = "Stability",
                    severity = Severity.FATAL,
                    title = "Silent Major Version Jump: ${resolved.selectedGroup}:${resolved.selectedName}",
                    reasoning = "Requested version '${resolved.requestedNotation}' was silently forced to '${resolved.selectedVersion}' by the dependency graph (Conflict Resolution).",
                    impactAnalysis = "Extremely high risk of runtime 'NoSuchMethodError' or 'ClassNotFoundException'. When a major version jumps (e.g., 1.x to 2.x), binary compatibility is usually broken. Since this happens silently at build time, your IDE might not show errors, but your production app will crash on specific code paths.",
                    resolution = "Align all modules to '${resolved.selectedVersion}' using a Version Catalog (TOML) or use a 'strictly' constraint in your build script to force a compatible version. Alternatively, identify and exclude the transitive dependency that is forcing this upgrade.",
                    roiAfterFix = "Elimination of non-deterministic runtime crashes and a predictable, stable dependency graph."
                ))
            }
        }

        return issues.distinctBy { it.title }
    }

    private fun isMajorVersionJump(requested: String, selected: String): Boolean {
        // Extract version from requested string (handles "group:name:version" format)
        val reqVer = if (requested.contains(":")) requested.substringAfterLast(":") else requested

        val reqMajor = reqVer.substringBefore(".").filter { it.isDigit() }
        val selMajor = selected.substringBefore(".").filter { it.isDigit() }

        if (reqMajor.isEmpty() || selMajor.isEmpty()) return false

        return try {
            selMajor.toInt() > reqMajor.toInt()
        } catch (_: NumberFormatException) {
            false
        }
    }
}
