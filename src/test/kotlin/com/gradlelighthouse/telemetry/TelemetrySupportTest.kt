package com.gradlelighthouse.telemetry

import com.sun.net.httpserver.HttpServer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.net.InetSocketAddress
import java.util.UUID
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class TelemetrySupportTest {

    @TempDir
    lateinit var tempGradleHome: File

    private var server: HttpServer? = null

    @AfterEach
    fun tearDown() {
        server?.stop(0)
    }

    // =========================================================================
    // Installation ID tests
    // =========================================================================

    @Test
    fun `getOrCreateInstallationId generates valid UUID and persists it`() {
        val id1 = TelemetrySupport.getOrCreateInstallationId(tempGradleHome)
        assertNotNull(UUID.fromString(id1), "Should be a valid UUID")

        val persistedFile = File(tempGradleHome, "caches/gradle-lighthouse/installation-id")
        assertTrue(persistedFile.exists(), "Installation ID file should exist")
        assertEquals(id1, persistedFile.readText().trim())

        // Second call should return the exact same persisted ID
        val id2 = TelemetrySupport.getOrCreateInstallationId(tempGradleHome)
        assertEquals(id1, id2, "Subsequent call must return identical persisted ID")
    }

    @Test
    fun `getOrCreateInstallationId recovers from corrupted persisted data`() {
        val cacheDir = File(tempGradleHome, "caches/gradle-lighthouse").apply { mkdirs() }
        val idFile = File(cacheDir, "installation-id")
        idFile.writeText("corrupted-not-a-uuid\n")

        val newId = TelemetrySupport.getOrCreateInstallationId(tempGradleHome)
        assertNotNull(UUID.fromString(newId), "Should generate new valid UUID when corrupt")
        assertNotEquals("corrupted-not-a-uuid", newId)
        assertEquals(newId, idFile.readText().trim(), "File should be updated with new valid UUID")
    }

    @Test
    fun `getOrCreateInstallationId handles concurrent calls safely`() {
        val threadCount = 10
        val executor = Executors.newFixedThreadPool(threadCount)
        val futures = (1..threadCount).map {
            executor.submit<String> {
                TelemetrySupport.getOrCreateInstallationId(tempGradleHome)
            }
        }
        executor.shutdown()
        assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS))

        val results = futures.map { it.get() }.toSet()
        assertEquals(1, results.size, "All concurrent calls must resolve to the exact same installation ID")
    }

    @Test
    fun `getOrCreateInstallationId handles inaccessible dir gracefully`() {
        val invalidFileAsHome = File(tempGradleHome, "invalid-home-file").apply {
            writeText("not a directory")
        }
        // Should not throw exception, should return fallback valid UUID
        val id = TelemetrySupport.getOrCreateInstallationId(invalidFileAsHome)
        assertNotNull(UUID.fromString(id))
    }

    // =========================================================================
    // Payload tests
    // =========================================================================

    @Test
    fun `payloadJson creates exact four-field JSON payload`() {
        val json = TelemetrySupport.payloadJson("test-uuid-123", "2.4.0", "9.0")

        assertTrue(json.contains("\"event\": \"audit_completed\""))
        assertTrue(json.contains("\"installationId\": \"test-uuid-123\""))
        assertTrue(json.contains("\"pluginVersion\": \"2.4.0\""))
        assertTrue(json.contains("\"gradleVersion\": \"9.0\""))

        // Count JSON fields: should be exactly 4 keys
        val keyMatches = Regex(""""([^"]+)":""").findAll(json).map { it.groupValues[1] }.toList()
        assertEquals(listOf("event", "installationId", "pluginVersion", "gradleVersion"), keyMatches,
            "JSON payload must contain exactly the 4 required fields and no extra fields")
    }

    // =========================================================================
    // HTTP client tests
    // =========================================================================

    @Test
    fun `sendTelemetry returns true on HTTP 202 accepted`() {
        startMockServer(202, """{"accepted":true}""")
        val endpoint = "http://localhost:${server!!.address.port}/telemetry"

        val success = TelemetrySupport.sendTelemetry(endpoint, UUID.randomUUID().toString(), "2.4.0", "9.0")
        assertTrue(success, "HTTP 202 accepted=true should succeed")
    }

    @Test
    fun `sendTelemetry returns false on HTTP 400`() {
        startMockServer(400, """{"accepted":false,"error":"bad request"}""")
        val endpoint = "http://localhost:${server!!.address.port}/telemetry"

        val success = TelemetrySupport.sendTelemetry(endpoint, UUID.randomUUID().toString(), "2.4.0", "9.0")
        assertFalse(success, "HTTP 400 should return false")
    }

    @Test
    fun `sendTelemetry returns false on HTTP 500`() {
        startMockServer(500, "Internal Server Error")
        val endpoint = "http://localhost:${server!!.address.port}/telemetry"

        val success = TelemetrySupport.sendTelemetry(endpoint, UUID.randomUUID().toString(), "2.4.0", "9.0")
        assertFalse(success, "HTTP 500 should return false")
    }

    @Test
    fun `sendTelemetry returns false on HTTP 200 without accepted true`() {
        startMockServer(200, """{"accepted":false}""")
        val endpoint = "http://localhost:${server!!.address.port}/telemetry"

        val success = TelemetrySupport.sendTelemetry(endpoint, UUID.randomUUID().toString(), "2.4.0", "9.0")
        assertFalse(success, "HTTP 200 without accepted=true should return false")
    }

    @Test
    fun `sendTelemetry returns false on connection refused`() {
        val endpoint = "http://127.0.0.1:65530/telemetry"
        val success = TelemetrySupport.sendTelemetry(endpoint, UUID.randomUUID().toString(), "2.4.0", "9.0")
        assertFalse(success, "Connection refused should return false gracefully")
    }

    @Test
    fun `sendTelemetry returns false on timeout`() {
        server = HttpServer.create(InetSocketAddress("localhost", 0), 0)
        server!!.createContext("/telemetry") { exchange ->
            Thread.sleep(4000) // Sleep longer than 3s timeout
            exchange.sendResponseHeaders(202, 0)
            exchange.responseBody.close()
        }
        server!!.start()
        val endpoint = "http://localhost:${server!!.address.port}/telemetry"

        val success = TelemetrySupport.sendTelemetry(endpoint, UUID.randomUUID().toString(), "2.4.0", "9.0")
        assertFalse(success, "Timeout should return false gracefully")
    }

    private fun startMockServer(statusCode: Int, responseBody: String) {
        server = HttpServer.create(InetSocketAddress("localhost", 0), 0)
        server!!.createContext("/telemetry") { exchange ->
            val bytes = responseBody.toByteArray()
            exchange.sendResponseHeaders(statusCode, bytes.size.toLong())
            exchange.responseBody.write(bytes)
            exchange.responseBody.close()
        }
        server!!.start()
    }
}
