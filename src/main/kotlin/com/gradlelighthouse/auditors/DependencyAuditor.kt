package com.gradlelighthouse.auditors

import com.gradlelighthouse.core.AuditContext
import com.gradlelighthouse.core.Auditor
import com.gradlelighthouse.core.AuditIssue
import com.gradlelighthouse.core.ConsoleLogger
import com.gradlelighthouse.core.Severity

/**
 * DependencyAuditor: Detects dependency hygiene issues that cause non-deterministic builds.
 *
 * Checks for:
 * - Heavy legacy libraries redundant in Kotlin-first projects (Guava, Commons)
 * - Dynamic version declarations ("+", "latest.release") that break reproducibility
 * - Sunset repositories (JCenter) that cause random sync failures
 */
class DependencyAuditor : Auditor {
    override val name: String = "DependencyHealth"

    override fun audit(context: AuditContext): List<AuditIssue> {
        val issues = mutableListOf<AuditIssue>()
        ConsoleLogger.auditorStart(name, "🔍", "[SCAN]", "Analyzing dependencies for module: ${context.projectName}")

        // 1. Check for heavy legacy libraries
        context.dependencies.forEach { dep ->
            if (dep.group.contains("com.google.guava") || dep.name.contains("commons-lang")) {
                issues.add(AuditIssue(
                    category = name,
                    severity = Severity.WARNING,
                    title = "Heavy Legacy Library: ${dep.name}",
                    reasoning = "Guava and Apache Commons-Lang are pre-modern Java utility libraries that are largely redundant in Kotlin-first projects.",
                    impactAnalysis = "These libraries add significant APK bloat (approx. 2-3MB) and contribute thousands of methods, pushing the app closer to the 64k multidex limit. They also contain many duplicate functionalities already present in the Kotlin Standard Library.",
                    resolution = "Replace with native Kotlin Standard Library extensions (e.g., use .filter{}, .map{}, or string extensions) and eventually remove the dependency.",
                    roiAfterFix = "Smaller APK size, faster dexing times, and a cleaner, more idiomatic Kotlin codebase."
                ))
            }

            // 2. Check for dynamic versions
            val version = dep.version ?: ""
            if (version.contains("+") || version.contains("latest.release") || version.contains("latest.integration")) {
                issues.add(AuditIssue(
                    category = name,
                    severity = Severity.ERROR,
                    title = "Dynamic Version Detected: ${dep.coordinate}",
                    reasoning = "Using '+' or 'latest.release' in dependency versions prevents deterministic builds. This is a critical architectural risk.",
                    impactAnalysis = "This leads to 'Non-Deterministic Builds' where two developers running the same code get different binary results because a library auto-updated. It causes random CI/CD failures and production crashes that are impossible to reproduce locally.",
                    resolution = "Lock the dependency to a specific, strict semantic version (e.g., 1.2.3). Use a Version Catalog (TOML) to manage these versions centrally.",
                    roiAfterFix = "100% build reproducibility across all developer machines and CI/CD pipelines."
                ))
            }
        }

        // 3. Check for JCenter usage in repositories
        val usesJCenter = context.repositories.any { repo ->
            repo.name.contains("jcenter", ignoreCase = true) ||
            repo.url.contains("jcenter", ignoreCase = true)
        }
        if (usesJCenter) {
            issues.add(AuditIssue(
                category = name,
                severity = Severity.FATAL,
                title = "Sunset Repository: JCenter",
                reasoning = "JCenter is officially sunset by JFrog and its performance and reliability are severely degraded.",
                impactAnalysis = "High risk of sudden sync failures and artifact resolution timeouts. Many new library versions are no longer published to JCenter, leading to 'Could not resolve' errors.",
                resolution = "Remove 'jcenter()' from your root settings.gradle.kts and build.gradle.kts. Move all dependencies to 'mavenCentral()' or 'google()'.",
                roiAfterFix = "Faster, reliable Gradle syncs and access to the latest security patches for all libraries."
            ))
        }

        return issues
    }
}
