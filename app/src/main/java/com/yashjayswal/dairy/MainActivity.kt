package com.yashjayswal.dairy

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.yashjayswal.dairy.ai.llm.GemmaInferenceEngine
import com.yashjayswal.dairy.ai.rag.RagRetriever
import com.yashjayswal.dairy.data.repository.EntryRepository
import com.yashjayswal.dairy.ui.chat.ChatScreen
import com.yashjayswal.dairy.ui.entry.EntryScreen
import com.yashjayswal.dairy.ui.theme.DairyTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = application as DairyApplication
        setContent {
            DairyTheme {
                Surface {
                    DairyApp(app.entryRepository, app.ragRetriever, app::gemmaInferenceEngine)
                }
            }
        }
    }
}

private enum class Screen(val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    ENTRY("Write", Icons.Filled.Edit),
    CHAT("Ask", Icons.AutoMirrored.Filled.Chat)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DairyApp(
    entryRepository: EntryRepository,
    ragRetriever: RagRetriever,
    getGemmaInferenceEngine: suspend () -> GemmaInferenceEngine
) {
    var screen by remember { mutableStateOf(Screen.ENTRY) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("dAIry") },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors()
            )
        },
        bottomBar = {
            NavigationBar {
                Screen.entries.forEach { option ->
                    NavigationBarItem(
                        selected = screen == option,
                        onClick = { screen = option },
                        icon = { Icon(option.icon, contentDescription = option.label) },
                        label = { Text(option.label) }
                    )
                }
            }
        }
    ) { innerPadding ->
        when (screen) {
            Screen.ENTRY -> EntryScreen(entryRepository, modifier = Modifier.padding(innerPadding))
            Screen.CHAT -> ChatScreen(ragRetriever, getGemmaInferenceEngine, modifier = Modifier.padding(innerPadding))
        }
    }
}
