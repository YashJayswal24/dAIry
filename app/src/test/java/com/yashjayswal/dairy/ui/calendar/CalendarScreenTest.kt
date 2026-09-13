package com.yashjayswal.dairy.ui.calendar

import com.yashjayswal.dairy.domain.model.DiaryEntry
import com.yashjayswal.dairy.domain.model.Emotion
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Test

class CalendarScreenTest {

    @Test
    fun `sundayFirstDaysOfWeek starts on Sunday and contains each day exactly once`() {
        val days = sundayFirstDaysOfWeek()

        assertEquals(DayOfWeek.SUNDAY, days.first())
        assertEquals(DayOfWeek.entries.toSet(), days.toSet())
        assertEquals(7, days.size)
    }

    @Test
    fun `sundayFirstDaysOfWeek is in calendar order starting from Sunday`() {
        val days = sundayFirstDaysOfWeek()

        assertEquals(
            listOf(
                DayOfWeek.SUNDAY, DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
                DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY
            ),
            days
        )
    }

    @Test
    fun `localDate converts createdAt using the system zone`() {
        val zone = ZoneId.systemDefault()
        val expected = LocalDate.of(2026, 9, 12)
        val createdAt = expected.atStartOfDay(zone).toInstant().toEpochMilli() + 3_600_000 // + 1h, still same day

        val entry = DiaryEntry(
            id = 1,
            text = "test",
            emotion = Emotion.JOY,
            emotionIntensity = 5,
            createdAt = createdAt
        )

        assertEquals(expected, entry.localDate())
    }
}
