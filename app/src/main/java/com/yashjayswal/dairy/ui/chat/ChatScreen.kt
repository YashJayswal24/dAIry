package com.yashjayswal.dairy.ui.chat

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.yashjayswal.dairy.ai.llm.GemmaInferenceEngine
import com.yashjayswal.dairy.ai.rag.RagRetriever
import kotlinx.coroutines.launch

data class ChatMessage(val text: String, val isFromUser: Boolean)

/**
 * On Send: [ChatAnswerer] retrieves relevant entries, builds the prompt, and
 * generates a reply via [GemmaInferenceEngine] (picked and cached by
 * [com.yashjayswal.dairy.DairyApplication.gemmaInferenceEngine] — the first
 * call on a device may be slow since it can involve an AICore feature
 * check/download).
 */
@Composable
fun ChatScreen(
    ragRetriever: RagRetriever,
    getGemmaInferenceEngine: suspend () -> GemmaInferenceEngine,
    modifier: Modifier = Modifier
) {
    var input by remember { mutableStateOf("") }
    var isGenerating by remember { mutableStateOf(false) }
    val messages = remember { mutableStateListOf<ChatMessage>() }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        if (messages.isEmpty()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text(
                    "Ask me anything about your diary — or just say hi.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(messages) { message -> ChatBubble(message) }
                if (isGenerating) {
                    item { ChatBubble(ChatMessage(text = "Thinking…", isFromUser = false)) }
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                placeholder = { Text("Ask a question...") },
                enabled = !isGenerating,
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(8.dp))
            Button(
                onClick = {
                    val question = input
                    if (question.isBlank()) return@Button
                    messages.add(ChatMessage(text = question, isFromUser = true))
                    input = ""
                    isGenerating = true
                    coroutineScope.launch {
                        listState.animateScrollToItem(messages.lastIndex)
                        val reply = ChatAnswerer.answer(question, ragRetriever, getGemmaInferenceEngine)
                        messages.add(ChatMessage(text = reply, isFromUser = false))
                        isGenerating = false
                        listState.animateScrollToItem(messages.lastIndex)
                    }
                },
                enabled = input.isNotBlank() && !isGenerating
            ) {
                Text("Send")
            }
        }
    }
}

@Composable
private fun ChatBubble(message: ChatMessage) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.isFromUser) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            color = if (message.isFromUser) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            },
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            SelectionContainer {
                Text(message.text, modifier = Modifier.padding(12.dp))
            }
        }
    }
}
