package com.yashjayswal.dairy.ui.calendar

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.kizitonwose.calendar.compose.HorizontalCalendar
import com.kizitonwose.calendar.compose.rememberCalendarState
import com.kizitonwose.calendar.core.CalendarDay
import com.kizitonwose.calendar.core.DayPosition
import com.kizitonwose.calendar.core.OutDateStyle
import com.yashjayswal.dairy.domain.model.DiaryEntry
import com.yashjayswal.dairy.data.repository.EntryRepository
import com.yashjayswal.dairy.ui.common.displayTitle
import com.yashjayswal.dairy.ui.common.emoji
import com.yashjayswal.dairy.ui.common.label
import com.yashjayswal.dairy.ui.entry.EntryDetailScreen
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale

/**
 * Month grid via com.kizitonwose.calendar:compose (see docs/TODO.md for
 * why this library) -- days with an entry show that day's (first) emotion
 * emoji, days without show a plain number. Tapping a day shows its
 * entries below the grid, mirroring the reference "My Diary" screenshots
 * analyzed 2026-09-13 (not stored in this repo -- see TODO.md).
 */
@Composable
fun CalendarScreen(entryRepository: EntryRepository, modifier: Modifier = Modifier) {
    val entries by entryRepository.observeAll().collectAsState(initial = emptyList())
    val entriesByDate = remember(entries) {
        entries.groupBy { it.localDate() }
    }

    val today = remember { LocalDate.now() }
    val currentMonth = remember { YearMonth.now() }
    val state = rememberCalendarState(
        startMonth = remember { currentMonth.minusMonths(24) },
        endMonth = remember { currentMonth.plusMonths(24) },
        firstVisibleMonth = currentMonth,
        firstDayOfWeek = DayOfWeek.SUNDAY,
        outDateStyle = OutDateStyle.EndOfRow
    )
    var selectedDate by remember { mutableStateOf(today) }
    var selectedEntryId by remember { mutableStateOf<Long?>(null) }

    BackHandler(enabled = selectedEntryId != null) { selectedEntryId = null }

    val selectedEntry = entries.find { it.id == selectedEntryId }
    if (selectedEntry != null) {
        EntryDetailScreen(
            entry = selectedEntry,
            allEntries = entries,
            entryRepository = entryRepository,
            onClose = { selectedEntryId = null },
            onNavigate = { selectedEntryId = it },
            modifier = modifier
        )
        return
    }

    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        val visibleMonth = state.firstVisibleMonth.yearMonth
        Text(
            "${visibleMonth.month.getDisplayName(TextStyle.FULL, Locale.getDefault())} ${visibleMonth.year}",
            style = MaterialTheme.typography.titleLarge
        )
        Spacer(Modifier.height(12.dp))

        HorizontalCalendar(
            state = state,
            dayContent = { day ->
                DayCell(
                    day = day,
                    dayEntries = entriesByDate[day.date].orEmpty(),
                    isSelected = day.date == selectedDate,
                    onClick = { if (day.position == DayPosition.MonthDate) selectedDate = day.date }
                )
            },
            monthHeader = { DaysOfWeekHeader() }
        )

        Spacer(Modifier.height(16.dp))

        val selectedEntries = entriesByDate[selectedDate].orEmpty()
        Text(
            selectedDate.toDisplayString(),
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(Modifier.height(8.dp))

        if (selectedEntries.isEmpty()) {
            Text(
                "No entries on this day.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(selectedEntries, key = { it.id }) { entry ->
                    DayEntryCard(entry, onClick = { selectedEntryId = entry.id })
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun DaysOfWeekHeader() {
    val daysOfWeek = remember { sundayFirstDaysOfWeek() }
    Row(modifier = Modifier.fillMaxWidth()) {
        daysOfWeek.forEach { dayOfWeek ->
            Text(
                text = dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun DayCell(
    day: CalendarDay,
    dayEntries: List<DiaryEntry>,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val isInMonth = day.position == DayPosition.MonthDate
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .padding(2.dp)
            .clip(CircleShape)
            .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
            .clickable(enabled = isInMonth, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        val contentColor = when {
            !isInMonth -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
            isSelected -> MaterialTheme.colorScheme.onPrimary
            else -> MaterialTheme.colorScheme.onSurfaceVariant
        }
        if (isInMonth && dayEntries.isNotEmpty()) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(dayEntries.first().emotion.emoji(), style = MaterialTheme.typography.bodyMedium)
                Text(day.date.dayOfMonth.toString(), style = MaterialTheme.typography.labelSmall, color = contentColor)
            }
        } else {
            Text(day.date.dayOfMonth.toString(), style = MaterialTheme.typography.bodyMedium, color = contentColor)
        }
    }
}

@Composable
private fun DayEntryCard(entry: DiaryEntry, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(entry.displayTitle(), style = MaterialTheme.typography.titleMedium, maxLines = 1)
            Spacer(Modifier.height(4.dp))
            Text(entry.text, style = MaterialTheme.typography.bodyMedium, maxLines = 2)
            Spacer(Modifier.height(6.dp))
            Text(
                "${entry.emotion.emoji()} ${entry.emotion.label()} · ${entry.emotionIntensity}/5",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// internal (not private): unit-tested from app/src/test without a Compose
// UI test harness.
internal fun DiaryEntry.localDate(): LocalDate =
    Instant.ofEpochMilli(createdAt).atZone(ZoneId.systemDefault()).toLocalDate()

private fun LocalDate.toDisplayString(): String =
    "${dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault())}, " +
        "${month.getDisplayName(TextStyle.FULL, Locale.getDefault())} $dayOfMonth"

internal fun sundayFirstDaysOfWeek(): List<DayOfWeek> {
    val days = DayOfWeek.entries // Monday..Sunday
    val sundayIndex = days.indexOf(DayOfWeek.SUNDAY)
    return days.subList(sundayIndex, days.size) + days.subList(0, sundayIndex)
}
