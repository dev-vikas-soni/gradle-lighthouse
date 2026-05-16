package com.gradlelighthouse.auditors

import com.gradlelighthouse.core.AuditContext
import com.gradlelighthouse.core.Auditor
import com.gradlelighthouse.core.AuditIssue
import com.gradlelighthouse.core.ConsoleLogger
import com.gradlelighthouse.core.Severity
import java.io.File

class CiCdOptimizationAuditor : Auditor {
    override val name: String = "BuildSpeed"

    override fun audit(context: AuditContext): List<AuditIssue> {
        val issues = mutableListOf<AuditIssue>()
        
        // Only run this on the root project to avoid duplicating the issue in every module
        if (context.projectPath != ":") {
            return issues
        }
        
        ConsoleLogger.auditorStart(name, "☁️", "[SCAN]", "Analyzing CI/CD caching for ${context.projectName}")

        val cachingEnabled = context.gradleProperties["org.gradle.caching"] == "true"
        
        if (cachingEnabled) {
            val settingsKts = File(context.rootDir, "settings.gradle.kts")
            val settingsGroovy = File(context.rootDir, "settings.gradle")
            
            val settingsContent = when {
                settingsKts.exists() -> settingsKts.readText()
                settingsGroovy.exists() -> settingsGroovy.readText()
                else -> ""
            }

            val hasRemoteCache = settingsContent.contains("remote(") || settingsContent.contains("buildCache")
            
            if (!hasRemoteCache) {
                issues.add(AuditIssue(
                    category = name,
                    severity = Severity.WARNING,
                    title = "Remote Build Cache Not Configured",
                    reasoning = "Local caching (org.gradle.caching=true) is enabled, but no remote cache is configured in settings.gradle(.kts).",
                    impactAnalysis = "Ephemeral CI/CD agents (GitHub Actions, Bitrise) start with fresh disk state. Without a remote cache to pull from, your CI/CD builds gain zero benefit from caching and must rebuild from scratch every time.",
                    resolution = "Configure a remote build cache (e.g., Gradle Enterprise, AWS S3, or a custom HTTP cache node) in the buildCache {} block in settings.gradle.kts.",
                    roiAfterFix = "CI/CD build times reduced by up to 50-80% as nodes share task outputs."
                ))
            }
        }

        return issues
    }
}
