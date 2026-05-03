package com.gradlelighthouse.task

import com.gradlelighthouse.auditors.*
import com.gradlelighthouse.core.AuditContext
import com.gradlelighthouse.core.Auditor
import com.gradlelighthouse.core.AuditIssue
import com.gradlelighthouse.core.ConsoleLogger
import com.gradlelighthouse.core.Severity
import com.gradlelighthouse.reporting.HtmlReportGenerator
import com.gradlelighthouse.reporting.SarifReportGenerator
import com.gradlelighthouse.reporting.JunitXmlReportGenerator
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import java.io.File
import javax.inject.Inject

/**
 * The core audit task for Gradle Lighthouse.
 *
 * All project data is captured during the **configuration phase** by [com.gradlelighthouse.LighthousePlugin]
 * and passed as task `@Input` properties. The `@TaskAction` method has **zero** `project` access,
 * ensuring full compatibility with Gradle Configuration Cache (8.x+) and Isolated Projects (9.x+).
 */
abstract class LighthouseTask @Inject constructor() : DefaultTask() {

    init {
        group = "Gradle Lighthouse"
        description = "Executes the 360° Android Project Health and Architecture Audit."
    }

    // =========================================================================
    // Inputs — All wired by LighthousePlugin during configuration phase
    // =========================================================================

    @get:Input abstract val moduleName: Property<String>
    @get:Input abstract val modulePath: Property<String>
    @get:Input abstract val buildFileContent: Property<String>
    @get:Input abstract val gradleVersionStr: Property<String>
    @get:Input abstract val pluginIds: SetProperty<String>
    @get:Input abstract val pluginVersion: Property<String>
    @get:Input abstract val gradleProps: MapProperty<String, String>
    @get:Input abstract val hasVersionCatalog: Property<Boolean>

    /** Serialized dependency data as "configName|group|name|version" */
    @get:Input abstract val dependencyData: ListProperty<String>

    /** Serialized resolved dependency data as "requested|selGroup|selName|selVersion" */
    @get:Input abstract val resolvedDependencyData: ListProperty<String>

    /** Serialized repository data as "name|url" */
    @get:Input abstract val repositoryData: ListProperty<String>

    /** Serialized source set data as "setName|kotlinDir|javaDir|resDir|manifestPath|assetsPath" */
    @get:Input abstract val sourceSetData: ListProperty<String>

    // Internal file paths (not for up-to-date checks, but serializable for CC)
    @get:Internal abstract val moduleDirPath: Property<String>
    @get:Internal abstract val rootDirPath: Property<String>
    @get:Internal abstract val buildFilePath: Property<String>

    // =========================================================================
    // Extension configuration
    // =========================================================================

    @get:Input abstract val enabledAuditorNames: SetProperty<String>
    @get:Input abstract val failOnSeverityStr: Property<String>
    @get:Input abstract val enableSarif: Property<Boolean>
    @get:Input abstract val enableJunitXml: Property<Boolean>

    // =========================================================================
    // Output
    // =========================================================================

    @get:OutputDirectory abstract val reportOutputDir: DirectoryProperty

    // =========================================================================
    // Task Action — NO project access below this line
    // =========================================================================

    @TaskAction
    fun execute() {
        val name = moduleName.get()
        val version = pluginVersion.get()

        ConsoleLogger.section("🚀", "[LH]", "Gradle Lighthouse V$version")
        ConsoleLogger.info("📡", "[SCAN]", "Scanning Architecture: ${modulePath.get()}")
        ConsoleLogger.rule()

        // 1. Reconstruct AuditContext from serialized inputs
        val context = buildAuditContext()

        // 2. Select active auditors based on extension config
        val enabledSet = enabledAuditorNames.get()
        val activeAuditors = buildAuditorList(enabledSet)

        if (activeAuditors.isEmpty()) {
            ConsoleLogger.warn("No auditors are enabled in the `lighthouse { }` block.")
            return
        }

        // 3. Execute auditors with error boundaries
        val allIssues = mutableListOf<AuditIssue>()
        activeAuditors.forEach { auditor ->
            try {
                val issues = auditor.audit(context)
                allIssues.addAll(issues)
            } catch (e: Exception) {
                ConsoleLogger.error("Auditor '${auditor.name}' failed: ${e.message}")
                allIssues.add(AuditIssue(
                    category = auditor.name,
                    severity = Severity.WARNING,
                    title = "Auditor '${auditor.name}' encountered an error",
                    reasoning = "The ${auditor.name} auditor threw an exception during analysis: ${e.message}",
                    impactAnalysis = "This auditor's checks were skipped for this module. Other auditors ran normally.",
                    resolution = "This may be a plugin bug. Please report it at https://github.com/dev-vikas-soni/gradle-lighthouse/issues with your build.gradle.kts.",
                    roiAfterFix = "Full audit coverage for this module."
                ))
            }
        }

        // 4. Generate reports
        ConsoleLogger.info("🎯", "[DONE]", "[$name] Analysis Complete. Exporting Intelligence...")
        val outputDir = reportOutputDir.get().asFile
        if (!outputDir.exists()) outputDir.mkdirs()

        // HTML Report
        val htmlContent = HtmlReportGenerator.generate(name, version, gradleVersionStr.get(), allIssues)
        val htmlFile = File(outputDir, "${name}-index.html")
        htmlFile.writeText(htmlContent)
        ConsoleLogger.info("📊", "[HTML]", "Report: ${htmlFile.toURI()}")

        // JSON Report (for aggregation)
        val jsonContent = HtmlReportGenerator.generateJson(name, modulePath.get(), allIssues)
        val jsonFile = File(outputDir, "module-report.json")
        jsonFile.writeText(jsonContent)

        // SARIF Report
        if (enableSarif.get()) {
            val sarifContent = SarifReportGenerator.generate(name, version, allIssues)
            val sarifFile = File(outputDir, "${name}-report.sarif")
            sarifFile.writeText(sarifContent)
            ConsoleLogger.info("🔒", "[SARIF]", "SARIF: ${sarifFile.toURI()}")
        }

        // JUnit XML Report
        if (enableJunitXml.get()) {
            val junitContent = JunitXmlReportGenerator.generate(name, allIssues)
            val junitFile = File(outputDir, "${name}-report.xml")
            junitFile.writeText(junitContent)
            ConsoleLogger.info("🧪", "[JUNIT]", "JUnit XML: ${junitFile.toURI()}")
        }

        ConsoleLogger.rule()

        // 5. CI/CD Build Gate
        val failSeverity = failOnSeverityStr.get()
        if (failSeverity != "NONE") {
            val threshold = try { Severity.valueOf(failSeverity) } catch (_: Exception) { null }
            if (threshold != null) {
                val blocking = allIssues.filter { it.severity.ordinal >= threshold.ordinal }
                if (blocking.isNotEmpty()) {
                    throw GradleException(
                        "Gradle Lighthouse: ${blocking.size} issue(s) at severity $failSeverity or above found in '$name'. " +
                        "Fix them or adjust 'lighthouseAuditor { failOnSeverity }' to unblock."
                    )
                }
            }
        }
    }

