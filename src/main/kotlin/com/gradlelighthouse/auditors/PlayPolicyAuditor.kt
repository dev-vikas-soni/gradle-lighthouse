package com.gradlelighthouse.auditors

import com.gradlelighthouse.core.AuditContext
import com.gradlelighthouse.core.Auditor
import com.gradlelighthouse.core.AuditIssue
import com.gradlelighthouse.core.ConsoleLogger
import com.gradlelighthouse.core.Severity
import javax.xml.parsers.DocumentBuilderFactory
import org.w3c.dom.Element

/**
 * PlayPolicyAuditor: Scans for Google Play Store policy violations that cause rejections.
 *
 * Checks for:
 * - Dangerous permissions that trigger manual review or auto-rejection
 * - Missing android:exported flags (API 31+ compliance)
 * - Malformed manifest XML
 */
class PlayPolicyAuditor : Auditor {
    override val name: String = "PlayStorePolicy"

    private val dangerousPermissions = listOf(
        "android.permission.READ_SMS",
        "android.permission.RECEIVE_SMS",
        "android.permission.READ_CALL_LOG",
        "android.permission.WRITE_CALL_LOG",
        "android.permission.MANAGE_EXTERNAL_STORAGE",
        "android.permission.REQUEST_INSTALL_PACKAGES"
    )

    override fun audit(context: AuditContext): List<AuditIssue> {
        val issues = mutableListOf<AuditIssue>()
        ConsoleLogger.auditorStart(name, "🛡️", "[PLAY]", "Scanning Play Store Compliance...")

        // Check manifests across all source sets (KMP support)
        val manifestFiles = context.sourceSets.mapNotNull { it.manifestFile }.filter { it.exists() }

        if (manifestFiles.isEmpty()) return issues

        manifestFiles.forEach { manifestFile ->
            try {
                analyzeManifest(manifestFile, issues)
            } catch (e: Exception) {
                issues.add(AuditIssue(
                    category = name,
                    severity = Severity.FATAL,
                    title = "Manifest Parse Error",
                    reasoning = "The AndroidManifest.xml file contains malformed XML syntax at ${manifestFile.path}. Error: ${e.message}",
                    impactAnalysis = "Gradle cannot merge manifests, causing complete build failure and blocking all developer workflows.",
                    resolution = "Fix the XML syntax errors (e.g., missing closing tags or invalid attributes) in src/main/AndroidManifest.xml.",
                    roiAfterFix = "Successful build completion and correct manifest merging across all builds.",
                    sourceFile = manifestFile.absolutePath
                ))
            }
        }

        return issues
    }

    private fun analyzeManifest(manifestFile: java.io.File, issues: MutableList<AuditIssue>) {
        val factory = DocumentBuilderFactory.newInstance()
        // Security: Disable external entity processing (XXE prevention)
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false)
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false)

        val doc = factory.newDocumentBuilder().parse(manifestFile)
        doc.documentElement.normalize()

        // 1. Check dangerous permissions
        val permissions = doc.getElementsByTagName("uses-permission")
        for (i in 0 until permissions.length) {
            val permissionName = (permissions.item(i) as Element).getAttribute("android:name")
            if (dangerousPermissions.contains(permissionName)) {
                issues.add(AuditIssue(
                    category = name,
                    severity = Severity.ERROR,
                    title = "Dangerous Permission: $permissionName",
                    reasoning = "Google Play has strict policies on $permissionName. These are high-risk permissions that grant deep access to private user data.",
                    impactAnalysis = "Automated store rejection or significant manual review delays (up to 14 days). Non-compliance can lead to account-level strikes.",
                    resolution = "Verify if this permission is absolutely essential. Consider using alternative APIs like the SMS Retriever API or the generic system File Picker to avoid requiring sensitive permissions.",
                    roiAfterFix = "Reduced approval timelines, zero risk of store suspension, and significantly increased user privacy trust.",
                    sourceFile = manifestFile.absolutePath
                ))
            }
        }

        // 2. Check for missing android:exported
        val targetComponents = listOf("activity", "service", "receiver")
        targetComponents.forEach { tag ->
            val components = doc.getElementsByTagName(tag)
            for (i in 0 until components.length) {
                val element = components.item(i) as Element
                if (element.getElementsByTagName("intent-filter").length > 0 && !element.hasAttribute("android:exported")) {
                    val compName = element.getAttribute("android:name") ?: "Unknown"
                    issues.add(AuditIssue(
                        category = name,
                        severity = Severity.FATAL,
                        title = "Missing exported flag on <$tag> $compName",
                        reasoning = "In apps targeting Android 12+, components with intent filters must explicitly specify android:exported='true/false'.",
                        impactAnalysis = "Instant crash on app startup or when trying to launch the affected component on Android 12+ devices. Higher ANR/Crash rates on modern devices.",
                        resolution = "Explicitly add android:exported=\"true\" if the component should be reachable from outside the app, or \"false\" if it is internal.",
                        roiAfterFix = "A 100% crash-free launch experience on Android 12, 13, 14, and 15.",
                        sourceFile = manifestFile.absolutePath
                    ))
                }
            }
        }
    }
}
