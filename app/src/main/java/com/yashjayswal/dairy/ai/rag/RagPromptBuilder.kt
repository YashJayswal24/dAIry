package com.yashjayswal.dairy.ai.rag

/**
 * Builds the prompt sent to the LLM from a question plus the entries
 * [RagRetriever] found for it. Simple version, on purpose: the question is
 * embedded and searched as-is — no LLM-driven query rewriting/keyword
 * extraction step first. See the Roadmap in README.md for why (dense
 * embeddings already handle paraphrasing; an extra LLM call would double
 * on-device generation latency for a benefit that mainly shows up with
 * multi-turn follow-up questions, which this chat doesn't have yet).
 */
object RagPromptBuilder {
    fun build(question: String, contextEntries: List<String>): String {
        if (contextEntries.isEmpty()) {
            return "You are the user's personal diary assistant. The diary has no " +
                "entries yet, so you have no context to draw on. Politely tell the " +
                "user there's nothing to answer from yet instead of guessing. " +
                "Their question was: \"$question\""
        }

        val context = contextEntries
            .mapIndexed { index, entry -> "${index + 1}. $entry" }
            .joinToString("\n")

        return """
            You are the user's personal diary assistant. Answer the question
            using ONLY the diary entries below as context. If the entries
            don't contain enough information to answer, say so honestly
            instead of guessing.

            Diary entries:
            $context

            Question: $question
        """.trimIndent()
    }
}
