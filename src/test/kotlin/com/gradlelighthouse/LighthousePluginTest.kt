package com.gradlelighthouse

import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class LighthousePluginTest {

    @TempDir
    lateinit var testProjectDir: File

    private lateinit var buildFile: File
    private lateinit var settingsFile: File

    @BeforeEach
    fun setup() {
        settingsFile = File(testProjectDir, "settings.gradle.kts").apply {
            writeText("rootProject.name = \"test-project\"")
        }
        buildFile = File(testProjectDir, "build.gradle.kts").apply {
            writeText("""
                plugins {
                    id("com.gradlelighthouse.plugin")
                }
                
                repositories {
                    mavenCentral()
                }
                
                lighthouse {
                    enableDependencyHealth.set(true)
                }
            """.trimIndent())
        }
    }

    @Test
    fun `plugin registers lighthouseAudit task`() {
        val result = GradleRunner.create()
            .withProjectDir(testProjectDir)
            .withArguments("tasks")
            .withPluginClasspath()
            .build()

        assertTrue(result.output.contains("lighthouseAudit"), "Task 'lighthouseAudit' should be registered")
    }

    @Test
    fun `lighthouseAudit task executes successfully`() {
        val srcDir = File(testProjectDir, "src/main/kotlin")
        srcDir.mkdirs()
        File(srcDir, "Lib.kt").writeText("package com.test\nclass Lib")

        val result = GradleRunner.create()
            .withProjectDir(testProjectDir)
            .withArguments("lighthouseAudit", "--stacktrace")
            .withPluginClasspath()
            .build()

        assertTrue(result.output.contains("Gradle Lighthouse"), "Output should contain plugin header")
        assertTrue(result.output.contains("Analysis Complete"), "Output should contain completion message")
        
        val reportFile = File(testProjectDir, "build/reports/lighthouse/test-project-index.html")
        assertTrue(reportFile.exists(), "HTML report should be generated at ${reportFile.absolutePath}")
    }
}
