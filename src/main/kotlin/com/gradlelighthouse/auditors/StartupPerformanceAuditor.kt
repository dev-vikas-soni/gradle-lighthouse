package com.gradlelighthouse.auditors

import com.gradlelighthouse.core.AuditContext
import com.gradlelighthouse.core.Auditor
import com.gradlelighthouse.core.AuditIssue
import com.gradlelighthouse.core.ConsoleLogger
import com.gradlelighthouse.core.Severity

/**
 * StartupPerformanceAuditor: Identifies "Time To Interactive" (TTI) killers.
 *
 * Many SDKs implicitly initialize themselves in onCreate() using ContentProviders or
 * manual calls. This piles up, causing the "Startup Stall" for the user.
 */
class StartupPerformanceAuditor : Auditor {
    override val name: String = "StartupPerformance"

    private val heavySDKs = mapOf(
        "com.google.firebase" to "Firebase (Auto-Init)",
        "com.google.android.gms:play-services-ads" to "AdMob (Blocking Init)",
        "com.segment.analytics" to "Segment (Network Init)",
        "com.appcenter" to "AppCenter (I/O Init)"
    )

    override fun audit(context: AuditContext): List<AuditIssue> {
        val issues = mutableListOf<AuditIssue>()
        ConsoleLogger.auditorStart(name, "🚀", "[PERF]", "Scanning for startup-performance bottlenecks...")

        // 1. Detect Heavy SDKs in dependencies
        val implementationDeps = context.dependencies
            .filter { it.configurationName.contains("implementation", ignoreCase = true) }

        val foundHeavySDKs = implementationDeps
            .filter { dep -> heavySDKs.keys.any { key -> dep.coordinate.contains(key) } }
            .map { dep ->
                val entry = heavySDKs.entries.first { dep.coordinate.contains(it.key) }
                "${entry.value} (${dep.coordinate})"
            }
            .distinct()

        if (foundHeavySDKs.isNotEmpty()) {
            issues.add(AuditIssue(
                category = "Performance",
                severity = Severity.WARNING,
                title = "Heavy Startup SDKs: ${foundHeavySDKs.size} Detected",
                reasoning = "The following libraries were detected: ${foundHeavySDKs.joinToString(", ")}. These SDKs are known to perform synchronous I/O or network calls during ContentProvider initialization sequence.",
                impactAnalysis = "Combined startup delay (App Start to TTI) of 250ms - 600ms on Tier 2/3 devices. This causes the OS to keep the splash screen visible longer, increasing 'Quick Exit' rates.",
                resolution = "Disable auto-initialization in the AndroidManifest.xml and use the 'androidx.startup' library or a custom background executor to initialize these SDKs after the first frame is rendered.",
                roiAfterFix = "Instant perceived performance boost for regional users and improved 'Retention' metrics."
            ))
        }

        // 2. Scan Manifest for ContentProvider auto-initializers
        context.sourceSets.forEach { sourceSet ->
            val manifestFile = sourceSet.manifestFile
            if (manifestFile != null && manifestFile.exists()) {
                try {
                    val content = manifestFile.readText()
                    if (content.contains("<provider") && content.contains("init", ignoreCase = true)) {
                        issues.add(AuditIssue(
                            category = "Performance",
                            severity = Severity.INFO,
                            title = "ContentProvider-based Auto-Init Detected",
                            reasoning = "AndroidManifest.xml contains providers typically used by modern libraries for 'magic' zero-config initialization.",
                            impactAnalysis = "These providers are initialized *before* the Application.onCreate() call, contributing to 'Main Thread Bloat' when your first Activity starts. This is a common silent performance killer.",
                            resolution = "Audit library documentation for a 'manual initialization' mode. Use Jetpack App Startup to orchestrate these dependencies in a more CPU-aware manner.",
                            roiAfterFix = "Improved 'Time To First Frame' by approximately 15-20% on cold starts.",
                            sourceFile = manifestFile.absolutePath
                        ))
                    }
                } catch (_: Exception) {
                    // Graceful degradation — don't fail if manifest can't be read
                }
            }
        }

        return issues
    }
}
