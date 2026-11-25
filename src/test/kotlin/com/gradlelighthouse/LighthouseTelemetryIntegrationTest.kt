package com.gradlelighthouse

import com.sun.net.httpserver.HttpServer
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.net.InetSocketAddress
import java.util.concurrent.CopyOnWriteArrayList

class LighthouseTelemetryIntegrationTest {

    @TempDir
    lateinit var testProjectDir: File

    private var mockServer: HttpServer? = null
    private val receivedPayloads = CopyOnWriteArrayList<String>()

    @BeforeEach
    fun setup() {
        receivedPayloads.clear()
        mockServer = HttpServer.create(InetSocketAddress("localhost", 0), 0).apply {
            createContext("/telemetry") { exchange ->
                val requestBody = exchange.requestBody.bufferedReader().readText()
                receivedPayloads.add(requestBody)
                val response = """{"accepted":true}""".toByteArray()
                exchange.sendResponseHeaders(202, response.size.toLong())
                exchange.responseBody.write(response)
                exchange.responseBody.close()
            }
            start()
        }

        File(testProjectDir, "settings.gradle.kts").writeText("rootProject.name = \"telemetry-test-project\"")
        val endpoint = "http://localhost:${mockServer!!.address.port}/telemetry"
        File(testProjectDir, "build.gradle.kts").writeText("""
            plugins {
                id("io.github.dev-vikas-soni.lighthouse")
            }

            lighthouse {
                telemetryEndpoint.set("$endpoint")
            }
        """.trimIndent())
    }

    @AfterEach
    fun tearDown() {
        mockServer?.stop(0)
    }

    @Test
    fun `completed audit triggers exactly one telemetry event`() {
        val srcDir = File(testProjectDir, "src/main/kotlin").apply { mkdirs() }
        File(srcDir, "Lib.kt").writeText("package com.test\nclass Lib")

        val result = GradleRunner.create()
            .withProjectDir(testProjectDir)
            .withArguments("lighthouseAudit")
            .withPluginClasspath()
            .build()

        assertTrue(result.output.contains("anonymous telemetry submitted"), "Should log telemetry success")
        assertEquals(1, receivedPayloads.size, "Exactly one telemetry event should be sent")

        val payload = receivedPayloads[0]
        assertTrue(payload.contains("\"event\": \"audit_completed\"") || payload.contains("\"event\":\"audit_completed\""))
    }

    @Test
    fun `severity gate failure still sends completed audit telemetry`() {
        val endpoint = "http://localhost:${mockServer!!.address.port}/telemetry"
        File(testProjectDir, "build.gradle.kts").writeText("""
            plugins {
                id("io.github.dev-vikas-soni.lighthouse")
                id("java")
            }

            dependencies {
                implementation("com.example:unstable:1.+")
            }

            lighthouse {
                telemetryEndpoint.set("$endpoint")
                failOnSeverity.set("ERROR")
            }
        """.trimIndent())

        val result = GradleRunner.create()
            .withProjectDir(testProjectDir)
            .withArguments("lighthouseAudit")
            .withPluginClasspath()
            .buildAndFail()

        assertTrue(result.output.contains("Analysis Complete"))
        assertEquals(1, receivedPayloads.size, "Telemetry should be sent before severity gate fails the build")
    }

    @Test
    fun `plugin application alone or unrelated task does not trigger telemetry`() {
        val result = GradleRunner.create()
            .withProjectDir(testProjectDir)
            .withArguments("tasks")
            .withPluginClasspath()
            .build()

        assertFalse(result.output.contains("[TELEMETRY]"), "Should not log telemetry for unrelated tasks")
        assertEquals(0, receivedPayloads.size, "No telemetry events should be sent for unrelated tasks")
    }

    @Test
    fun `telemetry opt-out via property prevents network activity`() {
        val srcDir = File(testProjectDir, "src/main/kotlin").apply { mkdirs() }
        File(srcDir, "Lib.kt").writeText("package com.test\nclass Lib")

        val result = GradleRunner.create()
            .withProjectDir(testProjectDir)
            .withArguments("lighthouseAudit", "-PgradleLighthouseTelemetry=false")
            .withPluginClasspath()
            .build()

        assertFalse(result.output.contains("anonymous telemetry submitted"), "Should not submit telemetry when opted out")
        assertEquals(0, receivedPayloads.size, "Zero telemetry events when opted out")
    }

