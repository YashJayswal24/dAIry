package com.yashjayswal.dairy

import android.app.Application
import androidx.room.Room
import com.yashjayswal.dairy.ai.embedding.EmbeddingEngine
import com.yashjayswal.dairy.ai.embedding.MediaPipeEmbeddingEngine
import com.yashjayswal.dairy.data.local.DairyDatabase
import com.yashjayswal.dairy.data.repository.EntryRepository

class DairyApplication : Application() {

    lateinit var entryRepository: EntryRepository
        private set

    override fun onCreate() {
        super.onCreate()
        val database = Room.databaseBuilder(this, DairyDatabase::class.java, "dairy.db").build()
        val embeddingEngine: EmbeddingEngine = MediaPipeEmbeddingEngine(this)
        entryRepository = EntryRepository(database.entryDao(), embeddingEngine)
        // TODO: construct RagRetriever + GemmaInferenceEngineProvider here
        // once ChatScreen is wired up to real generation (see ui/chat).
    }
}
