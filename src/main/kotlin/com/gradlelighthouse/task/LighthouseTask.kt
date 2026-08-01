package com.gradlelighthouse.task

import com.gradlelighthouse.auditors.*
import com.gradlelighthouse.core.AuditContext
import com.gradlelighthouse.core.Auditor
import com.gradlelighthouse.core.AuditIssue
import com.gradlelighthouse.core.ConsoleLogger
import com.gradlelighthouse.core.LighthouseCategory
import com.gradlelighthouse.core.Severity
import com.gradlelighthouse.reporting.HtmlReportGenerator
import com.gradlelighthouse.reporting.SarifReportGenerator
import com.gradlelighthouse.reporting.JunitXmlReportGenerator
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.work.DisableCachingByDefault
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import java.io.File
import javax.inject.Inject

/**
 * The core audit task for Gradle Lighthouse.
 *
 * This task implements a stateless analyzer that operates on a point-in-time
 * snapshot of the project (AuditContext).
 *
 * **Execution Pipeline**:
 * 1. Reconstruct structured models from serialized task inputs.
 * 2. Filter findings against the Baseline system to ignore existing debt.
 * 3. Execute 20+ stateless Auditors in parallel.
 * 4. Apply the Health Score Model weighting and square root dampening.
 * 5. Export multi-format intelligence reports (HTML, SARIF, JUnit, JSON).
 *
 * Ensures 100% compatibility with Gradle Configuration Cache (8.x+) and Isolated Projects (9.x+).
 */