    @Test
    fun `telemetry opt-out via environment variable prevents network activity`() {
        val srcDir = File(testProjectDir, "src/main/kotlin").apply { mkdirs() }
        File(srcDir, "Lib.kt").writeText("package com.test\nclass Lib")

        val result = GradleRunner.create()
            .withProjectDir(testProjectDir)
            .withArguments("lighthouseAudit")
            .withEnvironment(System.getenv() + mapOf("GRADLE_LIGHTHOUSE_TELEMETRY" to "false"))
            .withPluginClasspath()
            .build()

        assertFalse(result.output.contains("anonymous telemetry submitted"), "Should not submit telemetry when opted out via env var")
        assertEquals(0, receivedPayloads.size, "Zero telemetry events when opted out via env var")
    }

    @Test
    fun `telemetry opt-out via extension DSL prevents network activity`() {
        val endpoint = "http://localhost:${mockServer!!.address.port}/telemetry"
        File(testProjectDir, "build.gradle.kts").writeText("""
            plugins {
                id("io.github.dev-vikas-soni.lighthouse")
            }

            lighthouse {
                telemetryEndpoint.set("$endpoint")
                enableTelemetry.set(false)
            }
        """.trimIndent())

        val srcDir = File(testProjectDir, "src/main/kotlin").apply { mkdirs() }
        File(srcDir, "Lib.kt").writeText("package com.test\nclass Lib")

        val result = GradleRunner.create()
            .withProjectDir(testProjectDir)
            .withArguments("lighthouseAudit")
            .withPluginClasspath()
            .build()

        assertFalse(result.output.contains("anonymous telemetry submitted"), "Should not submit telemetry when opted out")
        assertEquals(0, receivedPayloads.size, "Zero telemetry events when enableTelemetry is false in DSL")
    }

    @Test
    fun `backend unavailability does not fail the audit`() {
        val unavailableEndpoint = "http://127.0.0.1:65534/telemetry"
        File(testProjectDir, "build.gradle.kts").writeText("""
            plugins {
                id("io.github.dev-vikas-soni.lighthouse")
            }

            lighthouse {
                telemetryEndpoint.set("$unavailableEndpoint")
            }
        """.trimIndent())
        mockServer?.stop(0) // Stop server so port becomes unavailable

        val srcDir = File(testProjectDir, "src/main/kotlin").apply { mkdirs() }
        File(srcDir, "Lib.kt").writeText("package com.test\nclass Lib")

        val result = GradleRunner.create()
            .withProjectDir(testProjectDir)
            .withArguments("lighthouseAudit")
            .withPluginClasspath()
            .build()

        assertTrue(result.output.contains("telemetry unavailable; continuing without telemetry"), "Should log graceful fallback")
        assertTrue(result.output.contains("Analysis Complete"), "Audit should still complete successfully")
    }

    @Test
    fun `repeated builds reuse the same installation ID`() {
        val srcDir = File(testProjectDir, "src/main/kotlin").apply { mkdirs() }
        File(srcDir, "Lib.kt").writeText("package com.test\nclass Lib")

        GradleRunner.create()
            .withProjectDir(testProjectDir)
            .withArguments("lighthouseAudit")
            .withPluginClasspath()
            .build()

        GradleRunner.create()
            .withProjectDir(testProjectDir)
            .withArguments("lighthouseAudit", "--rerun-tasks")
            .withPluginClasspath()
            .build()

        assertEquals(2, receivedPayloads.size)
        val id1 = Regex(""""installationId":\s*"([^"]+)"""").find(receivedPayloads[0])?.groupValues?.get(1)
        val id2 = Regex(""""installationId":\s*"([^"]+)"""").find(receivedPayloads[1])?.groupValues?.get(1)
        assertNotNull(id1)
        assertNotNull(id2)
        assertEquals(id1, id2, "Installation ID must remain identical across repeated builds")
    }

    @Test
    fun `telemetry is compatible with configuration cache`() {
        val srcDir = File(testProjectDir, "src/main/kotlin").apply { mkdirs() }
        File(srcDir, "Lib.kt").writeText("package com.test\nclass Lib")

        val result1 = GradleRunner.create()
            .withProjectDir(testProjectDir)
            .withArguments("lighthouseAudit", "--configuration-cache")
            .withPluginClasspath()
            .build()

        assertTrue(result1.output.contains("Configuration cache entry stored") || result1.output.contains("Calculating task graph"),
            "Configuration cache should store entry")

        val result2 = GradleRunner.create()
            .withProjectDir(testProjectDir)
            .withArguments("lighthouseAudit", "--configuration-cache", "--rerun-tasks")
            .withPluginClasspath()
            .build()

        assertTrue(result2.output.contains("Reusing configuration cache"),
            "Configuration cache should be reused on second run")
    }
}
