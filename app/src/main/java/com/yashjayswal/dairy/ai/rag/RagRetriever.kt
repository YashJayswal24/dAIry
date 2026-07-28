package com.yashjayswal.dairy.ai.rag

import com.yashjayswal.dairy.ai.embedding.EmbeddingEngine
import com.yashjayswal.dairy.data.local.dao.EntryDao
import com.yashjayswal.dairy.data.local.toFloatArray
import kotlin.math.sqrt

/**
 * The retrieval half of RAG: brute-force cosine similarity over every stored
 * entry. See docs/ARCHITECTURE.md for why this isn't a vector database.
 */
class RagRetriever(
    private val entryDao: EntryDao,
    private val embeddingEngine: EmbeddingEngine
) {
    suspend fun retrieve(question: String, topK: Int = 5): List<String> {
        val queryVector = embeddingEngine.embed(question)
        return entryDao.getAllForSearch()
            .map { it to cosineSimilarity(queryVector, it.embedding.toFloatArray()) }
            .sortedByDescending { it.second }
            .take(topK)
            .map { it.first.text }
    }

    private fun cosineSimilarity(a: FloatArray, b: FloatArray): Float {
        var dot = 0f
        var normA = 0f
        var normB = 0f
        for (i in a.indices) {
            dot += a[i] * b[i]
            normA += a[i] * a[i]
            normB += b[i] * b[i]
        }
        return dot / (sqrt(normA) * sqrt(normB) + 1e-8f)
    }
}
