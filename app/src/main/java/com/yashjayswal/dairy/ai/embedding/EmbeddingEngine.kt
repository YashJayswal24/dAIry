package com.yashjayswal.dairy.ai.embedding

/**
 * Text in, vector out. Called once per diary entry on save, and once per
 * user question in RagRetriever. Implementation TODO: an on-device embedding
 * model (e.g. EmbeddingGemma) via MediaPipe Tasks / LiteRT — see
 * docs/REQUIREMENTS.md.
 */
interface EmbeddingEngine {
    suspend fun embed(text: String): FloatArray
}
