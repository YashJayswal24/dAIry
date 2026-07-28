package com.yashjayswal.dairy

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.yashjayswal.dairy.ui.entry.EntryScreen
import com.yashjayswal.dairy.ui.theme.DairyTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            DairyTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    EntryScreen()
                }
            }
        }
    }
}