    // =========================================================================
    // Private helpers
    // =========================================================================

    private fun buildAuditContext(): AuditContext {
        val deps = dependencyData.get().map { line ->
            val parts = line.split("|", limit = 4)
            com.gradlelighthouse.core.DependencySnapshot(
                configurationName = parts.getOrElse(0) { "" },
                group = parts.getOrElse(1) { "" },
                name = parts.getOrElse(2) { "" },
                version = parts.getOrElse(3) { "" }.ifBlank { null }
            )
        }

        val resolvedDeps = resolvedDependencyData.get().map { line ->
            val parts = line.split("|", limit = 4)
            com.gradlelighthouse.core.ResolvedDependencySnapshot(
                requestedNotation = parts.getOrElse(0) { "" },
                selectedGroup = parts.getOrElse(1) { "" },
                selectedName = parts.getOrElse(2) { "" },
                selectedVersion = parts.getOrElse(3) { "" }
            )
        }

        val repos = repositoryData.get().map { line ->
            val parts = line.split("|", limit = 2)
            com.gradlelighthouse.core.RepositorySnapshot(
                name = parts.getOrElse(0) { "" },
                url = parts.getOrElse(1) { "" }
            )
        }

        val sourceSets = sourceSetData.get().map { line ->
            val parts = line.split("|", limit = 6)
            com.gradlelighthouse.core.SourceSetSnapshot(
                name = parts.getOrElse(0) { "main" },
                kotlinDirs = parts.getOrElse(1) { "" }.split(",").filter { it.isNotBlank() }.map { File(it) },
                javaDirs = parts.getOrElse(2) { "" }.split(",").filter { it.isNotBlank() }.map { File(it) },
                resDirs = parts.getOrElse(3) { "" }.split(",").filter { it.isNotBlank() }.map { File(it) },
                manifestFile = parts.getOrElse(4) { "" }.let { if (it.isNotBlank()) File(it) else null },
                assetsDir = parts.getOrElse(5) { "" }.let { if (it.isNotBlank()) File(it) else null }
            )
        }

        return AuditContext(
            projectName = moduleName.get(),
            projectPath = modulePath.get(),
            projectDir = File(moduleDirPath.get()),
            rootDir = File(rootDirPath.get()),
            buildFile = File(buildFilePath.get()),
            buildFileContent = buildFileContent.get(),
            gradleVersion = gradleVersionStr.get(),
            pluginIds = pluginIds.get(),
            dependencies = deps,
            resolvedDependencies = resolvedDeps,
            repositories = repos,
            gradleProperties = gradleProps.get(),
            sourceSets = sourceSets,
            hasVersionCatalog = hasVersionCatalog.get()
        )
    }

    private fun buildAuditorList(enabled: Set<String>): List<Auditor> {
        val auditors = mutableListOf<Auditor>()

        if ("DependencyHealth" in enabled) auditors.add(DependencyAuditor())
        if ("PlayStorePolicy" in enabled) auditors.add(PlayPolicyAuditor())
        if ("CatalogMigration" in enabled) auditors.add(CatalogMigrationAuditor())
        if ("BuildSpeed" in enabled) auditors.add(BuildSpeedAuditor())
        if ("AppSize" in enabled) auditors.add(AppSizeAuditor())
        if ("Stability" in enabled) {
            auditors.add(ProguardSafetyAuditor())
            auditors.add(ManifestAuditor())
        }
        if ("ConflictIntelligence" in enabled) auditors.add(ConflictIntelligenceAuditor())
        if ("Modernization" in enabled) {
            auditors.add(ModernizationAuditor())
            auditors.add(StartupPerformanceAuditor())
        }
        if ("KmpStructure" in enabled) auditors.add(KmpStructureAuditor())

        return auditors
    }
}
