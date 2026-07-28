package com.yashjayswal.dairy.data.repository

import com.yashjayswal.dairy.ai.embedding.EmbeddingEngine
import com.yashjayswal.dairy.data.local.dao.EntryDao
import com.yashjayswal.dairy.domain.model.DiaryEntry

class EntryRepository(
    private val entryDao: EntryDao,
    private val embeddingEngine: EmbeddingEngine
) {
    // TODO: save(entry) -> embed text via embeddingEngine, map DiaryEntry to
    // EntryEntity (see EmbeddingCodec for the FloatArray <-> ByteArray step),
    // insert via entryDao. observeAll() -> map EntryEntity rows back to
    // DiaryEntry for the UI layer.
}
