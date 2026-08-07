package com.yashjayswal.dairy.ai.llm

import android.content.Context
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import com.google.mediapipe.tasks.genai.llminference.LlmInference.LlmInferenceOptions
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * [GemmaInferenceEngine] backed by MediaPipe's LLM Inference task. The model
 * file is never bundled in the APK (see .gitignore / docs/REQUIREMENTS.md) —
 * it's expected at [resolveModelFile] on the device, pushed there per
 * docs/RUNNING_ON_DEVICE.md step 8.
 *
 * Generation params (max tokens / topK / temperature) are fixed constants
 * for now rather than user-configurable — see docs/HUMAN_IN_THE_LOOP.md:
 * a hardcoded, readable default beats a settings screen nobody asked for
 * yet.
 */
class MediaPipeGemmaInferenceEngine(private val context: Context) : GemmaInferenceEngine {

    private val initLock = Mutex()
    private var llmInference: LlmInference? = null

    override suspend fun generate(prompt: String): String = withContext(Dispatchers.IO) {
        val inference = llmInference ?: initLock.withLock {
            llmInference ?: createLlmInference().also { llmInference = it }
        }
        inference.generateResponse(prompt)
    }

    private fun createLlmInference(): LlmInference {
        val modelFile = requireModelFile(resolveModelFile(context.getExternalFilesDir(MODELS_SUBDIR)))
        val options = LlmInferenceOptions.builder()
            .setModelPath(modelFile.absolutePath)
            .setMaxTokens(MAX_TOKENS)
            .setTopK(TOP_K)
            .setTemperature(TEMPERATURE)
            .setRandomSeed(RANDOM_SEED)
            .build()
        return LlmInference.createFromOptions(context, options)
    }

    companion object {
        const val MODEL_FILE_NAME = "gemma-model.task"
        private const val MODELS_SUBDIR = "models"

        // MediaPipe LLM Inference generation defaults. See
        // https://ai.google.dev/edge/mediapipe/solutions/genai/llm_inference/android
        // for what each knob does; these are reasonable general-purpose
        // starting points, not tuned for this app specifically yet.
        private const val MAX_TOKENS = 512
        private const val TOP_K = 40
        private const val TEMPERATURE = 0.8f
        private const val RANDOM_SEED = 0

        /** Pure path logic, kept separate from Context/MediaPipe so it's unit-testable. */
        internal fun resolveModelFile(modelsDir: File?): File {
            checkNotNull(modelsDir) {
                "External storage is unavailable; cannot locate the Gemma model."
            }
            return File(modelsDir, MODEL_FILE_NAME)
        }

        internal fun requireModelFile(modelFile: File): File {
            check(modelFile.exists()) {
                "Gemma model not found at ${modelFile.absolutePath}. " +
                    "See docs/RUNNING_ON_DEVICE.md step 8 for how to push it to the device."
            }
            return modelFile
        }
    }
}
