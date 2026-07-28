package com.yashjayswal.dairy.data.local

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

class EmbeddingCodecTest {

    @Test
    fun `float array survives a round trip through bytes`() {
        val original = floatArrayOf(0.1f, -2.5f, 3.14159f, 0f, 100f)

        val restored = original.toByteArray().toFloatArray()

        assertArrayEquals(original, restored, 0.0001f)
    }

    @Test
    fun `empty array survives a round trip`() {
        val original = FloatArray(0)

        val restored = original.toByteArray().toFloatArray()

        assertArrayEquals(original, restored, 0.0001f)
    }

    @Test
    fun `byte array size is four times the float count`() {
        val original = FloatArray(384) { it.toFloat() }

        val bytes = original.toByteArray()

        assertEquals(384 * 4, bytes.size)
    }
}
