package com.yashjayswal.dairy.ui.entry

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun EntryScreen(modifier: Modifier = Modifier) {
    Column(modifier = modifier.padding(16.dp)) {
        Text("dAIry — write today's entry here.")
        // TODO: text input, emotion + intensity picker, save button wired to
        // EntryRepository.
    }
}
