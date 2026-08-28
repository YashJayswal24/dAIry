package com.yashjayswal.dairy.ui.entry

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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
import com.yashjayswal.dairy.domain.model.DiaryEntry
import com.yashjayswal.dairy.domain.model.Emotion
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

@Composable
fun EntryScreen(entryRepository: EntryRepository, modifier: Modifier = Modifier) {
    var text by remember { mutableStateOf("") }
    var emotion by remember { mutableStateOf(Emotion.NEUTRAL) }
    var intensity by remember { mutableFloatStateOf(3f) }
    var isSaving by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()
    val entries by entryRepository.observeAll().collectAsState(initial = emptyList())

    LazyColumn(
        modifier = modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        item {
            Spacer(Modifier.height(12.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("What's on your mind?", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(12.dp))

                    OutlinedTextField(
                        value = text,
                        onValueChange = { text = it },
                        placeholder = { Text("Today I...") },
                        minLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.height(16.dp))
                    Text("How are you feeling?", style = MaterialTheme.typography.labelLarge)
                    Spacer(Modifier.height(8.dp))
                    Row(modifier = Modifier.horizontalScroll(rememberScrollState())) {
                        Emotion.entries.forEach { option ->
                            FilterChip(
                                selected = emotion == option,
                                onClick = { emotion = option },
                                label = { Text("${option.emoji()} ${option.label()}") },
                                modifier = Modifier.padding(end = 8.dp)
                            )
                        }
                    }

                    Spacer(Modifier.height(16.dp))
                    Text("Intensity: ${intensity.roundToInt()}/5", style = MaterialTheme.typography.labelLarge)
                    Slider(
                        value = intensity,
                        onValueChange = { intensity = it },
                        valueRange = 1f..5f,
                        steps = 3
                    )

                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = {
                            val entryText = text
                            isSaving = true
                            errorMessage = null
                            coroutineScope.launch {
                                try {
                                    entryRepository.save(entryText, emotion, intensity.roundToInt())
                                    text = ""
                                    emotion = Emotion.NEUTRAL
                                    intensity = 3f
                                } catch (e: Exception) {
                                    errorMessage = e.message ?: "Failed to save entry."
                                } finally {
                                    isSaving = false
                                }
                            }
                        },
                        enabled = text.isNotBlank() && !isSaving,
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text(if (isSaving) "Saving…" else "Save entry")
                    }

                    errorMessage?.let { message ->
                        Spacer(Modifier.height(4.dp))
                        Text(message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
            Text("Past entries", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))

            if (entries.isEmpty()) {
                Text(
                    "No entries yet — write your first one above.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        items(entries, key = { it.id }) { entry ->
            EntryCard(entry)
            Spacer(Modifier.height(8.dp))
        }

        item { Spacer(Modifier.height(16.dp)) }
    }
}

@Composable
private fun EntryCard(entry: DiaryEntry) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(entry.text, style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(6.dp))
            Text(
                "${entry.emotion.emoji()} ${entry.emotion.label()} · " +
                    "${entry.emotionIntensity}/5 · ${formatDate(entry.createdAt)}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun Emotion.label(): String = name.lowercase().replaceFirstChar(Char::uppercase)

private fun Emotion.emoji(): String = when (this) {
    Emotion.JOY -> "😊"
    Emotion.SADNESS -> "😢"
    Emotion.ANGER -> "😠"
    Emotion.FEAR -> "😨"
    Emotion.SURPRISE -> "😲"
    Emotion.CALM -> "😌"
    Emotion.NEUTRAL -> "😐"
}

private fun formatDate(epochMillis: Long): String =
    SimpleDateFormat("MMM d, yyyy h:mm a", Locale.getDefault()).format(Date(epochMillis))
