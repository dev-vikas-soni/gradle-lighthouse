package com.gradlelighthouse

import com.gradlelighthouse.extension.LighthouseExtension
import com.gradlelighthouse.task.LighthouseTask
import com.gradlelighthouse.task.LighthouseAggregateTask
import org.gradle.api.Action
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.ExternalDependency
import org.gradle.api.artifacts.repositories.MavenArtifactRepository
import org.gradle.api.artifacts.result.ResolvedDependencyResult
import java.io.File
import java.util.Properties

/**
 * Gradle Lighthouse: Enterprise-grade Build Intelligence Engine.
 * Hardened for Configuration Cache, Project Isolation, and Gradle 9.0 compatibility.
 */
class LighthousePlugin : Plugin<Project> {

    companion object {
        const val VERSION = "2.1.1"
    }

    override fun apply(project: Project) {
        if (project == project.rootProject) {
            println("[Lighthouse] 🛡️ Hardened Intelligence Engine V$VERSION initialized for ${project.name}")
        }

        val extension = project.extensions.create(
            "lighthouse",
            LighthouseExtension::class.java
        )

        project.tasks.register("lighthouseAudit", LighthouseTask::class.java, object : Action<LighthouseTask> {
            override fun execute(task: LighthouseTask) {
                configureLighthouseTask(project, task, extension)
            }
        })

        if (project == project.rootProject) {
            project.tasks.register("lighthouseAggregate", LighthouseAggregateTask::class.java, object : Action<LighthouseAggregateTask> {
                override fun execute(task: LighthouseAggregateTask) {
                    task.group = "Gradle Lighthouse"
                    task.description = "Aggregates intelligence reports from all modules into a Global Dashboard."
                    task.gradleVersionStr.set(project.gradle.gradleVersion)
                    task.pluginVersion.set(VERSION)
                    task.reportOutputDir.set(project.layout.buildDirectory.dir("reports/lighthouse"))

                    project.subprojects.forEach { sub ->
                        sub.plugins.withId("io.github.dev-vikas-soni.lighthouse") {
                            val subAuditTask = sub.tasks.named("lighthouseAudit", LighthouseTask::class.java)
                            task.dependsOn(subAuditTask)
                            task.moduleReportDirs.from(subAuditTask.flatMap { it.reportOutputDir })
                        }
                    }
                }
            })
        }
    }

    private fun configureLighthouseTask(project: Project, task: LighthouseTask, extension: LighthouseExtension) {
        // Capture static data
        task.moduleName.set(project.name)
        task.modulePath.set(project.path)
        task.moduleDirPath.set(project.projectDir.absolutePath)
        task.rootDirPath.set(project.rootDir.absolutePath)
        task.buildFilePath.set(project.buildFile.absolutePath)
        task.gradleVersionStr.set(project.gradle.gradleVersion)
        task.pluginVersion.set(VERSION)

        // Lazy content capture
        task.buildFileContent.set(project.provider {
            if (project.buildFile.exists()) project.buildFile.readText() else ""
        })

        task.hasVersionCatalog.set(project.provider {
            project.file("gradle/libs.versions.toml").exists() ||
            File(project.rootDir, "gradle/libs.versions.toml").exists()
        })

        task.pluginIds.set(project.provider {
            val ids = mutableSetOf<String>()
            project.plugins.forEach { ids.add(it.javaClass.name) }
            val pm = project.pluginManager
            listOf("kotlin-kapt", "com.google.devtools.ksp", "com.android.application", "com.android.library", "kotlin-multiplatform")
                .forEach { if (pm.hasPlugin(it)) ids.add(it) }
            ids
        })

        task.gradleProps.set(project.provider {
            val props = Properties()
            val file = File(project.rootDir, "gradle.properties")
            if (file.exists()) file.inputStream().use { props.load(it) }
            props.entries.associate { it.key.toString() to it.value.toString() }
        })

        task.repositoryData.set(project.provider {
            project.repositories.mapNotNull { repo ->
                if (repo is MavenArtifactRepository) "${repo.name}|${repo.url}" else "${repo.name}|local"
            }
        })

        // Complex providers: Capture dependencies and graph
        task.dependencyData.set(project.provider { captureDependencies(project, extension) })
        task.resolvedDependencyData.set(project.provider { captureResolvedDependencies(project, extension) })
        task.sourceSetData.set(project.provider { captureSourceSets(project) })
        task.moduleDependencyGraphData.set(project.provider { captureModuleDependencyGraph(project) })

        // Safe extension capturing for Configuration Cache
        val ext = extension
        task.enabledAuditorNames.set(project.provider {
            val enabled = mutableSetOf<String>()
            if (ext.enableDependencyHealth.get()) enabled.add("DependencyHealth")
            if (ext.enablePlayPolicy.get()) enabled.add("PlayStorePolicy")
            if (ext.enableCatalogMigration.get()) enabled.add("CatalogMigration")
            if (ext.enableBuildSpeed.get()) enabled.add("BuildSpeed")
            if (ext.enableAppSize.get()) enabled.add("AppSize")
            if (ext.enableStabilityCheck.get()) enabled.add("Stability")
            if (ext.enableConflictCheck.get()) enabled.add("ConflictIntelligence")
            if (ext.enableModernizationCheck.get()) enabled.add("Modernization")
            if (ext.enableKmpCheck.get()) enabled.add("KmpStructure")
            if (ext.enableConfigCacheCheck.get()) enabled.add("ConfigCacheReadiness")
            if (ext.enableModuleGraphCheck.get()) enabled.add("ModuleGraph")
            if (ext.enableUnusedDependencyCheck.get()) enabled.add("UnusedDependency")
            if (ext.enableTestCoverageCheck.get()) enabled.add("TestCoverage")
            if (ext.enableVersionCatalogHygiene.get()) enabled.add("VersionCatalogHygiene")
            if (ext.enableSecurityCheck.get()) enabled.add("Security")
            if (ext.enableModuleSizeCheck.get()) enabled.add("ModuleSize")
            if (ext.enableTrendTracking.get()) enabled.add("TrendTracking")
            enabled
        })

        task.failOnSeverityStr.set(ext.failOnSeverity)
        task.enableSarif.set(ext.enableSarifReport)
        task.enableJunitXml.set(ext.enableJunitXmlReport)
        task.reportOutputDir.set(project.layout.buildDirectory.dir("reports/lighthouse"))
    }

