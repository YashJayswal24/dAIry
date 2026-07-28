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
    suspend fun save(text: String, emotion: Emotion, emotionIntensity: Int): Long {
        val embedding = embeddingEngine.embed(text)
        return entryDao.insert(
            EntryEntity(
                text = text,
                emotion = emotion.name,
                emotionIntensity = emotionIntensity,
                createdAt = System.currentTimeMillis(),
                embedding = embedding.toByteArray()
            )
        )
    }

    fun observeAll(): Flow<List<DiaryEntry>> =
        entryDao.observeAll().map { rows -> rows.map { it.toDomain() } }

    private fun EntryEntity.toDomain() = DiaryEntry(
        id = id,
        text = text,
        emotion = Emotion.valueOf(emotion),
        emotionIntensity = emotionIntensity,
        createdAt = createdAt,
        embedding = embedding.toFloatArray()
    )
}
