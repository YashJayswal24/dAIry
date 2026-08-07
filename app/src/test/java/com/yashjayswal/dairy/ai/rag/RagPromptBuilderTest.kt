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
    fun `empty context produces a distinct no-entries prompt instead of an empty context block`() {
        val prompt = RagPromptBuilder.build(question = "Anything interesting?", contextEntries = emptyList())

        assertTrue(prompt.contains("no entries yet"))
        assertTrue(prompt.contains("Anything interesting?"))
    }
}
