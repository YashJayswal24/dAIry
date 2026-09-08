package com.yashjayswal.dairy

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.vector.ImageVector
import com.yashjayswal.dairy.ai.llm.GemmaInferenceEngine
import com.yashjayswal.dairy.ai.rag.RagRetriever
import com.yashjayswal.dairy.data.repository.EntryRepository
import com.yashjayswal.dairy.ui.chat.ChatScreen
import com.yashjayswal.dairy.ui.entry.EntryScreen
import com.yashjayswal.dairy.ui.theme.DairyTheme
import com.yashjayswal.dairy.ui.theme.ThemeMode
import com.yashjayswal.dairy.ui.theme.ThemePreferences

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = application as DairyApplication
        setContent {
            var themeMode by remember { mutableStateOf(ThemePreferences.load(this)) }

            DairyTheme(themeMode) {
                Surface {
                    DairyApp(
                        entryRepository = app.entryRepository,
                        ragRetriever = app.ragRetriever,
                        getGemmaInferenceEngine = app::gemmaInferenceEngine,
                        themeMode = themeMode,
                        onToggleTheme = {
                            themeMode = themeMode.next()
                            ThemePreferences.save(this, themeMode)
                        }
                    )
                }
            }
        }
    }
}

private enum class Screen(val label: String, val icon: ImageVector) {
    ENTRY("Write", Icons.Filled.Edit),
    CHAT("Ask", Icons.AutoMirrored.Filled.Chat)
}

private fun ThemeMode.icon(): ImageVector = when (this) {
    ThemeMode.SYSTEM -> Icons.Filled.BrightnessAuto
    ThemeMode.LIGHT -> Icons.Filled.LightMode
    ThemeMode.DARK -> Icons.Filled.DarkMode
}

private fun ThemeMode.description(): String = when (this) {
    ThemeMode.SYSTEM -> "Theme: follows system, tap to switch to light"
    ThemeMode.LIGHT -> "Theme: light, tap to switch to dark"
    ThemeMode.DARK -> "Theme: dark, tap to switch to system"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DairyApp(
    entryRepository: EntryRepository,
    ragRetriever: RagRetriever,
    getGemmaInferenceEngine: suspend () -> GemmaInferenceEngine,
    themeMode: ThemeMode,
    onToggleTheme: () -> Unit
) {
    var screen by remember { mutableStateOf(Screen.ENTRY) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("dAIry") },
                actions = {
                    IconButton(onClick = onToggleTheme) {
                        Icon(themeMode.icon(), contentDescription = themeMode.description())
                    }
                },
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