    private fun captureDependencies(project: Project, extension: LighthouseExtension): List<String> {
        val variant = extension.targetVariant.get().lowercase()
        val baseConfigs = setOf("implementation", "api", "compileOnly", "kapt", "ksp", "commonMainImplementation")

        return project.configurations.filter { config ->
            val name = config.name.lowercase()
            baseConfigs.any { name == it || (variant.isNotEmpty() && name.contains(variant) && name.contains(it)) }
        }.flatMap { config ->
            config.dependencies.filterIsInstance<ExternalDependency>().map { dep ->
                "${config.name}|${dep.group}|${dep.name}|${dep.version}"
            }
        }
    }

    private fun captureResolvedDependencies(project: Project, extension: LighthouseExtension): List<String> {
        val variant = extension.targetVariant.get().lowercase()
        val configNames = if (variant.isNotBlank()) listOf("${variant}RuntimeClasspath", "releaseRuntimeClasspath", "runtimeClasspath")
                          else listOf("releaseRuntimeClasspath", "runtimeClasspath")

        val config = configNames.mapNotNull { project.configurations.findByName(it) }.firstOrNull() ?: return emptyList()

        return try {
            config.incoming.resolutionResult.allDependencies.mapNotNull { dep ->
                if (dep is ResolvedDependencyResult) {
                    val selected = dep.selected.moduleVersion ?: return@mapNotNull null
                    "${dep.requested}|${selected.group}|${selected.name}|${selected.version}"
                } else null
            }.distinct()
        } catch (_: Exception) { emptyList() }
    }

    private fun captureSourceSets(project: Project): List<String> {
        val results = mutableListOf<String>()
        val sets = mapOf("main" to "src/main", "commonMain" to "src/commonMain", "androidMain" to "src/androidMain")

        sets.forEach { (name, path) ->
            val dir = File(project.projectDir, path)
            if (dir.exists()) {
                val k = File(dir, "kotlin").let { if (it.exists()) it.absolutePath else "" }
                val j = File(dir, "java").let { if (it.exists()) it.absolutePath else "" }
                val r = File(dir, "res").let { if (it.exists()) it.absolutePath else "" }
                val m = File(dir, "AndroidManifest.xml").let { if (it.exists()) it.absolutePath else "" }
                val a = File(dir, "assets").let { if (it.exists()) it.absolutePath else "" }
                results.add("$name|$k|$j|$r|$m|$a")
            }
        }
        return results
    }

    private fun captureModuleDependencyGraph(project: Project): List<String> {
        val results = mutableListOf<String>()
        val targets = if (project == project.rootProject) project.allprojects else setOf(project)

        targets.forEach { proj ->
            val deps = mutableSetOf<String>()
            proj.configurations.forEach { config ->
                try {
                    config.dependencies.forEach { dep ->
                        val cls = dep.javaClass
                        // Reflection-based extraction to bypass Gradle 9.0 ProjectDependency binary removal
                        if (cls.name.contains("ProjectDependency") || cls.interfaces.any { it.name.contains("ProjectDependency") }) {
                            try {
                                val path = cls.getMethod("getPath").invoke(dep) as? String
                                if (path != null) deps.add(path)
                            } catch (_: Exception) {
                                val s = dep.toString()
                                if (s.contains("project '")) deps.add(s.substringAfter("'").substringBefore("'"))
                            }
                        }
                    }
                } catch (_: Exception) { }
            }
            if (deps.isNotEmpty()) results.add("${proj.path}|${deps.joinToString(",")}")
        }
        return results
    }
}
