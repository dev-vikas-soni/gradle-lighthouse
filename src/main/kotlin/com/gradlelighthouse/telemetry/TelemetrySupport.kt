package com.gradlelighthouse.telemetry

import java.io.File
import java.io.RandomAccessFile
import java.nio.charset.StandardCharsets
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.nio.file.attribute.PosixFilePermission
import java.util.UUID

internal object TelemetrySupport {
    const val DEFAULT_ENDPOINT =
        "https://asia-south1-gradle-lighthouse-telemetry.cloudfunctions.net/telemetry"

    private val installationIdPattern =
        Regex("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$")

    @Synchronized
    fun getOrCreateInstallationId(gradleUserHomeDir: File): String {
        return try {
            val directory = File(gradleUserHomeDir, "caches/gradle-lighthouse")
            if (!directory.isDirectory && !directory.mkdirs() && !directory.isDirectory) {
                return UUID.randomUUID().toString()
            }

            val idFile = File(directory, "installation-id")
            val lockFile = File(directory, "installation-id.lock")

            RandomAccessFile(lockFile, "rw").use { lock ->
                lock.channel.lock().use {
                    val existingId = if (idFile.isFile) {
                        try {
                            idFile.readText(StandardCharsets.UTF_8).trim()
                        } catch (_: Exception) {
                            ""
                        }
                    } else {
                        ""
                    }

                    if (installationIdPattern.matches(existingId.lowercase())) {
                        return existingId.lowercase()
                    }

                    val installationId = UUID.randomUUID().toString()
                    val temporaryFile = Files.createTempFile(
                        directory.toPath(),
                        "installation-id-",
                        ".tmp"
                    )
                    try {
                        Files.writeString(
                            temporaryFile,
                            "$installationId\n",
                            StandardCharsets.UTF_8
                        )

                        // Attempt to set POSIX permissions to owner read/write only if supported
                        try {
                            val permissions = setOf(
                                PosixFilePermission.OWNER_READ,
                                PosixFilePermission.OWNER_WRITE
                            )
                            Files.setPosixFilePermissions(temporaryFile, permissions)
                        } catch (_: Exception) {
                            // Non-POSIX OS (e.g. Windows) or unsupported filesystem; safe to ignore
                        }

                        try {
                            Files.move(
                                temporaryFile,
                                idFile.toPath(),
                                StandardCopyOption.ATOMIC_MOVE,
                                StandardCopyOption.REPLACE_EXISTING
                            )
                        } catch (_: AtomicMoveNotSupportedException) {
                            Files.move(
                                temporaryFile,
                                idFile.toPath(),
                                StandardCopyOption.REPLACE_EXISTING
                            )
                        }
                    } finally {
                        Files.deleteIfExists(temporaryFile)
                    }
                    return installationId
                }
            }
        } catch (_: Exception) {
            // Never fail the build due to installation ID creation or persistence failure
            UUID.randomUUID().toString()
        }
    }

    fun payloadJson(
        installationId: String,
        pluginVersion: String,
        gradleVersion: String
    ): String {
        val safeInstallationId = escapeJson(installationId)
        val safePluginVersion = escapeJson(pluginVersion)
        val safeGradleVersion = escapeJson(gradleVersion)
        return """
            {
              "event": "audit_completed",
              "installationId": "$safeInstallationId",
              "pluginVersion": "$safePluginVersion",
              "gradleVersion": "$safeGradleVersion"
            }
        """.trimIndent()
    }

    fun sendTelemetry(
        endpoint: String,
        installationId: String,
        pluginVersion: String,
        gradleVersion: String
    ): Boolean {
        var connection: java.net.HttpURLConnection? = null
        return try {
            val url = java.net.URI.create(endpoint).toURL()
            connection = url.openConnection() as java.net.HttpURLConnection
            connection.requestMethod = "POST"
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/json")
            connection.connectTimeout = 3000
            connection.readTimeout = 3000

            val payload = payloadJson(installationId, pluginVersion, gradleVersion)
            connection.outputStream.use { it.write(payload.toByteArray(StandardCharsets.UTF_8)) }

            val responseCode = connection.responseCode
            val responseBody = if (responseCode in 200..299) {
                connection.inputStream.use { it.bufferedReader(StandardCharsets.UTF_8).readText() }
            } else {
                connection.errorStream?.use { it.bufferedReader(StandardCharsets.UTF_8).readText() } ?: ""
            }

            responseCode == 202 && Regex(""""accepted"\s*:\s*true""").containsMatchIn(responseBody)
        } catch (_: Exception) {
            false
        } finally {
            try {
                connection?.disconnect()
            } catch (_: Exception) {}
        }
    }

    private fun escapeJson(str: String): String =
        str.replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
}
