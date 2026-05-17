package com.gradlelighthouse

import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class LighthouseMultiModuleTest {

    @TempDir
    lateinit var testProjectDir: File

    @BeforeEach
    fun setup() {
        File(testProjectDir, "settings.gradle.kts").writeText("""
            rootProject.name = "multi-module-test"
            include(":app")
            include(":core")
        """.trimIndent())

        File(testProjectDir, "build.gradle.kts").writeText("""
            plugins {
                id("io.github.dev-vikas-soni.lighthouse")
            }

            lighthouse {
                minHealthScore.set(10)
            }
        """.trimIndent())

        setupModule("app", listOf(":core"))
        setupModule("core", emptyList())
    }

    private fun setupModule(name: String, deps: List<String>) {
        val moduleDir = File(testProjectDir, name).apply { mkdirs() }
        val depLines = deps.joinToString("\n") { "implementation(project(\"$it\"))" }
        File(moduleDir, "build.gradle.kts").writeText("""
            plugins {
                kotlin("jvm") version "2.0.0"
                id("io.github.dev-vikas-soni.lighthouse")
            }
            dependencies {
                $depLines
            }
        """.trimIndent())

        val srcDir = File(moduleDir, "src/main/kotlin")
        srcDir.mkdirs()
        File(srcDir, "Lib.kt").writeText("package com.$name\nclass Lib")
    }

    @Test
    fun `lighthouseAudit tracks global history in multi-module project`() {
        // First run
        GradleRunner.create()
            .withProjectDir(testProjectDir)
            .withArguments("lighthouseAudit", "lighthouseAggregate")
            .withPluginClasspath()
            .build()

        val globalHistoryFile = File(testProjectDir, ".lighthouse/global-history.json")
        assertTrue(globalHistoryFile.exists(), "Global history file should be created")

        val initialHistory = globalHistoryFile.readText()
        assertTrue(initialHistory.contains("\"score\""), "History should contain global score")

        // Second run
        GradleRunner.create()
            .withProjectDir(testProjectDir)
            .withArguments("lighthouseAudit", "lighthouseAggregate", "--rerun-tasks")
            .withPluginClasspath()
            .build()

        val updatedHistory = globalHistoryFile.readText()
        // Check if there are two entries (simplified check)
        assertTrue(updatedHistory.contains("},{") || updatedHistory.split("{\"score\"").size > 2,
            "History should contain multiple entries after multiple runs")
    }
}
