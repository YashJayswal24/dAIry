package com.yashjayswal.dairy.ai.llm

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Runs real MediaPipe LLM inference on a device/emulator — see
 * docs/RUNNING_ON_DEVICE.md step 8 for pushing `gemma-model.task` to the
 * device first. This is a *much* bigger download than the embedding model
 * (hundreds of MB to a few GB — see docs/REQUIREMENTS.md), so unlike the
 * embedding engine's instrumented test, don't expect this one to be cheap
 * to run repeatedly.
 *
 * Without the model file present this fails with the "Gemma model not
 * found" message from [MediaPipeGemmaInferenceEngine], which is the
 * expected/correct failure, not a bug.
 *
 * Run with: ./gradlew connectedDebugAndroidTest
 */
@RunWith(AndroidJUnit4::class)
class MediaPipeGemmaInferenceEngineInstrumentedTest {

    private val engine = MediaPipeGemmaInferenceEngine(ApplicationProvider.getApplicationContext<Context>())

    @Test
    fun generateReturnsANonBlankResponse() = runTest(timeout = 120.seconds) {
        val response = engine.generate("In one short sentence, what is a diary?")

        assertTrue("expected a non-blank response, got: \"$response\"", response.isNotBlank())
    }
}
