package com.yashjayswal.dairy.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.yashjayswal.dairy.data.local.entity.EntryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EntryDao {
    @Insert
    suspend fun insert(entry: EntryEntity): Long

    @Query("SELECT * FROM entries ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<EntryEntity>>

    // Used by RagRetriever for the brute-force cosine similarity scan.
    @Query("SELECT * FROM entries")
    suspend fun getAllForSearch(): List<EntryEntity>

    // createdAt is deliberately left untouched -- editing an entry
    // changes what it says, not when it was written.
    @Query(
        "UPDATE entries SET title = :title, text = :text, emotion = :emotion, " +
            "emotionIntensity = :emotionIntensity, embedding = :embedding WHERE id = :id"
    )
    suspend fun update(
        id: Long,
        title: String,
        text: String,
        emotion: String,
        emotionIntensity: Int,
        embedding: ByteArray
    )

    @Query("DELETE FROM entries WHERE id = :id")
    suspend fun delete(id: Long)
}
