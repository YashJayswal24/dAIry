package com.yashjayswal.dairy.ui.entry

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yashjayswal.dairy.data.repository.EntryRepository
import com.yashjayswal.dairy.domain.model.DiaryEntry
import com.yashjayswal.dairy.ui.common.EmotionAvatarButton
import com.yashjayswal.dairy.ui.common.EmotionPickerSheet
import com.yashjayswal.dairy.ui.common.PlainTextField
import com.yashjayswal.dairy.ui.common.displayTitle
import com.yashjayswal.dairy.ui.common.label
import java.time.Instant
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

/**
 * Full-screen entry detail: view, edit, or delete one entry, with
 * Previous/Next to browse neighbouring entries without returning to the
 * list first -- shared by [com.yashjayswal.dairy.ui.entry.EntryScreen]'s
 * past-entries list and [com.yashjayswal.dairy.ui.calendar.CalendarScreen]'s
 * day view. Modeled on the reference "My Diary" entry detail screenshot
 * (analyzed 2026-09-13, not stored in this repo -- see docs/TODO.md).
 *
 * [allEntries] should be the same ordering used to find [entry]'s
 * neighbours (newest first, matching [EntryRepository.observeAll]).
 */
@Composable
fun EntryDetailScreen(
    entry: DiaryEntry,
    allEntries: List<DiaryEntry>,
    entryRepository: EntryRepository,
    onClose: () -> Unit,
    onNavigate: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var isEditing by remember(entry.id) { mutableStateOf(false) }
    var title by remember(entry.id) { mutableStateOf(entry.title) }
    var text by remember(entry.id) { mutableStateOf(entry.text) }
    var emotion by remember(entry.id) { mutableStateOf(entry.emotion) }
    var intensity by remember(entry.id) { mutableFloatStateOf(entry.emotionIntensity.toFloat()) }
    var isSaving by remember(entry.id) { mutableStateOf(false) }
    var errorMessage by remember(entry.id) { mutableStateOf<String?>(null) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showEmotionPicker by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    val previousEntry = previousEntry(allEntries, entry.id)
    val nextEntry = nextEntry(allEntries, entry.id)

    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onClose) {
                Icon(Icons.Filled.Close, contentDescription = "Close")
            }
            Row {
                if (!isEditing) {
                    IconButton(onClick = { isEditing = true }) {
                        Icon(Icons.Filled.Edit, contentDescription = "Edit entry")
                    }
                }
                IconButton(onClick = { showDeleteConfirm = true }) {
                    Icon(Icons.Filled.Delete, contentDescription = "Delete entry")
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(entry.dayOfMonthString(), style = MaterialTheme.typography.displaySmall.copy(fontSize = 40.sp))
                Text(
                    entry.monthYearWeekdayString(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            EmotionAvatarButton(
                emotion = emotion,
                onClick = { if (isEditing) showEmotionPicker = true }
            )
        }

        Spacer(Modifier.height(8.dp))

        if (isEditing) {
            PlainTextField(
                value = title,
                onValueChange = { title = it },
                placeholder = "Title",
                textStyle = MaterialTheme.typography.headlineSmall,
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(8.dp))
            PlainTextField(
                value = text,
                onValueChange = { text = it },
                placeholder = "Start typing here...",
                textStyle = MaterialTheme.typography.bodyLarge,
                minLines = 5,
                modifier = Modifier.fillMaxWidth().weight(1f)
            )

            Spacer(Modifier.height(16.dp))
            Text("Intensity: ${intensity.roundToInt()}/5", style = MaterialTheme.typography.labelLarge)
            Slider(value = intensity, onValueChange = { intensity = it }, valueRange = 1f..5f, steps = 3)

            Spacer(Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(
                    onClick = {
                        title = entry.title
                        text = entry.text
                        emotion = entry.emotion
                        intensity = entry.emotionIntensity.toFloat()
                        isEditing = false
                        errorMessage = null
                    },
                    enabled = !isSaving
                ) { Text("Cancel") }
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = {
                        isSaving = true
                        errorMessage = null
                        coroutineScope.launch {
                            try {
                                entryRepository.update(entry.id, title, text, emotion, intensity.roundToInt())
                                isEditing = false
                            } catch (e: Exception) {
                                errorMessage = e.message ?: "Failed to save changes."
                            } finally {
                                isSaving = false
                            }
                        }
                    },
                    enabled = text.isNotBlank() && !isSaving
                ) { Text(if (isSaving) "Saving…" else "Save changes") }
            }

            errorMessage?.let { message ->
                Spacer(Modifier.height(4.dp))
                Text(message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
        } else {
            Text(entry.displayTitle(), style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(4.dp))
            Text(
                "${entry.emotion.label()} · ${entry.emotionIntensity}/5",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(16.dp))
            Text(
                entry.text,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())
            )
        }

        Spacer(Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            TextButton(
                onClick = { previousEntry?.let { onNavigate(it.id) } },
                enabled = previousEntry != null && !isEditing
            ) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = null)
                Text("Previous")
            }
            TextButton(
                onClick = { nextEntry?.let { onNavigate(it.id) } },
                enabled = nextEntry != null && !isEditing
            ) {
                Text("Next")
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
            }
        }
    }

    if (showEmotionPicker) {
        EmotionPickerSheet(
            selected = emotion,
            onSelect = { emotion = it },
            onDismiss = { showEmotionPicker = false }
        )
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete this entry?") },
            text = { Text("This can't be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    coroutineScope.launch {
                        entryRepository.delete(entry.id)
                        onClose()
                    }
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") }
            }
        )
    }
}

// allEntries is newest-first (matches EntryRepository.observeAll): Previous
// = further back in time, Next = more recent. internal (not private) so
// these are unit-testable from app/src/test without a Compose UI test
// harness.
internal fun previousEntry(allEntries: List<DiaryEntry>, currentId: Long): DiaryEntry? {
    val index = allEntries.indexOfFirst { it.id == currentId }
    return if (index >= 0) allEntries.getOrNull(index + 1) else null
}

internal fun nextEntry(allEntries: List<DiaryEntry>, currentId: Long): DiaryEntry? {
    val index = allEntries.indexOfFirst { it.id == currentId }
    return if (index >= 0) allEntries.getOrNull(index - 1) else null
}

private fun DiaryEntry.dayOfMonthString(): String =
    Instant.ofEpochMilli(createdAt).atZone(ZoneId.systemDefault()).dayOfMonth.toString().padStart(2, '0')

private fun DiaryEntry.monthYearWeekdayString(): String {
    val date = Instant.ofEpochMilli(createdAt).atZone(ZoneId.systemDefault())
    val month = date.month.getDisplayName(TextStyle.SHORT, Locale.getDefault())
    val weekday = date.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault())
    return "$month, ${date.year} · $weekday"
}
