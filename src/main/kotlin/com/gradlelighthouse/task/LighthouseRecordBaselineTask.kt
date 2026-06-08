package com.gradlelighthouse.task

import com.gradlelighthouse.core.ConsoleLogger
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault
import java.io.File

/**
 * Records all current architectural issues into a baseline file.
 * Subsequent 'lighthouseAudit' runs will ignore these issues, allowing
 * teams to focus on preventing NEW technical debt.
 */
@DisableCachingByDefault(because = "Baseline recording should always reflect the current state and is not intended to be cached.")
abstract class LighthouseRecordBaselineTask : LighthouseTask() {

    @get:OutputFile
    abstract val outputBaselineFile: RegularFileProperty

    init {
        description = "Records all current issues into a baseline file to suppress them in future runs."
    }

    @TaskAction
    override fun execute() {
        // 1. Run the standard audit logic but capture all issues
        // We override execute to change the behavior after audit

        // This is a bit tricky because LighthouseTask.execute() does a lot of things.
        // For simplicity in this implementation, we will perform a similar logic
        // but focus on writing the baseline.

        val version = pluginVersion.get()
        ConsoleLogger.section("🛡️", "[BASELINE]", "Recording Baseline for '${moduleName.get()}' (V$version)")

        val context = buildAuditContext()
        val enabledSet = enabledAuditorNames.get()
        val activeAuditors = buildAuditorList(enabledSet).filter { it.name != "TrendTracking" }

        val allIssues = mutableListOf<com.gradlelighthouse.core.AuditIssue>()
        activeAuditors.forEach { auditor ->
            try {
                allIssues.addAll(auditor.audit(context))
            } catch (_: Exception) {}
        }

        val rootDir = File(rootDirPath.get())
        val fingerprints = allIssues.map { it.fingerprint(rootDir) }.distinct()
        val outFile = outputBaselineFile.get().asFile

        outFile.parentFile.mkdirs()
        outFile.writeText(fingerprints.joinToString("\n"))

        ConsoleLogger.info("✅", "[DONE]", "Recorded ${fingerprints.size} issues to baseline: ${outFile.absolutePath}")
        ConsoleLogger.info("💡", "[TIP]", "Commit this file to your repository to share the baseline with your team.")
    }
}
