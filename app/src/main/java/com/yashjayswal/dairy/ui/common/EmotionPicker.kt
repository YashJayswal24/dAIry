package com.yashjayswal.dairy.ui.common

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.yashjayswal.dairy.domain.model.Emotion

/**
 * Circular button showing the current mood's emoji -- tap to open
 * [EmotionPickerSheet]. Modeled on the reference "My Diary" write-screen
 * mood avatar (analyzed 2026-09-13, not stored in this repo -- see
 * docs/TODO.md); uses our existing emoji set rather than custom
 * illustrated stickers, which would be a real art-asset cost, not a
 * library.
 */
@Composable
fun EmotionAvatarButton(emotion: Emotion, onClick: () -> Unit) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceVariant,
        onClick = onClick
    ) {
        Box(modifier = Modifier.size(48.dp), contentAlignment = Alignment.Center) {
            Text(emotion.emoji(), style = MaterialTheme.typography.headlineSmall)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmotionPickerSheet(selected: Emotion, onSelect: (Emotion) -> Unit, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState()) {
        Text(
            "How are you feeling?",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
            textAlign = TextAlign.Center
        )
        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            items(Emotion.entries) { option ->
                EmotionGridItem(
                    emotion = option,
                    isSelected = option == selected,
                    onClick = {
                        onSelect(option)
                        onDismiss()
                    }
                )
            }
        }
    }
}

@Composable
private fun EmotionGridItem(emotion: Emotion, isSelected: Boolean, onClick: () -> Unit) {
    Box(modifier = Modifier.padding(8.dp), contentAlignment = Alignment.Center) {
        Surface(
            shape = CircleShape,
            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
            onClick = onClick
        ) {
            Box(modifier = Modifier.size(64.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(emotion.emoji(), style = MaterialTheme.typography.headlineSmall)
                    Text(emotion.label(), style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}
