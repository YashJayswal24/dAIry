package com.yashjayswal.dairy.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "entries")
data class EntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String = "",
    val text: String,
    val emotion: String,
    val emotionIntensity: Int,
    val createdAt: Long,
    val embedding: ByteArray // see EmbeddingCodec for the FloatArray <-> ByteArray conversion
)
