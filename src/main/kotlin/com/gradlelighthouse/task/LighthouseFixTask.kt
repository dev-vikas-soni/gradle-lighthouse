package com.gradlelighthouse.task

import com.gradlelighthouse.core.ConsoleLogger
import com.gradlelighthouse.core.AuditIssue
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option
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

    @Option(option = "ai", description = "Enables AI-based remediation for complex issues.")
    fun setAiMode(enabled: String) {
        useAiOverride.set(enabled.toBoolean())
    }

    @get:Input
    abstract val useAiOverride: org.gradle.api.provider.Property<Boolean>

    @TaskAction
    override fun execute() {
        val context = buildAuditContext()
        val enabledSet = enabledAuditorNames.get()
        val activeAuditors = buildAuditorList(enabledSet)

        val allIssues = mutableListOf<AuditIssue>()
        activeAuditors.forEach { auditor ->
            try {
                allIssues.addAll(auditor.audit(context))
            } catch (_: Exception) {}
        }

        val isAiEnabled = useAiOverride.getOrElse(false) || useAi.get()
        val fixableIssues = allIssues.filter { it.isFixable && it.fixId != null }

        if (fixableIssues.isEmpty()) {
            ConsoleLogger.info("✨", "[FIX]", "No fixable issues found. Your project is already following best practices!")
            return
        }

        ConsoleLogger.section("🛠️", "[FIX]", "Applying ${fixableIssues.size} fixes (AI Mode: ${if (isAiEnabled) "ON" else "OFF"})...")

        var appliedCount = 0
        fixableIssues.forEach { issue ->
            val fixed = if (isAiEnabled) {
                applyAiFix(issue)
            } else {
                applyFix(issue.fixId!!)
            }

            if (fixed) {
                ConsoleLogger.info("✅", "[FIXED]", issue.title)
                appliedCount++
            }
        }

        ConsoleLogger.info("🎉", "[DONE]", "Applied $appliedCount fixes. Please sync Gradle and rebuild.")
    }

    private fun applyAiFix(issue: AuditIssue): Boolean {
        val fixId = issue.fixId ?: return false

        // In a real implementation, this would call an LLM API.
        // For this lighthouse-fix --ai MVP, we implement surgical pattern-based
        // remediation for complex issues that were previously "unfixable".

        return when (fixId) {
            "migrate_kapt_to_ksp" -> performKspMigration()
            "migrate_to_version_catalog" -> performVersionCatalogMigration()
            else -> applyFix(fixId) // Fallback to deterministic fixes
        }
    }

    private fun performKspMigration(): Boolean {
        val buildFile = File(buildFilePath.get())
        if (!buildFile.exists()) return false

        var content = buildFile.readText()

        // 1. Add KSP plugin if missing
        if (!content.contains("com.google.devtools.ksp")) {
            content = content.replace("plugins {", "plugins {\n    id(\"com.google.devtools.ksp\") version \"1.9.22-1.0.17\"")
        }

        // 2. Remove KAPT plugin
        content = content.replace(Regex("""(?:id|kotlin)\s*\(\s*["'](?:kotlin-|)kapt["']\s*\)(?:\s*version\s*["'][^"']+["'])?"""), "// id(\"kotlin-kapt\") removed by Lighthouse AI")

        // 3. Replace kapt(...) with ksp(...)
        content = content.replace(Regex("""kapt\s*\("""), "ksp(")

        buildFile.writeText(content)
        return true
    }

    private fun performVersionCatalogMigration(): Boolean {
        // Logic to move hardcoded dependencies to libs.versions.toml
        val rootDir = File(rootDirPath.get())
        val libsFile = File(rootDir, "gradle/libs.versions.toml")
        if (!libsFile.parentFile.exists()) libsFile.parentFile.mkdirs()

        val buildFile = File(buildFilePath.get())
        val content = buildFile.readText()

        val depRegex = Regex("""(?:implementation|api|compileOnly|kapt|ksp|testImplementation|debugImplementation)\s*(?:\(|)\s*["']([^: "\n]+):([^: "\n]+):([^"'\n]+)["']\s*(?:\)|)""")
        val matches = depRegex.findAll(content)

        if (matches.none()) return false

        val tomlLibraries = mutableListOf<String>()
        matches.forEach { match ->
            val (group, artifact, version) = match.destructured
            val alias = "${group.replace(".", "-")}-${artifact.replace(".", "-")}"
            tomlLibraries.add("$alias = { group = \"$group\", name = \"$artifact\", version = \"$version\" }")
        }

        val tomlContent = """
            [versions]

            [libraries]
            ${tomlLibraries.joinToString("\n")}

            [plugins]
        """.trimIndent()

        libsFile.writeText(tomlContent)

        // Surgical replacement in build file
        var newContent = content
        matches.forEach { match ->
            val (group, artifact, version) = match.destructured
            val alias = "${group.replace(".", "-")}-${artifact.replace(".", "-")}"
            val notation = "$group:$artifact:$version"
            newContent = newContent.replace("\"$notation\"", "libs.$alias".replace("-", "."))
            newContent = newContent.replace("'$notation'", "libs.$alias".replace("-", "."))
        }
        buildFile.writeText(newContent)

        ConsoleLogger.info("🤖", "[AI]", "Generated 'gradle/libs.versions.toml' and updated dependencies in ${buildFile.name}")

        return true
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
