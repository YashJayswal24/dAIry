package com.yashjayswal.dairy.ai.llm

import java.io.File
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Covers only the pure path/validation logic in [MediaPipeGemmaInferenceEngine].
 * The actual LlmInference/MediaPipe calls need a real Android device or
 * emulator (native libs), so they're out of scope for a JVM unit test — see
 * docs/RUNNING_ON_DEVICE.md.
 */
class MediaPipeGemmaInferenceEngineTest {

    private lateinit var tempDir: File

    @Before
    fun setUp() {
        tempDir = File.createTempFile("dairy-gemma-test", "").apply {
            delete()
            mkdirs()
        }
    }

    @After
    fun tearDown() {
        tempDir.deleteRecursively()
    }

    @Test
    fun `resolveModelFile appends the expected file name`() {
        val resolved = MediaPipeGemmaInferenceEngine.resolveModelFile(tempDir)

        assertEquals(File(tempDir, "gemma-model.task"), resolved)
    }

    @Test
    fun `resolveModelFile throws when external storage is unavailable`() {
        assertThrows(IllegalStateException::class.java) {
            MediaPipeGemmaInferenceEngine.resolveModelFile(null)
        }
    }

    @Test
    fun `requireModelFile returns the file when it exists`() {
        val modelFile = File(tempDir, "gemma-model.task").apply { writeText("stub") }

        val result = MediaPipeGemmaInferenceEngine.requireModelFile(modelFile)

        assertEquals(modelFile, result)
    }

    @Test
    fun `requireModelFile throws a helpful message when the model is missing`() {
        val missingFile = File(tempDir, "gemma-model.task")

        val error = assertThrows(IllegalStateException::class.java) {
            MediaPipeGemmaInferenceEngine.requireModelFile(missingFile)
        }
        assertTrue(
            "Expected error message to point at docs/RUNNING_ON_DEVICE.md, was: ${error.message}",
            error.message!!.contains("RUNNING_ON_DEVICE.md")
        )
    }
}
