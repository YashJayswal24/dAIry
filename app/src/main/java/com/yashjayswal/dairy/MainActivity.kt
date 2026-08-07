package com.yashjayswal.dairy

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.yashjayswal.dairy.data.repository.EntryRepository
import com.yashjayswal.dairy.ui.chat.ChatScreen
import com.yashjayswal.dairy.ui.entry.EntryScreen
import com.yashjayswal.dairy.ui.theme.DairyTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val entryRepository = (application as DairyApplication).entryRepository
        setContent {
            DairyTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    DairyApp(entryRepository)
                }
            }
        }
    }
}

private enum class Screen(val label: String) {
    ENTRY("Write"), CHAT("Chat")
}

@Composable
private fun DairyApp(entryRepository: EntryRepository) {
    var screen by remember { mutableStateOf(Screen.ENTRY) }

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = screen.ordinal) {
            Screen.entries.forEach { option ->
                Tab(
                    selected = screen == option,
                    onClick = { screen = option },
                    text = { Text(option.label) }
                )
            }
        }
        when (screen) {
            Screen.ENTRY -> EntryScreen(entryRepository, modifier = Modifier.weight(1f))
            Screen.CHAT -> ChatScreen(modifier = Modifier.weight(1f))
        }
    }
}
