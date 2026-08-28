package com.yashjayswal.dairy.ai.rag

/**
 * Builds the prompt sent to the LLM from a question plus the entries
 * [RagRetriever] found for it. Simple version, on purpose: the question is
 * embedded and searched as-is — no LLM-driven query rewriting/keyword
 * extraction step first. See the Roadmap in README.md for why (dense
 * embeddings already handle paraphrasing; an extra LLM call would double
 * on-device generation latency for a benefit that mainly shows up with
 * multi-turn follow-up questions, which this chat doesn't have yet).
 *
 * This is a conversational agent that *has* retrieval, not a strict RAG
 * refusal bot: [RagRetriever] always returns its top-K entries regardless of
 * how relevant they actually are (it has no similarity cutoff), so the model
 * is told to use them only when they're actually relevant to the message —
 * a plain "hi" should get a normal reply, not a forced diary reference or a
 * refusal.
 */
object RagPromptBuilder {
    fun build(question: String, contextEntries: List<String>): String {
        if (contextEntries.isEmpty()) {
            return "You are the user's personal diary assistant: a warm, " +
                "conversational agent that can also look up their past diary " +
                "entries. They don't have any entries yet, so just chat with " +
                "them normally — don't invent or assume anything about their " +
                "diary. Their message was: \"$question\""
        }

        val context = contextEntries
            .mapIndexed { index, entry -> "${index + 1}. $entry" }
            .joinToString("\n")

        return """
            You are the user's personal diary assistant: a warm, conversational
            agent that can also look up their past diary entries. Below are
            entries that might be relevant to their message. Use them if they
            actually help answer; if the message is just small talk or doesn't
            need them, reply normally like any conversational assistant would —
            don't force in a diary reference where it doesn't belong, and don't
            claim something the entries don't actually support.

            Possibly relevant diary entries:
            $context

            Message: $question
        """.trimIndent()
    }
}
