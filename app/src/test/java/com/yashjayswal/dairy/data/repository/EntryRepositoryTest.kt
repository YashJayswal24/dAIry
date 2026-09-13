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

    override suspend fun update(
        id: Long,
        title: String,
        text: String,
        emotion: String,
        emotionIntensity: Int,
        embedding: ByteArray
    ) {
        val index = inserted.indexOfFirst { it.id == id }
        if (index >= 0) {
            inserted[index] = inserted[index].copy(
                title = title,
                text = text,
                emotion = emotion,
                emotionIntensity = emotionIntensity,
                embedding = embedding
            )
            rows.value = inserted.toList()
        }
    }

    override suspend fun delete(id: Long) {
        inserted.removeAll { it.id == id }
        rows.value = inserted.toList()
    }
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
    fun `save embeds the text and persists it with title, emotion, and intensity`() = runTest {
        val dao = FakeEntryDao()
        val embeddingEngine = FakeEmbeddingEngine(floatArrayOf(1f, 2f, 3f))
        val repository = EntryRepository(dao, embeddingEngine)

        repository.save("Good day", "today was good", Emotion.JOY, 4)

        val saved = dao.inserted.single()
        assertEquals("Good day", saved.title)
        assertEquals("today was good", saved.text)
        assertEquals("JOY", saved.emotion)
        assertEquals(4, saved.emotionIntensity)
    }

    @Test
    fun `save embeds title and text together when a title is given`() = runTest {
        val dao = FakeEntryDao()
        val embeddingEngine = FakeEmbeddingEngine(floatArrayOf(1f, 2f, 3f))
        val repository = EntryRepository(dao, embeddingEngine)

        repository.save("Good day", "today was good", Emotion.JOY, 4)

        assertEquals("Good day\ntoday was good", embeddingEngine.lastEmbeddedText)
    }

    @Test
    fun `save embeds only the text when title is blank`() = runTest {
        val dao = FakeEntryDao()
        val embeddingEngine = FakeEmbeddingEngine(floatArrayOf(1f, 2f, 3f))
        val repository = EntryRepository(dao, embeddingEngine)

        repository.save("", "today was good", Emotion.JOY, 4)

        assertEquals("today was good", embeddingEngine.lastEmbeddedText)
    }

    @Test
    fun `save defaults createdAt to now but accepts a backdated value`() = runTest {
        val dao = FakeEntryDao()
        val repository = EntryRepository(dao, FakeEmbeddingEngine(floatArrayOf(1f)))
        val yesterday = System.currentTimeMillis() - 86_400_000

        repository.save("", "backdated entry", Emotion.NEUTRAL, 3, createdAt = yesterday)

        assertEquals(yesterday, dao.inserted.single().createdAt)
    }

    @Test
    fun `observeAll maps stored rows back into domain entries`() = runTest {
        val dao = FakeEntryDao()
        val repository = EntryRepository(dao, FakeEmbeddingEngine(floatArrayOf(0.5f, -0.5f)))
        repository.save("Evening", "a calm evening", Emotion.CALM, 2)

        val result = repository.observeAll().first()

        val domainEntry = result.single()
        assertEquals("Evening", domainEntry.title)
        assertEquals("a calm evening", domainEntry.text)
        assertEquals(Emotion.CALM, domainEntry.emotion)
        assertEquals(2, domainEntry.emotionIntensity)
        assertArrayEquals(floatArrayOf(0.5f, -0.5f), domainEntry.embedding, 0.0001f)
    }

    @Test
    fun `update re-embeds the new title and text, changes fields, and preserves createdAt`() = runTest {
        val dao = FakeEntryDao()
        val embeddingEngine = FakeEmbeddingEngine(floatArrayOf(1f, 2f, 3f))
        val repository = EntryRepository(dao, embeddingEngine)
        val id = repository.save("Original", "original text", Emotion.SADNESS, 2)
        val originalCreatedAt = dao.inserted.single().createdAt

        repository.update(id, "Edited", "edited text", Emotion.JOY, 5)

        assertEquals("Edited\nedited text", embeddingEngine.lastEmbeddedText)
        val updated = dao.inserted.single()
        assertEquals("Edited", updated.title)
        assertEquals("edited text", updated.text)
        assertEquals("JOY", updated.emotion)
        assertEquals(5, updated.emotionIntensity)
        assertEquals(originalCreatedAt, updated.createdAt)
    }

    @Test
    fun `delete removes the entry entirely, embedding included`() = runTest {
        val dao = FakeEntryDao()
        val repository = EntryRepository(dao, FakeEmbeddingEngine(floatArrayOf(1f)))
        val id = repository.save("", "to be deleted", Emotion.NEUTRAL, 3)

        repository.delete(id)

        assertEquals(emptyList<EntryEntity>(), dao.inserted)
    }
}
