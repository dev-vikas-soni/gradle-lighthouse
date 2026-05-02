package com.gradlelighthouse.auditors

import com.gradlelighthouse.core.AuditContext
import com.gradlelighthouse.core.Auditor
import com.gradlelighthouse.core.AuditIssue
import com.gradlelighthouse.core.ConsoleLogger
import com.gradlelighthouse.core.Severity
import java.io.File

/**
 * AppSizeAuditor: Identifies binary bloat sources that increase APK/AAB size.
 *
 * Checks for:
 * - Disabled code shrinking (R8/minifyEnabled)
 * - Disabled resource shrinking
 * - Legacy drawable folder overuse (bitmaps in default drawable/)
 * - Unoptimized large assets (>2MB bundled files)
 */
class AppSizeAuditor : Auditor {
    override val name: String = "AppSize"

    override fun audit(context: AuditContext): List<AuditIssue> {
        val issues = mutableListOf<AuditIssue>()
        ConsoleLogger.auditorStart(name, "📦", "[SIZE]", "Scrutinizing resource management and shrinking rules...")

        // 1. Check build file for shrinking configuration
        val content = context.buildFileContent
        if (content.isNotBlank()) {
            if (!(content.contains("isMinifyEnabled = true") || content.contains("minifyEnabled true"))) {
                issues.add(AuditIssue(
                    category = name,
                    severity = Severity.ERROR,
                    title = "Code Shrinking (R8) Disabled",
                    reasoning = "The 'isMinifyEnabled' flag is set to false or missing in your build script. This prevents R8 from performing tree-shaking and optimization.",
                    impactAnalysis = "Massive binary bloat (estimated 5MB - 15MB) caused by unused methods, classes, and library overhead. This leads to higher 'Cancel' rates during Play Store downloads, especially in emerging markets.",
                    resolution = "Set 'isMinifyEnabled = true' in your release build type. This will activate R8 to strip dead code and obfuscate your logic.",
                    roiAfterFix = "Significant binary size reduction, improved startup time due to smaller DEX files, and basic protection against reverse-engineering.",
                    sourceFile = context.buildFile.absolutePath
                ))
            }

            if (!(content.contains("isShrinkResources = true") || content.contains("shrinkResources true"))) {
                issues.add(AuditIssue(
                    category = name,
                    severity = Severity.ERROR,
                    title = "Resource Shrinking (ResGuard) Disabled",
                    reasoning = "Resource shrinking is disabled. R8 can remove dead code, but unused XML layouts, drawables, and raw assets require 'isShrinkResources' to be enabled.",
                    impactAnalysis = "Unreferenced assets (like unused 4K images or legacy layouts) are still bundled into the final APK, wasting user storage space and bandwidth.",
                    resolution = "Set 'isShrinkResources = true' in your release build type. Note: This requires 'isMinifyEnabled' to be true.",
                    roiAfterFix = "Optimized asset handling and a leaner, more modular APK package.",
                    sourceFile = context.buildFile.absolutePath
                ))
            }
        }

        // 2. Scan resource directories across all source sets
        context.sourceSets.forEach { sourceSet ->
            sourceSet.resDirs.forEach { resDir ->
                if (resDir.exists()) {
                    checkLegacyDrawables(resDir, issues)
                }
            }

            // 3. Check for large assets
            val assetsDir = sourceSet.assetsDir
            if (assetsDir != null && assetsDir.exists()) {
                checkLargeAssets(assetsDir, issues)
            }
        }

        return issues
    }

    private fun checkLegacyDrawables(resDir: File, issues: MutableList<AuditIssue>) {
        val legacyDrawable = File(resDir, "drawable")
        if (!legacyDrawable.exists()) return

        val bitmapExtensions = setOf("png", "jpg", "jpeg", "bmp")
        val bitmapCount = legacyDrawable.listFiles()
            ?.count { it.isFile && it.extension.toLowerCase(java.util.Locale.ROOT) in bitmapExtensions }
            ?: 0

        if (bitmapCount > 5) {
            issues.add(AuditIssue(
                category = name,
                severity = Severity.WARNING,
                title = "Legacy Drawable Folder Overuse ($bitmapCount files)",
                reasoning = "Detected $bitmapCount bitmap files in the default 'drawable/' folder instead of density-specific folders (xhdpi, xxhdpi) or Vector format.",
                impactAnalysis = "Forced runtime rescaling by the Android OS on modern high-res devices. This is a primary cause of 'Janky Frames' (dropped frames) and increased RAM usage during image loading.",
                resolution = "Migrate bitmaps to density-specific folders or, ideally, convert them to Vector Drawables (SVG/XML). For large photos, use the WebP format.",
                roiAfterFix = "Pixel-perfect UI across all screen sizes and minimized GPU memory overhead.",
                sourceFile = legacyDrawable.absolutePath
            ))
        }
    }

    private fun checkLargeAssets(assetsDir: File, issues: MutableList<AuditIssue>) {
        val largeFiles = mutableListOf<String>()
        assetsDir.walkTopDown().forEach { file ->
            if (file.isFile && file.length() > 2 * 1024 * 1024) { // 2MB+
                largeFiles.add("${file.name} (${String.format("%.1f", file.length() / 1024.0 / 1024.0)}MB)")
            }
        }

        if (largeFiles.isNotEmpty()) {
            issues.add(AuditIssue(
                category = name,
                severity = Severity.WARNING,
                title = "Unoptimized Assets: ${largeFiles.size} Large Files",
                reasoning = "The following large files were found in assets/: ${largeFiles.joinToString(", ")}. Large assets significantly contribute to 'App Overweight' issues.",
                impactAnalysis = "These files are bundled as-is with zero compression in the APK/AAB. This directly impacts the 'Install Success Rate' on the Google Play Store.",
                resolution = "Compress assets using MP3/WebP or consider using 'Play Asset Delivery' (PAD) for large data files (>10MB).",
                roiAfterFix = "Improved developer and user experience with smaller, more manageable project artifacts.",
                sourceFile = assetsDir.absolutePath
            ))
        }
    }
}
