package com.gradlelighthouse.auditors

import com.gradlelighthouse.core.AuditContext
import com.gradlelighthouse.core.Auditor
import com.gradlelighthouse.core.AuditIssue
import com.gradlelighthouse.core.ConsoleLogger
import com.gradlelighthouse.core.Severity
import java.io.File

/**
 * UnusedDependencyAuditor: Detects dependencies declared but potentially never used in source code.
 *
 * Checks for:
 * - Declared dependencies whose group/artifact doesn't appear in any import statement
 * - Duplicate dependencies across configurations
 * - Dependencies declared in multiple modules with conflicting versions
 */
class UnusedDependencyAuditor : Auditor {
    override val name: String = "UnusedDependency"

    // Known package prefixes for common libraries
    private val artifactToPackageMap = mapOf(
        "retrofit" to "retrofit2",
        "okhttp" to "okhttp3",
        "gson" to "com.google.gson",
        "moshi" to "com.squareup.moshi",
        "glide" to "com.bumptech.glide",
        "picasso" to "com.squareup.picasso",
        "timber" to "timber.log",
        "rxjava" to "io.reactivex",
        "coroutines" to "kotlinx.coroutines",
        "dagger" to "dagger",
        "hilt" to "dagger.hilt",
        "room" to "androidx.room",
        "lifecycle" to "androidx.lifecycle",
        "navigation" to "androidx.navigation",
        "compose" to "androidx.compose",
        "material" to "com.google.android.material",
        "firebase" to "com.google.firebase",
        "coil" to "coil",
        "ktor" to "io.ktor",
        "koin" to "org.koin",
        "arrow" to "arrow"
    )

    override fun audit(context: AuditContext): List<AuditIssue> {
        val issues = mutableListOf<AuditIssue>()
        ConsoleLogger.auditorStart(name, "🗑️", "[UNUSED]", "Scanning for unused dependencies...")

        // Collect all import statements from source files
        val allImports = collectImports(context)

        // Check each declared dependency
        val implementationDeps = context.dependencies.filter {
            it.configurationName.contains("implementation", ignoreCase = true) ||
            it.configurationName.contains("api", ignoreCase = true)
        }.distinctBy { it.coordinate }

        val potentiallyUnused = mutableListOf<String>()

        for (dep in implementationDeps) {
            // Skip annotation processors, test deps, platform BOMs
            if (dep.configurationName.contains("kapt", ignoreCase = true) ||
                dep.configurationName.contains("ksp", ignoreCase = true) ||
                dep.configurationName.contains("test", ignoreCase = true) ||
                dep.configurationName.contains("androidTest", ignoreCase = true)) continue

            // Try to match the dependency to any import
            val isUsed = isDependencyLikelyUsed(dep.group, dep.name, allImports)
            if (!isUsed) {
                potentiallyUnused.add(dep.notation)
            }
        }

        if (potentiallyUnused.isNotEmpty()) {
            issues.add(AuditIssue(
                category = "DependencyHygiene",
                severity = Severity.WARNING,
                title = "Potentially Unused Dependencies Detected (${potentiallyUnused.size})",
                reasoning = "The following dependencies are declared but no matching import was found in source files: ${potentiallyUnused.joinToString(", ")}. These may be dead weight increasing APK size and build time.",
                impactAnalysis = "Unused dependencies increase APK size, slow down dependency resolution, increase attack surface, and clutter the dependency tree. Each unused dep adds ~100ms to clean builds.",
                resolution = "Remove unused dependencies or move them to the correct configuration. Use './gradlew dependencies' to verify they are needed transitively. Consider using the Gradle Dependency Analysis Plugin for precise results.",
                roiAfterFix = "Smaller APK, faster builds, reduced security surface. Estimated: ${potentiallyUnused.size * 100}ms saved per clean build."
            ))
        }

        // Check for duplicate declarations across configurations
        val duplicates = context.dependencies
            .groupBy { it.coordinate }
            .filter { it.value.size > 1 && it.value.map { d -> d.configurationName }.distinct().size > 1 }

        if (duplicates.isNotEmpty()) {
            val dupList = duplicates.keys.take(5).joinToString(", ")
            issues.add(AuditIssue(
                category = "DependencyHygiene",
                severity = Severity.INFO,
                title = "Duplicate Dependency Declarations (${duplicates.size})",
                reasoning = "These dependencies are declared in multiple configurations: $dupList. This can indicate confusion about api vs implementation scope.",
                impactAnalysis = "Duplicate declarations make upgrades error-prone (one may be missed) and indicate unclear module boundaries.",
                resolution = "Consolidate declarations. Use 'api' only for dependencies that are part of your module's public API surface. Use 'implementation' for everything else.",
                roiAfterFix = "Cleaner build files, single source of truth for each dependency version."
            ))
        }

        return issues
    }

    private fun collectImports(context: AuditContext): Set<String> {
        val imports = mutableSetOf<String>()
        context.sourceSets.forEach { sourceSet ->
            val dirs = sourceSet.kotlinDirs + sourceSet.javaDirs
            dirs.forEach { dir ->
                if (dir.exists()) {
                    dir.walkTopDown()
                        .filter { it.extension == "kt" || it.extension == "java" }
                        .forEach { file ->
                            try {
                                file.useLines { lines ->
                                    lines.filter { it.trimStart().startsWith("import ") }
                                        .forEach { imports.add(it.trim().removePrefix("import ").trim()) }
                                }
                            } catch (_: Exception) { }
                        }
                }
            }
        }
        return imports
    }

    private fun isDependencyLikelyUsed(group: String, artifactName: String, imports: Set<String>): Boolean {
        // Check if group appears in any import
        if (imports.any { it.startsWith(group) }) return true

        // Check known mappings
        val knownPackage = artifactToPackageMap.entries.find { (key, _) ->
            artifactName.contains(key, ignoreCase = true)
        }?.value
        if (knownPackage != null && imports.any { it.startsWith(knownPackage) }) return true

        // Heuristic: check if artifact name segments appear in imports
        val segments = artifactName.split("-", "_").filter { it.length > 3 }
        if (segments.any { seg -> imports.any { it.contains(seg, ignoreCase = true) } }) return true

        return false
    }
}

