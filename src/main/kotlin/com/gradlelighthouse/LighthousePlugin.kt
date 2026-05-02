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
 * Gradle Lighthouse Plugin Entry Point.
 */
class LighthousePlugin : Plugin<Project> {

    override fun apply(project: Project) {

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
                    task.reportOutputDir.set(project.layout.buildDirectory.dir("reports/lighthouse"))

                    project.subprojects.forEach { sub ->
                        sub.plugins.withId("com.gradlelighthouse.plugin") {
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
        task.moduleName.set(project.name)
        task.modulePath.set(project.path)
        task.moduleDirPath.set(project.projectDir.absolutePath)
        task.rootDirPath.set(project.rootDir.absolutePath)
        task.buildFilePath.set(project.buildFile.absolutePath)
        task.gradleVersionStr.set(project.gradle.gradleVersion)

        task.buildFileContent.set(project.provider {
            if (project.buildFile.exists()) project.buildFile.readText() else ""
        })

        task.hasVersionCatalog.set(project.provider {
            val localToml = project.file("gradle/libs.versions.toml")
            val rootToml = File(project.rootDir, "gradle/libs.versions.toml")
            localToml.exists() || rootToml.exists()
        })

        task.pluginIds.set(project.provider {
            project.plugins.mapNotNull { plugin ->
                plugin.javaClass.name
            }.toSet() + project.pluginManager.let { pm ->
                val knownIds = listOf(
                    "kotlin-kapt", "com.google.devtools.ksp",
                    "com.android.application", "com.android.library",
                    "org.jetbrains.kotlin.multiplatform", "kotlin-multiplatform"
                )
                knownIds.filter { id ->
                    try { pm.hasPlugin(id) } catch (_: Exception) { false }
                }.toSet()
            }
        })

        task.gradleProps.set(project.provider {
            val propsFile = File(project.rootDir, "gradle.properties")
            if (propsFile.exists()) {
                val props = Properties()
                propsFile.inputStream().use { props.load(it) }
                props.entries.associate { it.key.toString() to it.value.toString() }
            } else {
                emptyMap()
            }
        })

        task.repositoryData.set(project.provider {
            project.repositories.mapNotNull { repo ->
                when (repo) {
                    is MavenArtifactRepository -> "${repo.name}|${repo.url}"
                    else -> "${repo.name}|${repo.name}"
                }
            }
        })

        task.dependencyData.set(project.provider {
            val variant = extension.targetVariant.get()
            val baseConfigNames = setOf(
                "implementation", "api", "compileOnly",
                "kapt", "ksp", "commonMainImplementation", "commonMainApi",
                "androidMainImplementation", "androidMainApi"
            )
            
            project.configurations
                .filter { config ->
                    val name = config.name.toLowerCase()
                    if (variant.isNotBlank()) {
                        val v = variant.toLowerCase()
                        // If variant is "release", include "implementation" (base) and "releaseImplementation" (variant specific).
                        // Exclude "debugImplementation".
                        val isBase = baseConfigNames.any { name == it.toLowerCase() }
                        val isVariantSpecific = name.contains(v)
                        isBase || isVariantSpecific
                    } else {
                        // Default: include all base configs and any configs containing them (e.g. debugImplementation)
                        baseConfigNames.any { name.contains(it.toLowerCase()) }
                    }
                }
                .flatMap { config ->
                    config.dependencies.filterIsInstance<ExternalDependency>().map { dep ->
                        "${config.name}|${dep.group ?: ""}|${dep.name}|${dep.version ?: ""}"
                    }
                }
        })

        task.resolvedDependencyData.set(project.provider {
            captureResolvedDependencies(project)
        })

        task.sourceSetData.set(project.provider {
            captureSourceSets(project)
        })

        task.enabledAuditorNames.set(project.provider {
            val enabled = mutableSetOf<String>()
            if (extension.enableDependencyHealth.get()) enabled.add("DependencyHealth")
            if (extension.enablePlayPolicy.get()) enabled.add("PlayStorePolicy")
            if (extension.enableCatalogMigration.get()) enabled.add("CatalogMigration")
            if (extension.enableBuildSpeed.get()) enabled.add("BuildSpeed")
            if (extension.enableAppSize.get()) enabled.add("AppSize")
            if (extension.enableStabilityCheck.get()) enabled.add("Stability")
            if (extension.enableConflictCheck.get()) enabled.add("ConflictIntelligence")
            if (extension.enableModernizationCheck.get()) enabled.add("Modernization")
            if (extension.enableKmpCheck.get()) enabled.add("KmpStructure")
            enabled
        })

        task.failOnSeverityStr.set(extension.failOnSeverity)
        task.enableSarif.set(extension.enableSarifReport)
        task.enableJunitXml.set(extension.enableJunitXmlReport)

        task.reportOutputDir.set(project.layout.buildDirectory.dir("reports/lighthouse"))
    }

    private fun captureResolvedDependencies(project: Project): List<String> {
        val configNames = listOf("releaseRuntimeClasspath", "runtimeClasspath")
        val config = configNames.mapNotNull { project.configurations.findByName(it) }.firstOrNull()
            ?: return emptyList()

        return try {
            val result = config.incoming.resolutionResult
            result.allDependencies.mapNotNull { dep ->
                if (dep is ResolvedDependencyResult) {
                    val selected = dep.selected.moduleVersion ?: return@mapNotNull null
                    val requested = dep.requested.toString()
                    "$requested|${selected.group}|${selected.name}|${selected.version}"
                } else null
            }.distinct()
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun captureSourceSets(project: Project): List<String> {
        val results = mutableListOf<String>()
        val projectDir = project.projectDir

        val standardSets = mapOf(
            "main" to "src/main",
            "commonMain" to "src/commonMain",
            "androidMain" to "src/androidMain",
            "iosMain" to "src/iosMain",
            "desktopMain" to "src/desktopMain",
            "jsMain" to "src/jsMain",
            "jvmMain" to "src/jvmMain"
        )

        standardSets.forEach { (name, basePath) ->
            val baseDir = File(projectDir, basePath)
            if (baseDir.exists()) {
                val kotlinDir = File(baseDir, "kotlin")
                val javaDir = File(baseDir, "java")
                val resDir = File(baseDir, "res")
                val manifest = File(baseDir, "AndroidManifest.xml")
                val assetsDir = File(baseDir, "assets")

                val kotlinDirStr = if (kotlinDir.exists()) kotlinDir.absolutePath else ""
                val javaDirStr = if (javaDir.exists()) javaDir.absolutePath else ""
                val resDirStr = if (resDir.exists()) resDir.absolutePath else ""
                val manifestStr = if (manifest.exists()) manifest.absolutePath else ""
                val assetsDirStr = if (assetsDir.exists()) assetsDir.absolutePath else ""

                results.add("$name|$kotlinDirStr|$javaDirStr|$resDirStr|$manifestStr|$assetsDirStr")
            }
        }

        if (results.isEmpty()) {
            val mainKotlin = File(projectDir, "src/main/kotlin")
            val mainJava = File(projectDir, "src/main/java")
            val mainRes = File(projectDir, "src/main/res")
            val mainManifest = File(projectDir, "src/main/AndroidManifest.xml")
            val mainAssets = File(projectDir, "src/main/assets")

            results.add("main|${if (mainKotlin.exists()) mainKotlin.absolutePath else ""}|${if (mainJava.exists()) mainJava.absolutePath else ""}|${if (mainRes.exists()) mainRes.absolutePath else ""}|${if (mainManifest.exists()) mainManifest.absolutePath else ""}|${if (mainAssets.exists()) mainAssets.absolutePath else ""}")
        }

        return results
    }
}
