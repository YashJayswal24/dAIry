package com.yashjayswal.dairy.ai.rag

import org.junit.Assert.assertTrue
import org.junit.Test

class RagPromptBuilderTest {

    @Test
    fun `includes the question and every context entry`() {
        val prompt = RagPromptBuilder.build(
            question = "How was I feeling last week?",
            contextEntries = listOf("Felt great today.", "Rough day at work.")
        )

        assertTrue(prompt.contains("How was I feeling last week?"))
        assertTrue(prompt.contains("Felt great today."))
        assertTrue(prompt.contains("Rough day at work."))
    }

    @Test
    fun `numbers entries in order`() {
        val prompt = RagPromptBuilder.build(
            question = "q",
            contextEntries = listOf("first", "second")
        )

        val firstIndex = prompt.indexOf("1. first")
        val secondIndex = prompt.indexOf("2. second")
        assertTrue(firstIndex >= 0 && secondIndex > firstIndex)
    }

    @Test
    fun `empty context asks the model to chat normally instead of refusing`() {
        val prompt = RagPromptBuilder.build(question = "hi", contextEntries = emptyList())

        assertTrue(prompt.contains("chat with"))
        assertTrue(prompt.contains("don't invent or assume"))
        assertTrue(prompt.contains("hi"))
    }

    @Test
    fun `non-empty context tells the model entries are optional context, not mandatory`() {
        val prompt = RagPromptBuilder.build(question = "hi", contextEntries = listOf("Went for a run."))

        assertTrue(prompt.contains("Use them if they"))
        assertTrue(prompt.contains("reply normally"))
    }
}
