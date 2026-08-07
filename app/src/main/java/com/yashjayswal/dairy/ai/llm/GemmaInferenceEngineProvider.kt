package com.yashjayswal.dairy.ai.llm

import android.content.Context

/**
 * Picks the best available [GemmaInferenceEngine] for this device: AICore's
 * Gemini Nano ([AiCoreGemmaInferenceEngine]) when the device supports it
 * (flagship-tier chips only), otherwise the bundled-model path
 * ([MediaPipeGemmaInferenceEngine], works on any device with a model file
 * pushed per docs/RUNNING_ON_DEVICE.md).
 */
object GemmaInferenceEngineProvider {
    suspend fun create(context: Context): GemmaInferenceEngine =
        AiCoreGemmaInferenceEngine.createIfAvailable() ?: MediaPipeGemmaInferenceEngine(context)
}
