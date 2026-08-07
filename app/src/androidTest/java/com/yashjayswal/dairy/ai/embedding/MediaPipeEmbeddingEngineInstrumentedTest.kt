package com.yashjayswal.dairy.ai.embedding

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlin.math.sqrt
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Runs real MediaPipe inference on a device/emulator — see
 * docs/RUNNING_ON_DEVICE.md step 8 for pushing `embedding_model.tflite` to
 * the device first. Without that file present this fails with the
 * "Embedding model not found" message from [MediaPipeEmbeddingEngine],
 * which is the expected/correct failure, not a bug.
 *
 * Run with: ./gradlew connectedDebugAndroidTest
 */
@RunWith(AndroidJUnit4::class)
class MediaPipeEmbeddingEngineInstrumentedTest {

    private val engine = MediaPipeEmbeddingEngine(ApplicationProvider.getApplicationContext<Context>())

    @Test
    fun embedReturnsANonEmptyFiniteVector() = runTest {
        val vector = engine.embed("I felt really happy today, everything went well.")

        assertTrue("expected a non-empty embedding", vector.isNotEmpty())
        assertFalse(
            "embedding contained NaN/Infinite values",
            vector.any { it.isNaN() || it.isInfinite() }
        )
    }

    @Test
    fun similarSentencesEmbedCloserThanUnrelatedOnes() = runTest {
        val happyA = engine.embed("I felt really happy today, everything went well.")
        val happyB = engine.embed("Today was a joyful, wonderful day for me.")
        val unrelated = engine.embed("The quarterly server maintenance window starts at midnight.")

        val similarScore = cosineSimilarity(happyA, happyB)
        val unrelatedScore = cosineSimilarity(happyA, unrelated)

        assertTrue(
            "expected semantically similar sentences ($similarScore) to score higher " +
                "than unrelated ones ($unrelatedScore)",
            similarScore > unrelatedScore
        )
    }

    @Test
    fun embeddingTheSameTextTwiceIsDeterministic() = runTest {
        val text = "Diary entries should embed the same text identically."

        val first = engine.embed(text)
        val second = engine.embed(text)

        assertEquals(1.0, cosineSimilarity(first, second).toDouble(), 0.001)
    }

    private fun cosineSimilarity(a: FloatArray, b: FloatArray): Float {
        var dot = 0f
        var normA = 0f
        var normB = 0f
        for (i in a.indices) {
            dot += a[i] * b[i]
            normA += a[i] * a[i]
            normB += b[i] * b[i]
        }
        return dot / (sqrt(normA) * sqrt(normB))
    }
}
