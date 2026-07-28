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
}
