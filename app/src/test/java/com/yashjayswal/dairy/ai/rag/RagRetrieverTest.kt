package com.yashjayswal.dairy.ai.rag

import com.yashjayswal.dairy.ai.embedding.EmbeddingEngine
import com.yashjayswal.dairy.data.local.dao.EntryDao
import com.yashjayswal.dairy.data.local.entity.EntryEntity
import com.yashjayswal.dairy.data.local.toByteArray
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

private class FakeEntryDao(private val entries: List<EntryEntity>) : EntryDao {
    override suspend fun insert(entry: EntryEntity): Long = error("not used in this test")
    override fun observeAll(): Flow<List<EntryEntity>> = flowOf(entries)
    override suspend fun getAllForSearch(): List<EntryEntity> = entries
    override suspend fun update(
        id: Long,
        title: String,
        text: String,
        emotion: String,
        emotionIntensity: Int,
        embedding: ByteArray
    ): Unit = error("not used in this test")
    override suspend fun delete(id: Long): Unit = error("not used in this test")
}

private class FakeEmbeddingEngine(private val vector: FloatArray) : EmbeddingEngine {
    override suspend fun embed(text: String): FloatArray = vector
}

class RagRetrieverTest {

    private fun entry(text: String, vector: FloatArray) = EntryEntity(
        text = text,
        emotion = "NEUTRAL",
        emotionIntensity = 3,
        createdAt = 0L,
        embedding = vector.toByteArray()
    )

    @Test
    fun `ranks entries by cosine similarity, most similar first`() = runTest {
        val query = floatArrayOf(1f, 0f)
        val exactMatch = entry("matches the query", floatArrayOf(1f, 0f))
        val orthogonal = entry("unrelated", floatArrayOf(0f, 1f))
        val opposite = entry("opposite", floatArrayOf(-1f, 0f))
        val dao = FakeEntryDao(listOf(orthogonal, opposite, exactMatch))
        val retriever = RagRetriever(dao, FakeEmbeddingEngine(query))

        val result = retriever.retrieve("does this match?", topK = 3)

        assertEquals(listOf("matches the query", "unrelated", "opposite"), result)
    }

    @Test
    fun `topK limits the number of results returned`() = runTest {
        val query = floatArrayOf(1f, 0f)
        val entries = (1..10).map { entry("entry $it", floatArrayOf(1f, 0f)) }
        val dao = FakeEntryDao(entries)
        val retriever = RagRetriever(dao, FakeEmbeddingEngine(query))

        val result = retriever.retrieve("question", topK = 3)

        assertEquals(3, result.size)
    }

    @Test
    fun `empty diary returns empty results`() = runTest {
        val dao = FakeEntryDao(emptyList())
        val retriever = RagRetriever(dao, FakeEmbeddingEngine(floatArrayOf(1f, 0f)))

        val result = retriever.retrieve("anything")

        assertEquals(emptyList<String>(), result)
    }
}
