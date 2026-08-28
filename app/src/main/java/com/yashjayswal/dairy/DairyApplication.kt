package com.yashjayswal.dairy

import android.app.Application
import androidx.room.Room
import com.yashjayswal.dairy.ai.embedding.EmbeddingEngine
import com.yashjayswal.dairy.ai.embedding.MediaPipeEmbeddingEngine
import com.yashjayswal.dairy.ai.llm.GemmaInferenceEngine
import com.yashjayswal.dairy.ai.llm.GemmaInferenceEngineProvider
import com.yashjayswal.dairy.ai.rag.RagRetriever
import com.yashjayswal.dairy.data.local.DairyDatabase
import com.yashjayswal.dairy.data.repository.EntryRepository
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class DairyApplication : Application() {

    lateinit var entryRepository: EntryRepository
        private set

    lateinit var ragRetriever: RagRetriever
        private set

    private val gemmaEngineInitLock = Mutex()
    private var gemmaEngine: GemmaInferenceEngine? = null

    override fun onCreate() {
        super.onCreate()
        val database = Room.databaseBuilder(this, DairyDatabase::class.java, "dairy.db").build()
        val embeddingEngine: EmbeddingEngine = MediaPipeEmbeddingEngine(this)
        entryRepository = EntryRepository(database.entryDao(), embeddingEngine)
        ragRetriever = RagRetriever(database.entryDao(), embeddingEngine)
    }

    /**
     * Picks AICore or MediaPipe on first call (slow: may check/download an
     * AICore feature) and reuses the same engine after that, so chat isn't
     * re-picking and re-initializing a model on every message.
     */
    suspend fun gemmaInferenceEngine(): GemmaInferenceEngine {
        gemmaEngine?.let { return it }
        return gemmaEngineInitLock.withLock {
            gemmaEngine ?: GemmaInferenceEngineProvider.create(this).also { gemmaEngine = it }
        }
    }
}
