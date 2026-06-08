package com.gradlelighthouse.task

import com.gradlelighthouse.core.ConsoleLogger
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault
import java.io.File

/**
 * Automatically fixes deterministic architectural issues flagged by auditors.
 */
@DisableCachingByDefault(because = "Fixing tasks modify the project state directly and should not be cached.")
abstract class LighthouseFixTask : LighthouseTask() {

    init {
        description = "Automatically fixes deterministic architectural issues (e.g., gradle.properties settings)."
    }

    @TaskAction
    override fun execute() {
        val context = buildAuditContext()
        val enabledSet = enabledAuditorNames.get()
        val activeAuditors = buildAuditorList(enabledSet)

        val allIssues = mutableListOf<com.gradlelighthouse.core.AuditIssue>()
        activeAuditors.forEach { auditor ->
            try {
                allIssues.addAll(auditor.audit(context))
            } catch (_: Exception) {}
        }

        val fixableIssues = allIssues.filter { it.isFixable && it.fixId != null }
        if (fixableIssues.isEmpty()) {
            ConsoleLogger.info("✨", "[FIX]", "No fixable issues found. Your project is already following best practices!")
            return
        }

        ConsoleLogger.section("🛠️", "[FIX]", "Applying ${fixableIssues.size} fixes...")

        var appliedCount = 0
        fixableIssues.forEach { issue ->
            if (applyFix(issue.fixId!!)) {
                ConsoleLogger.info("✅", "[FIXED]", issue.title)
                appliedCount++
            }
        }

        ConsoleLogger.info("🎉", "[DONE]", "Applied $appliedCount fixes. Please sync Gradle and rebuild.")
    }

    private fun applyFix(fixId: String): Boolean {
        return try {
            when (fixId) {
                "enable_build_caching" -> updateGradleProperty("org.gradle.caching", "true")
                "enable_parallel_execution" -> updateGradleProperty("org.gradle.parallel", "true")
                "enable_non_transitive_r" -> updateGradleProperty("android.nonTransitiveRClass", "true")
                "disable_jetifier" -> updateGradleProperty("android.enableJetifier", "false")
                else -> false
            }
        } catch (_: Exception) {
            false
        }
    }

    private fun updateGradleProperty(key: String, value: String): Boolean {
        val propsFile = File(rootDirPath.get(), "gradle.properties")
        if (!propsFile.exists()) {
            propsFile.writeText("$key=$value\n")
            return true
        }

        val lines = propsFile.readLines().toMutableList()
        val existingIndex = lines.indexOfFirst { it.startsWith("$key=") || it.startsWith("#$key=") }

        if (existingIndex >= 0) {
            lines[existingIndex] = "$key=$value"
        } else {
            lines.add("$key=$value")
        }

        propsFile.writeText(lines.joinToString("\n") + "\n")
        return true
    }
}
