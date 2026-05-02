package com.gradlelighthouse.auditors

import com.gradlelighthouse.core.AuditContext
import com.gradlelighthouse.core.Auditor
import com.gradlelighthouse.core.AuditIssue
import com.gradlelighthouse.core.ConsoleLogger
import com.gradlelighthouse.core.Severity
import java.io.File
import java.util.Locale

/**
 * ManifestAuditor: Analyzes AndroidManifest.xml for security and performance risks.
 */
class ManifestAuditor : Auditor {
    override val name: String = "Manifest"

    override fun audit(context: AuditContext): List<AuditIssue> {
        val issues = mutableListOf<AuditIssue>()
        ConsoleLogger.auditorStart(name, "🛡️", "[SEC]", "Analyzing AndroidManifest.xml for security risks...")

        context.sourceSets.forEach { sourceSet ->
            val manifestFile = sourceSet.manifestFile
            if (manifestFile != null && manifestFile.exists()) {
                val content = manifestFile.readText()
                checkManifestContent(content, manifestFile, issues)
            }
        }

        return issues
    }

    private fun checkManifestContent(content: String, file: File, issues: MutableList<AuditIssue>) {
        if (content.contains("android:allowBackup=\"true\"")) {
            issues.add(AuditIssue(
                category = "Security",
                severity = Severity.WARNING,
                title = "Insecure Data Backup Enabled",
                reasoning = "The 'allowBackup' flag is set to true. This allows any user with USB debugging enabled to copy your app's private data via 'adb backup'.",
                impactAnalysis = "Sensitive user information (tokens, databases) can be extracted from the device without root access.",
                resolution = "Set 'android:allowBackup=\"false\"' in your <application> tag.",
                roiAfterFix = "Improved data privacy and protection against physical device access exploits.",
                sourceFile = file.absolutePath
            ))
        }

        if (content.contains("android:usesCleartextTraffic=\"true\"")) {
            issues.add(AuditIssue(
                category = "Security",
                severity = Severity.ERROR,
                title = "Cleartext Traffic Allowed",
                reasoning = "App allows HTTP (unencrypted) traffic. This is a violation of modern security standards.",
                impactAnalysis = "Susceptible to Man-in-the-Middle (MITM) attacks where network traffic can be intercepted and modified.",
                resolution = "Use HTTPS for all network calls or use a 'Network Security Configuration' to restrict cleartext traffic.",
                roiAfterFix = "Encrypted communication and protection against eavesdropping.",
                sourceFile = file.absolutePath
            ))
        }

        // Exported components without permission check
        val exportedRegex = Regex("<(activity|service|receiver|provider)[^>]+android:exported=\"true\"")
        val matches = exportedRegex.findAll(content)
        matches.forEach { match ->
            val tagContent = match.value
            if (!tagContent.contains("android:permission")) {
                val rawType = match.groupValues[1]
                val componentType = if (rawType.isNotEmpty()) rawType.substring(0, 1).toUpperCase() + rawType.substring(1) else rawType
                issues.add(AuditIssue(
                    category = "Security",
                    severity = Severity.ERROR,
                    title = "Publicly Exported $componentType",
                    reasoning = "A component is marked 'android:exported=\"true\"' without any custom permission requirement.",
                    impactAnalysis = "Other malicious apps on the device can start this component, potentially bypassing your app's internal logic or accessing sensitive UI.",
                    resolution = "Set 'android:exported=\"false\"' if the component is internal, or add an 'android:permission' attribute.",
                    roiAfterFix = "Hardened app sandbox and prevention of cross-app component hijacking.",
                    sourceFile = file.absolutePath
                ))
            }
        }
    }
}
