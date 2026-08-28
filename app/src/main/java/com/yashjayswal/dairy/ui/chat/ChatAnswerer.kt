package com.yashjayswal.dairy.ui.chat

import com.yashjayswal.dairy.ai.llm.GemmaInferenceEngine
import com.yashjayswal.dairy.ai.rag.RagPromptBuilder
import com.yashjayswal.dairy.ai.rag.RagRetriever

/**
 * The actual "answer this chat message" pipeline — retrieve, build the
 * prompt, generate — pulled out of [ChatScreen]'s Composable so it's
 * unit-testable on the JVM with fakes, instead of only exercisable through
 * UI automation or a real on-device instrumented test.
 */
object ChatAnswerer {
    suspend fun answer(
        question: String,
        ragRetriever: RagRetriever,
        getGemmaInferenceEngine: suspend () -> GemmaInferenceEngine
    ): String = try {
        val contextEntries = ragRetriever.retrieve(question)
        val prompt = RagPromptBuilder.build(question, contextEntries)
        getGemmaInferenceEngine().generate(prompt)
    } catch (e: Exception) {
        "Sorry, something went wrong answering that: ${e.message}"
    }
}
