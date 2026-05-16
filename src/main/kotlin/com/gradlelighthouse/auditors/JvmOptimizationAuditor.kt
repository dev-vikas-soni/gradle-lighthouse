package com.gradlelighthouse.auditors

import com.gradlelighthouse.core.AuditContext
import com.gradlelighthouse.core.Auditor
import com.gradlelighthouse.core.AuditIssue
import com.gradlelighthouse.core.ConsoleLogger
import com.gradlelighthouse.core.Severity

class JvmOptimizationAuditor : Auditor {
    override val name: String = "BuildSpeed"

    override fun audit(context: AuditContext): List<AuditIssue> {
        val issues = mutableListOf<AuditIssue>()
        
        // Only run this at the root project level to avoid duplicate spam per module
        if (context.projectPath != ":") {
            return issues
        }
        
        ConsoleLogger.auditorStart(name, "🚀", "[SCAN]", "Analyzing JVM arguments for ${context.projectName}")

        val jvmArgs = context.gradleProperties["org.gradle.jvmargs"]

        if (jvmArgs == null) {
            issues.add(AuditIssue(
                category = name,
                severity = Severity.ERROR,
                title = "Missing JVM Memory & GC Tuning",
                reasoning = "The project does not define 'org.gradle.jvmargs'. Gradle will use default memory settings (usually 512MB), causing massive Garbage Collection (GC) thrashing on large projects.",
                impactAnalysis = "Gradle will spend more time running GC than compiling code. This leads to OutOfMemory errors and sluggish daemon performance.",
                resolution = "Add 'org.gradle.jvmargs=-Xmx4g -XX:+UseParallelGC' to your root gradle.properties file.",
                roiAfterFix = "Significant reduction in configuration and execution time, and a stable Gradle Daemon."
            ))
            return issues
        }

        // Check Heap Size
        if (!jvmArgs.contains("-Xmx")) {
            issues.add(AuditIssue(
                category = name,
                severity = Severity.ERROR,
                title = "Missing Max Heap Size (-Xmx)",
                reasoning = "JVM arguments are defined, but max heap size (-Xmx) is missing.",
                impactAnalysis = "Gradle defaults to low heap space, risking OutOfMemory errors in multi-module builds.",
                resolution = "Add '-Xmx4g' (or higher) to org.gradle.jvmargs.",
                roiAfterFix = "Prevents OOM crashes and reduces GC pauses."
            ))
        } else {
            // Regex to match -Xmx followed by digits and 'g' or 'm'
            val xmxMatch = Regex("-Xmx(\\d+)([gGmM])").find(jvmArgs)
            if (xmxMatch != null) {
                val value = xmxMatch.groupValues[1].toIntOrNull() ?: 0
                val unit = xmxMatch.groupValues[2].lowercase()
                
                val memoryInMb = if (unit == "g") value * 1024 else value
                if (memoryInMb < 4096) {
                    issues.add(AuditIssue(
                        category = name,
                        severity = Severity.WARNING,
                        title = "Low Max Heap Size Detected (${value}${unit.uppercase()})",
                        reasoning = "The configured heap size is less than 4GB, which is considered too low for modern Android/KMP builds.",
                        impactAnalysis = "Increased risk of GC thrashing and OutOfMemory during heavy tasks like R8 shrinking.",
                        resolution = "Increase heap size to at least '-Xmx4g'.",
                        roiAfterFix = "Faster build times and stable R8 execution."
                    ))
                }
            }
        }

        // Check Garbage Collector
        if (!jvmArgs.contains("-XX:+UseParallelGC") && !jvmArgs.contains("-XX:+UseG1GC")) {
            issues.add(AuditIssue(
                category = name,
                severity = Severity.INFO,
                title = "Suboptimal Garbage Collector",
                reasoning = "No modern Garbage Collector flag (-XX:+UseParallelGC or -XX:+UseG1GC) was found in jvmargs.",
                impactAnalysis = "The JVM might use default, single-threaded GCs, severely slowing down memory cleanup.",
                resolution = "Add '-XX:+UseParallelGC' to org.gradle.jvmargs.",
                roiAfterFix = "Faster parallel memory cleanup during build pauses."
            ))
        }

        return issues
    }
}
