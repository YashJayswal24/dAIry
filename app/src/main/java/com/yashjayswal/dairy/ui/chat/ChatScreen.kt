package com.yashjayswal.dairy.ui.chat

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ChatScreen(modifier: Modifier = Modifier) {
    Column(modifier = modifier.padding(16.dp)) {
        Text("Ask your diary.")
        // TODO: message list + input box. On send: RagRetriever.retrieve(),
        // build a prompt from the retrieved entries, then
        // GemmaInferenceEngine.generate().
    }
}
