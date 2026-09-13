package com.yashjayswal.dairy.ui.entry

import com.yashjayswal.dairy.domain.model.DiaryEntry
import com.yashjayswal.dairy.domain.model.Emotion
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

private fun entry(id: Long) = DiaryEntry(
    id = id,
    text = "entry $id",
    emotion = Emotion.NEUTRAL,
    emotionIntensity = 3,
    createdAt = id // order doesn't matter here, only list position does
)

class EntryDetailScreenTest {

    // allEntries is newest-first, so index 0 is newest.
    private val newestFirst = listOf(entry(3), entry(2), entry(1))

    @Test
    fun `previousEntry returns the next-older entry`() {
        assertEquals(entry(2), previousEntry(newestFirst, currentId = 3))
        assertEquals(entry(1), previousEntry(newestFirst, currentId = 2))
    }

    @Test
    fun `previousEntry returns null for the oldest entry`() {
        assertNull(previousEntry(newestFirst, currentId = 1))
    }

    @Test
    fun `nextEntry returns the next-newer entry`() {
        assertEquals(entry(3), nextEntry(newestFirst, currentId = 2))
        assertEquals(entry(2), nextEntry(newestFirst, currentId = 1))
    }

    @Test
    fun `nextEntry returns null for the newest entry`() {
        assertNull(nextEntry(newestFirst, currentId = 3))
    }

    @Test
    fun `both return null when the current id is not in the list`() {
        assertNull(previousEntry(newestFirst, currentId = 999))
        assertNull(nextEntry(newestFirst, currentId = 999))
    }

    @Test
    fun `both return null for a single-entry list`() {
        val single = listOf(entry(1))
        assertNull(previousEntry(single, currentId = 1))
        assertNull(nextEntry(single, currentId = 1))
    }
}
