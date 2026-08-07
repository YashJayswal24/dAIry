package com.yashjayswal.dairy.ai.embedding

/**
 * Text in, vector out. Called once per diary entry on save, and once per
 * user question in RagRetriever. Real implementation: [MediaPipeEmbeddingEngine].
 * See docs/REQUIREMENTS.md for the model this expects and
 * docs/RUNNING_ON_DEVICE.md for how it gets onto a device.
 */
interface EmbeddingEngine {
    suspend fun embed(text: String): FloatArray
}
