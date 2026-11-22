package com.gradlelighthouse.core

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class TelemetryPayloadTest {
    @Test
    fun `payload contains only the documented usage fields`() {
        val payload = TelemetryPayload.create(
            installationId = "f9e53cb4-1e8a-4f03-9fd3-2e4c0b47b91f",
            pluginVersion = "2.3.2",
            gradleVersion = "9.9.1"
        )

        assertTrue(payload.contains("\"event\": \"audit_completed\""))
        assertTrue(payload.contains("\"installationId\": \"f9e53cb4-1e8a-4f03-9fd3-2e4c0b47b91f\""))
        assertTrue(payload.contains("\"pluginVersion\": \"2.3.2\""))
        assertTrue(payload.contains("\"gradleVersion\": \"9.9.1\""))
        assertFalse(payload.contains("projectName"))
        assertFalse(payload.contains("dependencies"))
        assertFalse(payload.contains("score"))
    }

    @Test
    fun `payload escapes values for JSON`() {
        val payload = TelemetryPayload.create(
            installationId = "id",
            pluginVersion = "version\"with-quote",
            gradleVersion = "gradle\\version"
        )

        assertTrue(payload.contains("version\\\"with-quote"))
        assertTrue(payload.contains("gradle\\\\version"))
    }
}
