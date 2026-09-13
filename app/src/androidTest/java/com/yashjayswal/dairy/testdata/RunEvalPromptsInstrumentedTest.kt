package com.yashjayswal.dairy.testdata

import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.yashjayswal.dairy.DairyApplication
import com.yashjayswal.dairy.MainActivity
import com.yashjayswal.dairy.ui.chat.ChatAnswerer
import java.io.File
import kotlin.time.Duration.Companion.minutes
import kotlinx.coroutines.test.runTest
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Debug-controllable eval runner: calls the real [ChatAnswerer] pipeline
 * (RagRetriever -> RagPromptBuilder -> the real GemmaInferenceEngine)
 * directly, in-process, for every prompt in `eval_prompts.json` (mirrors
 * docs/EVAL_PROMPTS.md) -- no UI, no simulated taps or typing. Real
 * embeddings, real on-device generation, real seeded data; only the
 * screen interaction is skipped, since simulating it is fragile (the
 * soft keyboard opening/closing shifts every on-screen coordinate, and a
 * mistimed tap can land on the keyboard itself instead of the field).
 *
 * Writes results as JSON to the app's external files dir so they can be
 * pulled with adb and judged, instead of reading them off a screenshot.
 *
 * This reinstalling the app wipes /sdcard/Android/data/<pkg>/files/ (see
 * docs/RUNNING_ON_DEVICE.md), so run it WITHOUT going through Gradle's
 * connectedDebugAndroidTest (which reinstalls both APKs as part of the
 * task, wiping the just-pushed embedding model before the test can use
 * it). Install once, push the model, then invoke the already-installed
 * test directly:
 *
 * ```
 * ./gradlew assembleDebug assembleDebugAndroidTest
 * adb install -r app/build/outputs/apk/debug/app-debug.apk
 * adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
 * adb push embedding_model.tflite /sdcard/Android/data/com.yashjayswal.dairy/files/models/
 * adb shell am instrument -w \
 *   -e class com.yashjayswal.dairy.testdata.RunEvalPromptsInstrumentedTest \
 *   com.yashjayswal.dairy.test/androidx.test.runner.AndroidJUnitRunner
 * adb pull /sdcard/Android/data/com.yashjayswal.dairy/files/eval_results.json
 * ```
 */
@RunWith(AndroidJUnit4::class)
class RunEvalPromptsInstrumentedTest {

    @Test
    fun runEvalPrompts() = runTest(timeout = 30.minutes) {
        // AICore's Prompt API refuses background calls.
        ActivityScenario.launch(MainActivity::class.java).use {
            val app = ApplicationProvider.getApplicationContext<DairyApplication>()
            val testApkAssets = InstrumentationRegistry.getInstrumentation().context.assets
            val json = testApkAssets.open("eval_prompts.json").bufferedReader().use { reader -> reader.readText() }
            val prompts = JSONArray(json)

            val results = JSONArray()
            for (i in 0 until prompts.length()) {
                if (i > 0) {
                    // Real wall-clock pause (runTest's delay() is virtual
                    // and would be skipped) -- spacing calls out like a
                    // human typing, to test whether AiCoreGemmaInferenceEngine's
                    // ErrorCode 9 failures are a rate limit rather than a
                    // fixed per-session call count.
                    Thread.sleep(15_000)
                }
                val entry = prompts.getJSONObject(i)
                val id = entry.getString("id")
                val question = entry.getString("prompt")
                val reply = ChatAnswerer.answer(question, app.ragRetriever) { app.gemmaInferenceEngine() }
                results.put(
                    JSONObject().apply {
                        put("id", id)
                        put("prompt", question)
                        put("response", reply)
                    }
                )
                // Written after every prompt, not just at the end, so a
                // timeout or crash partway through still leaves partial
                // results pullable instead of losing everything.
                File(app.getExternalFilesDir(null), "eval_results.json").writeText(results.toString(2))
            }
        }
    }
}
