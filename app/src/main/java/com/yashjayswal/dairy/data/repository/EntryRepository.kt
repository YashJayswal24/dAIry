package com.yashjayswal.dairy.data.repository

import com.yashjayswal.dairy.ai.embedding.EmbeddingEngine
import com.yashjayswal.dairy.data.local.dao.EntryDao
import com.yashjayswal.dairy.data.local.entity.EntryEntity
import com.yashjayswal.dairy.data.local.toByteArray
import com.yashjayswal.dairy.data.local.toFloatArray
import com.yashjayswal.dairy.domain.model.DiaryEntry
import com.yashjayswal.dairy.domain.model.Emotion
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class EntryRepository(
    private val entryDao: EntryDao,
    private val embeddingEngine: EmbeddingEngine
) {
    // createdAt defaults to now but can be overridden to backdate an entry
    // (e.g. writing today about yesterday) -- see EntryComposeScreen's date
    // picker.
    suspend fun save(
        title: String,
        text: String,
        emotion: Emotion,
        emotionIntensity: Int,
        createdAt: Long = System.currentTimeMillis()
    ): Long {
        val embedding = embeddingEngine.embed(embeddableText(title, text))
        return entryDao.insert(
            EntryEntity(
                title = title,
                text = text,
                emotion = emotion.name,
                emotionIntensity = emotionIntensity,
                createdAt = createdAt,
                embedding = embedding.toByteArray()
            )
        )
    }

    fun observeAll(): Flow<List<DiaryEntry>> =
        entryDao.observeAll().map { rows -> rows.map { it.toDomain() } }

    // Re-embeds the new title+text so the stored embedding always matches
    // what the entry currently says, not what it said when first saved.
    suspend fun update(id: Long, title: String, text: String, emotion: Emotion, emotionIntensity: Int) {
        val embedding = embeddingEngine.embed(embeddableText(title, text))
        entryDao.update(id, title, text, emotion.name, emotionIntensity, embedding.toByteArray())
    }

    suspend fun delete(id: Long) {
        entryDao.delete(id)
    }

    private fun embeddableText(title: String, text: String): String =
        if (title.isBlank()) text else "$title\n$text"

    private fun EntryEntity.toDomain() = DiaryEntry(
        id = id,
        title = title,
        text = text,
        emotion = Emotion.valueOf(emotion),
        emotionIntensity = emotionIntensity,
        createdAt = createdAt,
        embedding = embedding.toFloatArray()
    )
}
