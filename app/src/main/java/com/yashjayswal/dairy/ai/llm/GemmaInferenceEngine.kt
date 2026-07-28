package com.yashjayswal.dairy.ai.llm

/**
 * Prompt in, generated text out. Implementation TODO: wrap
 * com.google.mediapipe:tasks-genai's LlmInference, loading a Gemma .task
 * model from local storage. See docs/REQUIREMENTS.md for model options.
 */
interface GemmaInferenceEngine {
    suspend fun generate(prompt: String): String
}
