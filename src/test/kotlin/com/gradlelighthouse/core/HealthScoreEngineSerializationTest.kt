package com.gradlelighthouse.core

import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream

class HealthScoreEngineSerializationTest {

    @Test
    fun `deserialization preserves singleton identity`() {
        val bytes = ByteArrayOutputStream().use { buffer ->
            ObjectOutputStream(buffer).use { it.writeObject(HealthScoreEngine) }
            buffer.toByteArray()
        }

        val restored = ObjectInputStream(ByteArrayInputStream(bytes)).use {
            it.readObject()
        }

        assertSame(HealthScoreEngine, restored)
    }
}
