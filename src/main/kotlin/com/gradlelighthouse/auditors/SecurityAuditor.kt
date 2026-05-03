package com.gradlelighthouse.auditors

import com.gradlelighthouse.core.AuditContext
import com.gradlelighthouse.core.Auditor
import com.gradlelighthouse.core.AuditIssue
import com.gradlelighthouse.core.ConsoleLogger
import com.gradlelighthouse.core.Severity
import java.io.File

/**
 * SecurityAuditor: Detects security vulnerabilities and compliance issues.
 *
 * Checks for:
 * - Hardcoded secrets in gradle.properties or build.gradle.kts
 * - Signing configs with plain text passwords
 * - Gradle wrapper version compliance
 * - Missing dependency locking
 * - JDK toolchain consistency
 */
class SecurityAuditor : Auditor {
    override val name: String = "Security"

    private val secretPatterns = listOf(
        Regex("""(?i)(password|secret|token|apikey|api_key|api\.key)\s*[=:]\s*["']?[^\s"'}{]+["']?"""),
        Regex("""(?i)(signing\.key|store\.password|key\.password)\s*=\s*[^\s]+""")
    )

    private val safePatterns = listOf(
        "System.getenv",
        "project.property",
        "providers.gradleProperty",
        "providers.environmentVariable",
        "findProperty",
        "local.properties",
        "\${",
        "KEYSTORE_PASSWORD",
        "System.env"
    )

    override fun audit(context: AuditContext): List<AuditIssue> {
        val issues = mutableListOf<AuditIssue>()
        ConsoleLogger.auditorStart(name, "🔐", "[SEC]", "Scanning for security issues...")

        // 1. Check for hardcoded secrets in gradle.properties
        val propsFile = File(context.rootDir, "gradle.properties")
        if (propsFile.exists()) {
            val propsContent = propsFile.readText()
            val secrets = secretPatterns.flatMap { it.findAll(propsContent).toList() }
                .filter { match -> safePatterns.none { safe -> match.value.contains(safe) } }
                .map { it.value.substringBefore("=").trim() }
                .distinct()

            if (secrets.isNotEmpty()) {
                issues.add(AuditIssue(
                    category = "Security",
                    severity = Severity.FATAL,
                    title = "Hardcoded Secrets Detected in gradle.properties",
                    reasoning = "Found potential secrets in gradle.properties: ${secrets.joinToString(", ")}. These may be committed to version control, exposing credentials.",
                    impactAnalysis = "Hardcoded secrets in version-controlled files can be extracted by anyone with repo access. Leaked signing keys allow malicious APK distribution. API keys enable unauthorized API usage and billing.",
                    resolution = "Move secrets to local.properties (gitignored), environment variables, or a secrets manager. Reference via: val password: String by project or System.getenv(\"KEY_PASSWORD\").",
                    roiAfterFix = "Eliminated credential exposure risk. Required for SOC2/ISO27001 compliance.",
                    sourceFile = propsFile.absolutePath
                ))
            }
        }

        // 2. Check build file for plain text signing config
        val buildContent = context.buildFileContent
        if (buildContent.contains("signingConfigs") && buildContent.contains("storePassword")) {
            val hasHardcodedPassword = secretPatterns.any { it.containsMatchIn(buildContent) } &&
                safePatterns.none { buildContent.contains(it) && buildContent.contains("storePassword") }

            if (hasHardcodedPassword) {
                issues.add(AuditIssue(
                    category = "Security",
                    severity = Severity.FATAL,
                    title = "Signing Config Contains Plain Text Passwords",
                    reasoning = "The build file contains signing configuration with what appears to be hardcoded passwords rather than environment variable or property references.",
                    impactAnalysis = "Signing keys and passwords in source control allow anyone to sign APKs as your organization. This is a critical supply chain security failure.",
                    resolution = "Use environment variables or local.properties for signing credentials: storePassword = System.getenv(\"KEYSTORE_PASSWORD\") ?: findProperty(\"KEYSTORE_PASSWORD\") as String",
                    roiAfterFix = "Secure build pipeline, Play Store signing integrity maintained.",
                    sourceFile = context.buildFile.absolutePath
                ))
            }
        }

        // 3. Check Gradle wrapper version
        val wrapperProps = File(context.rootDir, "gradle/wrapper/gradle-wrapper.properties")
        if (wrapperProps.exists()) {
            val wrapperContent = wrapperProps.readText()
            val versionMatch = Regex("""gradle-(\d+\.\d+(?:\.\d+)?)""").find(wrapperContent)
            val wrapperVersion = versionMatch?.groupValues?.get(1) ?: ""

            if (wrapperVersion.isNotEmpty()) {
                val major = wrapperVersion.split(".").firstOrNull()?.toIntOrNull() ?: 0
                val minor = wrapperVersion.split(".").getOrNull(1)?.toIntOrNull() ?: 0
                if (major < 8 || (major == 8 && minor < 5)) {
                    issues.add(AuditIssue(
                        category = "Security",
                        severity = Severity.WARNING,
                        title = "Gradle Wrapper Version Outdated ($wrapperVersion)",
                        reasoning = "Gradle version $wrapperVersion may have known security vulnerabilities and lacks performance improvements from newer versions. Current recommended: 8.10+.",
                        impactAnalysis = "Older Gradle versions may have path traversal or dependency confusion vulnerabilities. They also miss important performance features (Configuration Cache stability, faster dependency resolution).",
                        resolution = "Update via: ./gradlew wrapper --gradle-version=8.10.2 --distribution-type=bin",
                        roiAfterFix = "Security patches, performance improvements, and access to modern Gradle features.",
                        sourceFile = wrapperProps.absolutePath
                    ))
                }
            }
        }

        // 4. Check for dependency locking
        val hasLocking = buildContent.contains("dependencyLocking") || buildContent.contains("lockAllConfigurations")
        if (!hasLocking) {
            issues.add(AuditIssue(
                category = "Security",
                severity = Severity.INFO,
                title = "Dependency Locking Not Configured",
                reasoning = "No dependency locking is configured. Without locking, builds may resolve different transitive versions over time, leading to non-reproducible builds.",
                impactAnalysis = "A transitive dependency could be silently upgraded to a compromised version between builds. Dependency locking ensures byte-for-byte reproducible resolution.",
                resolution = "Enable locking: dependencyLocking { lockAllConfigurations() } and commit lockfiles to VCS. Run ./gradlew dependencies --write-locks to generate initial lockfiles.",
                roiAfterFix = "Reproducible builds, supply chain security, deterministic CI/CD.",
                sourceFile = context.buildFile.absolutePath
            ))
        }

        // 5. Check JDK toolchain configuration
        val hasToolchain = buildContent.contains("jvmToolchain") || buildContent.contains("JavaLanguageVersion")
        if (!hasToolchain) {
            issues.add(AuditIssue(
                category = "Security",
                severity = Severity.INFO,
                title = "JDK Toolchain Not Configured",
                reasoning = "No JDK toolchain is configured. Builds may use different JDK versions across developer machines and CI, leading to inconsistent bytecode and potential compatibility issues.",
                impactAnalysis = "Different JDK versions produce different bytecode optimizations. Without toolchain pinning, a developer on JDK 21 may produce incompatible artifacts for a JDK 17 target.",
                resolution = "Add kotlin { jvmToolchain(17) } or java { toolchain { languageVersion.set(JavaLanguageVersion.of(17)) } } to your build file.",
                roiAfterFix = "Consistent builds across all environments, automatic JDK provisioning.",
                sourceFile = context.buildFile.absolutePath
            ))
        }

        return issues
    }
}

