package com.yashjayswal.dairy.data.repository

import com.yashjayswal.dairy.ai.embedding.EmbeddingEngine
import com.yashjayswal.dairy.data.local.dao.EntryDao
import com.yashjayswal.dairy.data.local.entity.EntryEntity
import com.yashjayswal.dairy.domain.model.Emotion
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

private class FakeEntryDao : EntryDao {
    val inserted = mutableListOf<EntryEntity>()
    private val rows = MutableStateFlow<List<EntryEntity>>(emptyList())

    override suspend fun insert(entry: EntryEntity): Long {
        val withId = entry.copy(id = inserted.size + 1L)
        inserted += withId
        rows.value = inserted.toList()
        return withId.id
    }

    override fun observeAll(): Flow<List<EntryEntity>> = rows
    override suspend fun getAllForSearch(): List<EntryEntity> = rows.value
}

private class FakeEmbeddingEngine(private val vector: FloatArray) : EmbeddingEngine {
    var lastEmbeddedText: String? = null
    override suspend fun embed(text: String): FloatArray {
        lastEmbeddedText = text
        return vector
    }
}

class EntryRepositoryTest {

    @Test
    fun `save embeds the text and persists it with emotion and intensity`() = runTest {
        val dao = FakeEntryDao()
        val embeddingEngine = FakeEmbeddingEngine(floatArrayOf(1f, 2f, 3f))
        val repository = EntryRepository(dao, embeddingEngine)

        repository.save("today was good", Emotion.JOY, 4)

        assertEquals("today was good", embeddingEngine.lastEmbeddedText)
        val saved = dao.inserted.single()
        assertEquals("today was good", saved.text)
        assertEquals("JOY", saved.emotion)
        assertEquals(4, saved.emotionIntensity)
    }

    @Test
    fun `observeAll maps stored rows back into domain entries`() = runTest {
        val dao = FakeEntryDao()
        val repository = EntryRepository(dao, FakeEmbeddingEngine(floatArrayOf(0.5f, -0.5f)))
        repository.save("a calm evening", Emotion.CALM, 2)

        val result = repository.observeAll().first()

        val domainEntry = result.single()
        assertEquals("a calm evening", domainEntry.text)
        assertEquals(Emotion.CALM, domainEntry.emotion)
        assertEquals(2, domainEntry.emotionIntensity)
        assertArrayEquals(floatArrayOf(0.5f, -0.5f), domainEntry.embedding, 0.0001f)
    }
}
