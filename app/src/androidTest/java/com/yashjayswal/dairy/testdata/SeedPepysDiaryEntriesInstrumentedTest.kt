package com.yashjayswal.dairy.testdata

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.yashjayswal.dairy.DairyApplication
import com.yashjayswal.dairy.domain.model.Emotion
import kotlin.time.Duration.Companion.minutes
import kotlinx.coroutines.test.runTest
import org.json.JSONArray
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Not a correctness test — a one-shot seeder. Loads a realistic volume of
 * diary-style text (300 paragraphs excerpted from Samuel Pepys' diary,
 * public domain, Project Gutenberg #4200 — see `pepys_diary_entries.json`
 * in this module's assets) into the real on-device database via the real
 * [com.yashjayswal.dairy.data.repository.EntryRepository], computing real
 * embeddings with [com.yashjayswal.dairy.ai.embedding.MediaPipeEmbeddingEngine]
 * (needs `embedding_model.tflite` pushed — see docs/RUNNING_ON_DEVICE.md
 * step 8). This is how RagRetriever/ChatScreen get exercised against
 * hundreds of entries instead of a handful typed by hand.
 *
 * Pepys' diary has no emotion labels, so every seeded entry is saved as
 * [Emotion.NEUTRAL] / intensity 3 — a placeholder, not a claim about how
 * Pepys felt.
 *
 * Run only this test (it's slow — ~300 real on-device embedding calls —
 * and re-running appends the same 300 entries again rather than
 * deduplicating, so don't run it as part of the routine full suite):
 *
 * ```
 * ./gradlew connectedDebugAndroidTest \
 *   -Pandroid.testInstrumentationRunnerArguments.class=com.yashjayswal.dairy.testdata.SeedPepysDiaryEntriesInstrumentedTest
 * ```
 */
@RunWith(AndroidJUnit4::class)
class SeedPepysDiaryEntriesInstrumentedTest {

    @Test
    fun seedPepysDiaryEntries() = runTest(timeout = 10.minutes) {
        val app = ApplicationProvider.getApplicationContext<DairyApplication>()
        val testApkAssets = InstrumentationRegistry.getInstrumentation().context.assets
        val json = testApkAssets.open("pepys_diary_entries.json").bufferedReader().use { it.readText() }
        val entries = JSONArray(json)

        for (i in 0 until entries.length()) {
            app.entryRepository.save(entries.getString(i), Emotion.NEUTRAL, 3)
        }

        assertTrue("expected the bundled dataset to be non-empty", entries.length() > 0)
    }
}
