package com.yashjayswal.dairy.ui.chat

import com.yashjayswal.dairy.ai.embedding.EmbeddingEngine
import com.yashjayswal.dairy.ai.llm.GemmaInferenceEngine
import com.yashjayswal.dairy.ai.rag.RagRetriever
import com.yashjayswal.dairy.data.local.dao.EntryDao
import com.yashjayswal.dairy.data.local.entity.EntryEntity
import com.yashjayswal.dairy.data.local.toByteArray
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

private class FakeEntryDao(private val entries: List<EntryEntity>) : EntryDao {
    override suspend fun insert(entry: EntryEntity): Long = error("not used in this test")
    override fun observeAll(): Flow<List<EntryEntity>> = flowOf(entries)
    override suspend fun getAllForSearch(): List<EntryEntity> = entries
}

private class FakeEmbeddingEngine : EmbeddingEngine {
    override suspend fun embed(text: String): FloatArray = floatArrayOf(1f, 0f)
}

private class FakeGemmaInferenceEngine(
    private val onGenerate: (String) -> String = { "fake reply" }
) : GemmaInferenceEngine {
    var lastPrompt: String? = null
    override suspend fun generate(prompt: String): String {
        lastPrompt = prompt
        return onGenerate(prompt)
    }
}

class ChatAnswererTest {

    private fun entry(text: String) = EntryEntity(
        text = text,
        emotion = "NEUTRAL",
        emotionIntensity = 3,
        createdAt = 0L,
        embedding = floatArrayOf(1f, 0f).toByteArray()
    )

    @Test
    fun `retrieves context, builds a prompt, and returns the engine's reply`() = runTest {
        val retriever = RagRetriever(FakeEntryDao(listOf(entry("Went for a run."))), FakeEmbeddingEngine())
        val engine = FakeGemmaInferenceEngine { "You went for a run!" }

        val reply = ChatAnswerer.answer("What did I do today?", retriever) { engine }

        assertEquals("You went for a run!", reply)
        assertTrue(engine.lastPrompt!!.contains("Went for a run."))
        assertTrue(engine.lastPrompt!!.contains("What did I do today?"))
    }

    @Test
    fun `an exception from retrieval or generation becomes a friendly reply instead of crashing`() = runTest {
        val retriever = RagRetriever(FakeEntryDao(emptyList()), FakeEmbeddingEngine())

        val reply = ChatAnswerer.answer("hi", retriever) {
            throw IllegalStateException("no engine available")
        }

        assertTrue(reply.contains("no engine available"))
    }
}