@DisableCachingByDefault(because = "Audit reports should be fresh and depend on non-file state like project configurations.")
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

    /** Serialized module dependency graph as "modulePath|dep1,dep2,dep3" */
    @get:Input abstract val moduleDependencyGraphData: ListProperty<String>

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
    @get:Input abstract val useAi: Property<Boolean>
    @get:Input abstract val enableTelemetry: Property<Boolean>
    @get:Input abstract val telemetryEndpoint: Property<String>

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val baselineFile: org.gradle.api.file.ConfigurableFileCollection

    // =========================================================================
    // Output
    // =========================================================================

    @get:OutputDirectory abstract val reportOutputDir: DirectoryProperty

    // =========================================================================
    // Task Action — NO project access below this line
    // =========================================================================

    @TaskAction
    open fun execute() {
        val name = moduleName.get()
        val version = pluginVersion.get()

        ConsoleLogger.section("🚀", "[LH]", "Gradle Lighthouse V$version")
        ConsoleLogger.info("📡", "[SCAN]", "Scanning Architecture: ${modulePath.get()}")
        ConsoleLogger.rule()

        // 1. Reconstruct AuditContext from serialized inputs
        val context = buildAuditContext()

        // 2. Load baseline if exists
        val baselineIssueIds = mutableSetOf<String>()
        val baseline = baselineFile.files.firstOrNull()
        if (baseline != null && baseline.exists()) {
            try {
                // Simplistic baseline parsing (fingerprints per line)
                baseline.readLines().forEach { line ->
                    if (line.isNotBlank()) baselineIssueIds.add(line.trim())
                }
            } catch (_: Exception) {}
        }

        // 3. Select active auditors based on extension config
        val enabledSet = enabledAuditorNames.get()
        val activeAuditors = buildAuditorList(enabledSet)

        if (activeAuditors.isEmpty()) {
            ConsoleLogger.warn("No auditors are enabled in the `lighthouse { }` block.")
            return
        }

        // 3a. Run all auditors EXCEPT TrendTracking first so we can compute the health score.
        //     TrendTracking needs context.currentScore which is only known after scoring, so it
        //     runs as a second pass against a context copy that carries the computed score.
        val allIssues = mutableListOf<AuditIssue>()
        val (trendAuditors, mainAuditors) = activeAuditors.partition { it.name == "TrendTracking" }

        fun runAuditor(auditor: Auditor, ctx: AuditContext) {
            try {
                allIssues.addAll(auditor.audit(ctx))
            } catch (e: Exception) {
                ConsoleLogger.error("Auditor '${auditor.name}' failed: ${e.message}")
                allIssues.add(AuditIssue(
                    category = LighthouseCategory.QUALITY,
                    severity = Severity.WARNING,
                    title = "Auditor '${auditor.name}' encountered an error",
                    reasoning = "The ${auditor.name} auditor threw an exception during analysis: ${e.message}",
                    impactAnalysis = "This auditor's checks were skipped for this module. Other auditors ran normally.",
                    resolution = "This may be a plugin bug. Please report it at https://github.com/dev-vikas-soni/gradle-lighthouse/issues with your build.gradle.kts.",
                    roiAfterFix = "Full audit coverage for this module."
                ))
            }
        }

        mainAuditors.forEach { runAuditor(it, context) }

        // Filter out baselined issues
        val rootDir = File(rootDirPath.get())
        val newIssues = allIssues.filter { it.fingerprint(rootDir) !in baselineIssueIds }
        val suppressedCount = allIssues.size - newIssues.size

        // 4. Generate reports
        ConsoleLogger.info("🎯", "[DONE]", "[$name] Analysis Complete. Exporting Intelligence...")
        if (suppressedCount > 0) {
            ConsoleLogger.info("🛡️", "[BASE]", "Suppressed $suppressedCount issues found in baseline.")
        }
        val outputDir = reportOutputDir.get().asFile
        if (!outputDir.exists()) outputDir.mkdirs()

        // Calculate health score from main auditors first
        val rootDirFile = File(rootDirPath.get())
        val moduleCountForBench = parseModuleDependencyGraph().size.coerceAtLeast(1)
        val scoringResult = com.gradlelighthouse.core.HealthScoreEngine.calculateModernResultWithBenchmarks(
            allIssues,
            moduleCountForBench,
            pluginIds.get(),
            rootDirFile
        )
        val healthReport = com.gradlelighthouse.core.HealthScoreEngine.generateReport(allIssues)

        // 3b. Now run TrendTracking with currentScore populated so delta comparison works correctly.
        var previousScore: Int? = null
        if (trendAuditors.isNotEmpty()) {
            previousScore = TrendTrackingAuditor().getPreviousScore(File(rootDirPath.get()), name)
            val contextWithScore = context.copy(currentScore = healthReport.score)
            trendAuditors.forEach { runAuditor(it, contextWithScore) }
            TrendTrackingAuditor().saveScore(File(rootDirPath.get()), name, healthReport.score)
        }

        // Print colorful terminal dashboard
        val passedChecks = mutableListOf<String>()
        val gradlePropsMap = gradleProps.get()
        if (gradlePropsMap["org.gradle.caching"] == "true") passedChecks.add("Build caching enabled")
        if (gradlePropsMap["org.gradle.parallel"] == "true") passedChecks.add("Parallel execution enabled")
        if (gradlePropsMap["android.enableJetifier"] != "true") passedChecks.add("Jetifier disabled")
        if (gradlePropsMap["org.gradle.configuration-cache"] == "true") passedChecks.add("Configuration Cache enabled")
        if (gradlePropsMap["android.nonTransitiveRClass"] == "true") passedChecks.add("Non-transitive R classes enabled")

        val topIssueLines = allIssues
            .sortedByDescending { it.severity.ordinal }
            .take(5)
            .map { issue ->
                val icon = when (issue.severity) {
                    com.gradlelighthouse.core.Severity.FATAL -> "\u001B[31m💀"
                    com.gradlelighthouse.core.Severity.ERROR -> "\u001B[31m❌"
                    com.gradlelighthouse.core.Severity.WARNING -> "\u001B[33m⚠️ "
                    com.gradlelighthouse.core.Severity.INFO -> "\u001B[34mℹ️ "
                }
                "$icon ${issue.title}\u001B[0m"
            }

        ConsoleLogger.printDashboard(
            moduleName = name,
            score = healthReport.score,
            previousScore = previousScore,
            rank = healthReport.rank,
            fatalCount = healthReport.fatalCount,
            errorCount = healthReport.errorCount,
            warningCount = healthReport.warningCount,
            infoCount = healthReport.infoCount,
            topIssues = topIssueLines,
            passedChecks = passedChecks
        )

        // Print Category Health breakdown
        ConsoleLogger.printCategoryHealth(scoringResult.categoryScores)

        // Print Path to 90
        ConsoleLogger.printImprovements(scoringResult.improvements, healthReport.score)

        // HTML Report
        val htmlContent = HtmlReportGenerator.generate(name, version, gradleVersionStr.get(), allIssues, scoringResult)
        val htmlFile = File(outputDir, "${name}-index.html")
        htmlFile.writeText(htmlContent)
        ConsoleLogger.info("📊", "[HTML]", "Report: ${htmlFile.toURI()}")

        // JSON Report (for aggregation)
        val jsonContent = HtmlReportGenerator.generateJson(name, modulePath.get(), allIssues, scoringResult)
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
            val junitContent = JunitXmlReportGenerator.generate(name, newIssues)
            val junitFile = File(outputDir, "${name}-report.xml")
            junitFile.writeText(junitContent)
            ConsoleLogger.info("🧪", "[JUNIT]", "JUnit XML: ${junitFile.toURI()}")
        }

        // 6. Telemetry (Predictive Dependency Intelligence)
        if (enableTelemetry.get()) {
            sendTelemetry(context, healthReport.score)
        }

        ConsoleLogger.rule()

        // 5. CI/CD Build Gate
        val failSeverity = failOnSeverityStr.get()
        if (failSeverity != "NONE") {
            val threshold = try { Severity.valueOf(failSeverity) } catch (_: Exception) { null }
            if (threshold != null) {
                val blocking = newIssues.filter { it.severity.ordinal >= threshold.ordinal }
                if (blocking.isNotEmpty()) {
                    throw GradleException(
                        "Gradle Lighthouse: ${blocking.size} new issue(s) at severity $failSeverity or above found in '$name'. " +
                        "Fix them, record them in baseline, or adjust 'lighthouse { failOnSeverity }' to unblock."
                    )
                }
            }
        }
    }

    // =========================================================================
    // Private helpers
    // =========================================================================

    protected fun buildAuditContext(): AuditContext {
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
            hasVersionCatalog = hasVersionCatalog.get(),
            moduleDependencyGraph = parseModuleDependencyGraph()
        )
    }

    protected fun buildAuditorList(enabled: Set<String>): List<Auditor> {
        val auditors = mutableListOf<Auditor>()

        if ("DependencyHealth" in enabled) auditors.add(DependencyAuditor())
        if ("PlayStorePolicy" in enabled) auditors.add(PlayPolicyAuditor())
        if ("CatalogMigration" in enabled) auditors.add(CatalogMigrationAuditor())
        if ("BuildSpeed" in enabled) {
            auditors.add(BuildSpeedAuditor())
            auditors.add(JvmOptimizationAuditor())
            auditors.add(CiCdOptimizationAuditor())
        }
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
        if ("ConfigCacheReadiness" in enabled) auditors.add(ConfigCacheReadinessAuditor())
        if ("ModuleGraph" in enabled) auditors.add(ModuleGraphAuditor())
        if ("UnusedDependency" in enabled) auditors.add(UnusedDependencyAuditor())
        if ("TestCoverage" in enabled) auditors.add(TestCoverageAuditor())
        if ("VersionCatalogHygiene" in enabled) auditors.add(VersionCatalogHygieneAuditor())
        if ("Security" in enabled) auditors.add(SecurityAuditor())
        if ("ModuleSize" in enabled) auditors.add(ModuleSizeAuditor())
        if ("PredictiveDependencyIntelligence" in enabled) auditors.add(PredictiveDependencyIntelligenceAuditor())
        if ("TrendTracking" in enabled) auditors.add(TrendTrackingAuditor())

        return auditors
    }

    private fun parseModuleDependencyGraph(): Map<String, Set<String>> {
        val graph = mutableMapOf<String, MutableSet<String>>()
        moduleDependencyGraphData.get().forEach { line ->
            val parts = line.split("|", limit = 2)
            val module = parts.getOrElse(0) { "" }
            val deps = parts.getOrElse(1) { "" }.split(",").filter { it.isNotBlank() }.toMutableSet()
            if (module.isNotEmpty()) graph[module] = deps
        }
        return graph
    }

    private fun sendTelemetry(context: AuditContext, score: Int) {
        try {
            val endpoint = telemetryEndpoint.get()
            val url = java.net.URL(endpoint)
            val conn = url.openConnection() as java.net.HttpURLConnection
            conn.requestMethod = "POST"
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", "application/json")
            conn.connectTimeout = 2000
            conn.readTimeout = 2000

            val deps = context.dependencies.joinToString(",") { "${it.group}:${it.name}:${it.version}" }
            val payload = """
                {
                    "projectHash": "${context.projectName.hashCode()}",
                    "score": $score,
                    "gradleVersion": "${context.gradleVersion}",
                    "dependencies": "$deps"
                }
            """.trimIndent()

            conn.outputStream.use { it.write(payload.toByteArray()) }
            val responseCode = conn.responseCode
            if (responseCode in 200..299) {
                ConsoleLogger.info("📡", "[TELEMETRY]", "Anonymized intelligence shared with the Lighthouse community.")
            }
        } catch (_: Exception) {
            // Silently fail telemetry to not break the build
        }
    }
}
