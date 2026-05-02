package com.gradlelighthouse.auditors

import com.gradlelighthouse.core.AuditContext
import com.gradlelighthouse.core.Auditor
import com.gradlelighthouse.core.AuditIssue
import com.gradlelighthouse.core.ConsoleLogger
import com.gradlelighthouse.core.Severity
import java.io.File

/**
 * CatalogMigrationAuditor: Scans for legacy hardcoded dependency strings
 * and recommends migration to Gradle Version Catalogs (libs.versions.toml).
 *
 * Also supports auto-generating a `.generated` TOML file when enabled via extension.
 */
class CatalogMigrationAuditor : Auditor {
    override val name: String = "CatalogMigration"

    override fun audit(context: AuditContext): List<AuditIssue> {
        val issues = mutableListOf<AuditIssue>()
        ConsoleLogger.auditorStart(name, "📦", "[TOML]", "Scanning for hardcoded dependency strings...")

        if (!context.buildFile.exists()) return issues

        // If TOML already exists, module is considered "Modern"
        if (context.hasVersionCatalog) return issues

        val depRegex = Regex("""(?:implementation|api|compileOnly|kapt|ksp|testImplementation|debugImplementation)\s*(?:\(|)\s*["']([^:"]+):([^:"]+):([^"']+)["']\s*(?:\)|)""")
        val lines = context.buildFileContent.lines()
        val foundDeps = mutableListOf<Triple<String, String, String>>()

        lines.forEach { line ->
            depRegex.find(line)?.let { match ->
                val (group, artifact, version) = match.destructured
                // Only flag static version strings (not variables or dynamic +)
                if (!version.contains("$") && !version.contains("+")) {
                    foundDeps.add(Triple(group, artifact, version))
                }
            }
        }

        if (foundDeps.isNotEmpty()) {
            issues.add(AuditIssue(
                category = "Modernization",
                severity = Severity.WARNING,
                title = "Legacy Hardcoded Dependencies (${foundDeps.size})",
                reasoning = "Detected ${foundDeps.size} static dependency strings in ${context.buildFile.name}. Hardcoded strings are deprecated in favor of Version Catalogs (libs.versions.toml).",
                impactAnalysis = "This practice makes dependency versioning inconsistent across multi-module projects. It hides version conflicts and significantly increases build-script maintenance time. It also prevents Gradle from performing 'Version Alignment' logic efficiently.",
                resolution = "Create a 'gradle/libs.versions.toml' file and migrate all dependency declarations to use the type-safe accessor syntax (e.g., libs.androidx.core.ktx).",
                roiAfterFix = "Centralized version management, 100% type-safe dependency access, and faster IDE sync times due to unified versioning.",
                sourceFile = context.buildFile.absolutePath
            ))
        }

        return issues
    }
}
