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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
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
import com.yashjayswal.dairy.data.repository.EntryRepository
import com.yashjayswal.dairy.domain.model.Emotion
import com.yashjayswal.dairy.ui.common.EmotionAvatarButton
import com.yashjayswal.dairy.ui.common.EmotionPickerSheet
import com.yashjayswal.dairy.ui.common.PlainTextField
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

/**
 * Full-screen "write a new entry" page, separate from the entries list --
 * modeled on the reference "My Diary" write screen (analyzed 2026-09-13,
 * not stored in this repo -- see docs/TODO.md): borderless title/body
 * fields (no boxed TextField chrome), a date row with a picker for
 * backdating, and a mood avatar that opens [EmotionPickerSheet] instead
 * of an inline chip row.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EntryComposeScreen(entryRepository: EntryRepository, onClose: () -> Unit, modifier: Modifier = Modifier) {
    var title by remember { mutableStateOf("") }
    var text by remember { mutableStateOf("") }
    var emotion by remember { mutableStateOf(Emotion.NEUTRAL) }
    var intensity by remember { mutableFloatStateOf(3f) }
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var showEmotionPicker by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()

    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onClose) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Discard and close")
            }
            Button(
                onClick = {
                    isSaving = true
                    errorMessage = null
                    val createdAt = selectedDate.atTime(LocalTime.now())
                        .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                    coroutineScope.launch {
                        try {
                            entryRepository.save(title, text, emotion, intensity.roundToInt(), createdAt)
                            onClose()
                        } catch (e: Exception) {
                            errorMessage = e.message ?: "Failed to save entry."
                        } finally {
                            isSaving = false
                        }
                    }
                },
                enabled = text.isNotBlank() && !isSaving
            ) {
                Text(if (isSaving) "Saving…" else "Save")
            }
        }

        Spacer(Modifier.height(12.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = { showDatePicker = true }) {
                Column(horizontalAlignment = Alignment.Start) {
                    Text(selectedDate.dayOfMonth.toString().padStart(2, '0'), style = MaterialTheme.typography.headlineMedium)
                    Text(
                        "${selectedDate.month.getDisplayName(TextStyle.SHORT, Locale.getDefault())}, ${selectedDate.year} · " +
                            selectedDate.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault()),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            Spacer(Modifier.weight(1f))
            EmotionAvatarButton(emotion = emotion, onClick = { showEmotionPicker = true })
        }

        Spacer(Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Intensity", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Slider(
                value = intensity,
                onValueChange = { intensity = it },
                valueRange = 1f..5f,
                steps = 3,
                modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
            )
            Text("${intensity.roundToInt()}/5", style = MaterialTheme.typography.labelMedium)
        }

        Spacer(Modifier.height(12.dp))

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
            modifier = Modifier.fillMaxWidth().weight(1f)
        )

        errorMessage?.let { message ->
            Text(message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
    }

    if (showEmotionPicker) {
        EmotionPickerSheet(
            selected = emotion,
            onSelect = { emotion = it },
            onDismiss = { showEmotionPicker = false }
        )
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        selectedDate = Instant.ofEpochMilli(millis).atZone(ZoneId.of("UTC")).toLocalDate()
                    }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}
