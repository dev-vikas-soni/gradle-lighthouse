package com.gradlelighthouse.core

/**
 * Builds the deliberately small usage event sent only after a user explicitly opts in.
 */
object TelemetryPayload {
    fun create(
        installationId: String,
        pluginVersion: String,
        gradleVersion: String
    ): String = """
        {
          "event": "audit_completed",
          "installationId": "${escapeJson(installationId)}",
          "pluginVersion": "${escapeJson(pluginVersion)}",
          "gradleVersion": "${escapeJson(gradleVersion)}"
        }
    """.trimIndent()

    private fun escapeJson(value: String): String = buildString {
        value.forEach { char ->
            when (char) {
                '\\' -> append("\\\\")
                '"' -> append("\\\"")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                '\t' -> append("\\t")
                else -> if (char.code < 0x20) append("\\u%04x".format(char.code)) else append(char)
            }
        }
    }
}
