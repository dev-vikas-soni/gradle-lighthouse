package com.gradlelighthouse.auditors

import com.gradlelighthouse.core.AuditContext
import com.gradlelighthouse.core.Auditor
import com.gradlelighthouse.core.AuditIssue
import com.gradlelighthouse.core.ConsoleLogger
import com.gradlelighthouse.core.Severity
import java.io.File

/**
 * ProguardSafetyAuditor: Detects 'Invisible Landmines' caused by R8/ProGuard minification.
 *
 * Many libraries like Gson, Retrofit, and Moshi rely on reflection to parse JSON data.
 * If 'minifyEnabled' is true and the developer forgets to add '-keep' rules, the app
 * will build fine but crash on a signed release device with ClassNotFoundException or NPE.
 */
class ProguardSafetyAuditor : Auditor {
    override val name: String = "ProguardSafety"

    private val reflectionLibs = mapOf(
        "com.google.code.gson:gson" to "-keep class com.google.gson.** { *; }",
        "com.squareup.retrofit2:retrofit" to "-keep class retrofit2.** { *; }",
        "com.squareup.moshi:moshi" to "-keep class com.squareup.moshi.** { *; }",
        "com.fasterxml.jackson.core:jackson-databind" to "-keep class com.fasterxml.jackson.** { *; }",
        "io.reactivex.rxjava2:rxjava" to "-keep class io.reactivex.** { *; }",
        "io.reactivex.rxjava3:rxjava" to "-keep class io.reactivex.** { *; }"
    )

    override fun audit(context: AuditContext): List<AuditIssue> {
        val issues = mutableListOf<AuditIssue>()
        ConsoleLogger.auditorStart(name, "🛡️", "[SHIELD]", "Scanning for R8 reflection hazards...")

        // 1. Identify active reflection libraries
        val implementationDeps = context.dependencies.filter { dep ->
            dep.configurationName.contains("implementation", ignoreCase = true) ||
            dep.configurationName.contains("api", ignoreCase = true)
        }

        val usedReflectionLibs = implementationDeps.mapNotNull { dep ->
            val coord = dep.coordinate
            if (reflectionLibs.containsKey(coord)) coord to reflectionLibs[coord]!! else null
        }.toMap()

        if (usedReflectionLibs.isEmpty()) return issues

        // 2. Locate ProGuard rules files (check multiple standard locations)
        val proguardCandidates = listOf("proguard-rules.pro", "consumer-rules.pro", "proguard-android.txt")
        val proguardFile = proguardCandidates
            .map { File(context.projectDir, it) }
            .firstOrNull { it.exists() }

        if (proguardFile == null) {
            issues.add(AuditIssue(
                category = "Stability",
                severity = Severity.FATAL,
                title = "Critical: Missing ProGuard Rules Configuration",
                reasoning = "This module uses reflection-heavy libraries (${usedReflectionLibs.keys.joinToString()}) but no 'proguard-rules.pro' or 'consumer-rules.pro' file was found in the module root.",
                impactAnalysis = "Signed Release builds (APKs/AABs) will likely crash immediately upon any network or database operation. Because R8 obfuscates class names, libraries that rely on reflection will fail to find their targets, leading to 'ClassNotFoundException' or 'NoSuchMethodError'.",
                resolution = "Create a 'proguard-rules.pro' file and add the required '-keep' rules for your specific libraries.",
                roiAfterFix = "Prevents catastrophic production startup/runtime crashes in signed release builds.",
                sourceFile = context.projectDir.absolutePath
            ))
            return issues
        }

        // 3. Scan for missing rules
        val proguardContent = proguardFile.readText()
        usedReflectionLibs.forEach { (coord, requiredRule) ->
            val ruleStub = requiredRule.substringBefore("{").trim()
            if (!proguardContent.contains(ruleStub)) {
                issues.add(AuditIssue(
                    category = "Stability",
                    severity = Severity.FATAL,
                    title = "Missing R8 '-keep' Rule for $coord",
                    reasoning = "The library $coord is present in your implementation, but its required R8/ProGuard keep rule was not detected in '${proguardFile.name}'.",
                    impactAnalysis = "Classes and methods inside $coord will be renamed during obfuscation. Any code that tries to parse JSON into these classes will receive an empty object or crash the application with a 'NullPointerException' (NPE) in production.",
                    resolution = "Append the following rule to your ${proguardFile.name} file:\n\n$requiredRule\n\nAlso, ensure that your data model classes are marked with @Keep or their package is specifically excluded.",
                    roiAfterFix = "Deterministic stability in signed and optimized release builds.",
                    sourceFile = proguardFile.absolutePath
                ))
            }
        }

        return issues
    }
}
