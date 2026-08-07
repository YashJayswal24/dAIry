package com.yashjayswal.dairy.ai.llm

import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.yashjayswal.dairy.MainActivity
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Runs real AICore/Gemini Nano inference — only meaningful on a device that
 * actually supports it (flagship-tier chips, e.g. Pixel 8+, Galaxy S24+ —
 * see docs/REQUIREMENTS.md). On an unsupported device,
 * [AiCoreGemmaInferenceEngine.createIfAvailable] correctly returns null and
 * this test is skipped rather than failing, since "unsupported" is expected
 * behavior here, not a bug.
 *
 * AICore's Prompt API refuses to run unless the calling app is in the
 * foreground (`GenAiException [ErrorCode 30] Background usage is blocked`),
 * so this launches [MainActivity] first — matching how the real chat
 * feature will actually call this (user actively in the app).
 *
 * Run with: ./gradlew connectedDebugAndroidTest
 */
@RunWith(AndroidJUnit4::class)
class AiCoreGemmaInferenceEngineInstrumentedTest {

    @Test
    fun generateReturnsANonBlankResponseWhenAiCoreIsSupported() = runTest(timeout = 180.seconds) {
        ActivityScenario.launch(MainActivity::class.java).use {
            val engine = AiCoreGemmaInferenceEngine.createIfAvailable()
            if (engine == null) {
                // Expected on non-flagship devices — GemmaInferenceEngineProvider
                // falls back to MediaPipeGemmaInferenceEngine in that case.
                return@runTest
            }

            try {
                val response = engine.generate("In one short sentence, what is a diary?")

                assertTrue("expected a non-blank response, got: \"$response\"", response.isNotBlank())
            } finally {
                engine.close()
            }
        }
    }
}
