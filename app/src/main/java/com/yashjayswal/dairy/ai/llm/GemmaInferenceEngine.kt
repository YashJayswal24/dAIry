package com.yashjayswal.dairy.ai.llm

/**
 * Prompt in, generated text out. Real implementation: [MediaPipeGemmaInferenceEngine].
 * See docs/REQUIREMENTS.md for model options and docs/RUNNING_ON_DEVICE.md
 * for how the model file gets onto a device.
 */
interface GemmaInferenceEngine {
    suspend fun generate(prompt: String): String
}
