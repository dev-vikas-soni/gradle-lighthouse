package com.gradlelighthouse.auditors

import com.gradlelighthouse.core.AuditContext
import com.gradlelighthouse.core.Auditor
import com.gradlelighthouse.core.AuditIssue
import com.gradlelighthouse.core.ConsoleLogger
import com.gradlelighthouse.core.Severity
import java.io.File

/**
 * VersionCatalogHygieneAuditor: Ensures proper usage of Gradle Version Catalogs.
 *
 * Checks for:
 * - Hardcoded versions in build files instead of using libs.versions.toml
 * - Unused entries in version catalog
 * - Suggestions for version catalog bundles
 */
class VersionCatalogHygieneAuditor : Auditor {
    override val name: String = "VersionCatalogHygiene"

    private val hardcodedVersionPattern = Regex("""["'][\w.]+:[\w\-]+:\d+[\w.\-]*["']""")

    override fun audit(context: AuditContext): List<AuditIssue> {
        val issues = mutableListOf<AuditIssue>()
        ConsoleLogger.auditorStart(name, "📋", "[TOML]", "Checking version catalog hygiene...")

        if (!context.hasVersionCatalog) return issues

        val buildContent = context.buildFileContent

        // 1. Detect hardcoded versions in build files
        val hardcodedDeps = hardcodedVersionPattern.findAll(buildContent)
            .map { it.value.trim('"', '\'') }
            .filter { !it.startsWith("//") }
            .toList()

        if (hardcodedDeps.isNotEmpty()) {
            issues.add(AuditIssue(
                category = "DependencyHygiene",
                severity = Severity.WARNING,
                title = "Hardcoded Dependencies Found (${hardcodedDeps.size}) — Use Version Catalog",
                reasoning = "Found ${hardcodedDeps.size} dependencies with hardcoded version strings instead of using libs.versions.toml aliases: ${hardcodedDeps.take(5).joinToString(", ")}${if (hardcodedDeps.size > 5) "..." else ""}",
                impactAnalysis = "Hardcoded versions across modules lead to version drift, inconsistencies, and painful upgrades. Dependabot/Renovate can't auto-update scattered version strings.",
                resolution = "Move all dependency declarations to gradle/libs.versions.toml and reference them as libs.* in build files. Example: implementation(libs.retrofit.core) instead of implementation(\"com.squareup.retrofit2:retrofit:2.9.0\").",
                roiAfterFix = "Centralized version management, automated dependency updates, consistent versions across all modules.",
                sourceFile = context.buildFile.absolutePath
            ))
        }

        // 2. Check for version catalog TOML file and scan for unused entries
        val tomlFile = File(context.rootDir, "gradle/libs.versions.toml")
        if (tomlFile.exists()) {
            try {
                val tomlContent = tomlFile.readText()
                val libraryAliases = parseTomlLibraries(tomlContent)

                // Scan all build files to find which catalog entries are used
                // We can only check the current module's build file here
                val usedAliases = libraryAliases.filter { alias ->
                    val dotNotation = "libs.${alias.replace("-", ".")}"
                    val dashNotation = "libs.${alias}"
                    buildContent.contains(dotNotation) || buildContent.contains(dashNotation)
                }

                // We report this only at root level to avoid false positives
                if (context.projectPath == ":") {
                    val potentiallyUnused = libraryAliases - usedAliases.toSet()
                    if (potentiallyUnused.size > libraryAliases.size * 0.3 && potentiallyUnused.isNotEmpty()) {
                        issues.add(AuditIssue(
                            category = "DependencyHygiene",
                            severity = Severity.INFO,
                            title = "Version Catalog May Have Unused Entries",
                            reasoning = "The version catalog defines ${libraryAliases.size} library entries. Some may not be used across the project. Run a full-project scan to identify dead entries.",
                            impactAnalysis = "Unused catalog entries add noise and confusion, making it harder for developers to know which libraries are available and actively used.",
                            resolution = "Review gradle/libs.versions.toml and remove entries not referenced by any module. Consider using the 'version-catalog-update' plugin for automated cleanup.",
                            roiAfterFix = "Cleaner catalog, less confusion during dependency upgrades.",
                            sourceFile = tomlFile.absolutePath
                        ))
                    }
                }

                // 3. Suggest bundles for related deps
                val bundleCandidates = detectBundleCandidates(libraryAliases)
                if (bundleCandidates.isNotEmpty() && context.projectPath == ":") {
                    issues.add(AuditIssue(
                        category = "DependencyHygiene",
                        severity = Severity.INFO,
                        title = "Version Catalog Bundle Opportunities (${bundleCandidates.size})",
                        reasoning = "Related dependencies could be grouped into bundles for easier usage: ${bundleCandidates.entries.take(3).joinToString(", ") { "${it.key} (${it.value.size} libs)" }}",
                        impactAnalysis = "Without bundles, developers must remember to add multiple related dependencies individually, risking incomplete setups.",
                        resolution = "Add [bundles] section in libs.versions.toml grouping related libraries. Example: compose = [\"compose-ui\", \"compose-material\", \"compose-tooling\"]",
                        roiAfterFix = "Simpler build files, consistent dependency sets across modules.",
                        sourceFile = tomlFile.absolutePath
                    ))
                }
            } catch (_: Exception) { }
        }

        return issues
    }

    private fun parseTomlLibraries(content: String): List<String> {
        val libraries = mutableListOf<String>()
        var inLibraries = false
        for (line in content.lines()) {
            val trimmed = line.trim()
            if (trimmed == "[libraries]") { inLibraries = true; continue }
            if (trimmed.startsWith("[") && trimmed != "[libraries]") { inLibraries = false; continue }
            if (inLibraries && trimmed.isNotEmpty() && !trimmed.startsWith("#")) {
                val alias = trimmed.substringBefore("=").trim()
                if (alias.isNotEmpty()) libraries.add(alias)
            }
        }
        return libraries
    }

    private fun detectBundleCandidates(aliases: List<String>): Map<String, List<String>> {
        return aliases.groupBy { alias ->
            alias.substringBefore("-").takeIf { prefix ->
                aliases.count { it.startsWith("$prefix-") } >= 3
            } ?: ""
        }.filter { it.key.isNotEmpty() && it.value.size >= 3 }
    }
}


