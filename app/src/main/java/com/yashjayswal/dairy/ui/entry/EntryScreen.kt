package com.yashjayswal.dairy.ui.entry

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
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
import androidx.compose.ui.unit.dp
import com.yashjayswal.dairy.data.repository.EntryRepository
import com.yashjayswal.dairy.domain.model.DiaryEntry
import com.yashjayswal.dairy.ui.common.displayTitle
import com.yashjayswal.dairy.ui.common.emoji
import com.yashjayswal.dairy.ui.common.label
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private enum class EntryScreenMode { LIST, COMPOSE }

@Composable
fun EntryScreen(entryRepository: EntryRepository, modifier: Modifier = Modifier) {
    val entries by entryRepository.observeAll().collectAsState(initial = emptyList())
    var mode by remember { mutableStateOf(EntryScreenMode.LIST) }
    var selectedEntryId by remember { mutableStateOf<Long?>(null) }

    BackHandler(enabled = mode == EntryScreenMode.COMPOSE || selectedEntryId != null) {
        if (mode == EntryScreenMode.COMPOSE) mode = EntryScreenMode.LIST else selectedEntryId = null
    }

    val selectedEntry = entries.find { it.id == selectedEntryId }
    when {
        mode == EntryScreenMode.COMPOSE -> EntryComposeScreen(
            entryRepository = entryRepository,
            onClose = { mode = EntryScreenMode.LIST },
            modifier = modifier
        )
        selectedEntry != null -> EntryDetailScreen(
            entry = selectedEntry,
            allEntries = entries,
            entryRepository = entryRepository,
            onClose = { selectedEntryId = null },
            onNavigate = { selectedEntryId = it },
            modifier = modifier
        )
        else -> EntryListContent(
            entries = entries,
            onEntryClick = { selectedEntryId = it.id },
            onNewEntry = { mode = EntryScreenMode.COMPOSE },
            modifier = modifier
        )
    }
}

@Composable
private fun EntryListContent(
    entries: List<DiaryEntry>,
    onEntryClick: (DiaryEntry) -> Unit,
    onNewEntry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            contentPadding = PaddingValues(bottom = 88.dp)
        ) {
            item {
                Spacer(Modifier.height(12.dp))
                Text("Past entries", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))

                if (entries.isEmpty()) {
                    Text(
                        "No entries yet — tap + to write your first one.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            items(entries, key = { it.id }) { entry ->
                EntryCard(entry, onClick = { onEntryClick(entry) })
                Spacer(Modifier.height(8.dp))
            }
        }

        FloatingActionButton(
            onClick = onNewEntry,
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)
        ) {
            Icon(Icons.Filled.Add, contentDescription = "New entry")
        }
    }
}

@Composable
private fun EntryCard(entry: DiaryEntry, onClick: () -> Unit) {
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
                "${entry.emotion.emoji()} ${entry.emotion.label()} · " +
                    "${entry.emotionIntensity}/5 · ${formatDate(entry.createdAt)}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun formatDate(epochMillis: Long): String =
    SimpleDateFormat("MMM d, yyyy h:mm a", Locale.getDefault()).format(Date(epochMillis))
