package com.yashjayswal.dairy.ai.llm

import android.util.Log
import com.google.mlkit.genai.common.DownloadStatus
import com.google.mlkit.genai.common.FeatureStatus
import com.google.mlkit.genai.prompt.Generation
import com.google.mlkit.genai.prompt.GenerativeModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.withContext

private const val TAG = "AiCoreGemmaEngine"

/**
 * [GemmaInferenceEngine] backed by Android's AICore system service (Gemini
 * Nano via ML Kit's Prompt API) instead of a bundled model file. Only
 * available on flagship-tier chips — see [createIfAvailable] and
 * [GemmaInferenceEngineProvider], which picks this when usable and falls
 * back to [MediaPipeGemmaInferenceEngine] otherwise. Model download and
 * updates are managed entirely by the OS/Play services, outside this app's
 * control — see docs/HUMAN_IN_THE_LOOP.md for why [MediaPipeGemmaInferenceEngine]
 * (an explicit, app-controlled model file) remains the default path.
 */
class AiCoreGemmaInferenceEngine private constructor(
    private val model: GenerativeModel
) : GemmaInferenceEngine {

    override suspend fun generate(prompt: String): String = withContext(Dispatchers.IO) {
        model.generateContent(prompt).candidates.first().text
    }

    fun close() = model.close()

    internal enum class AvailabilityDecision { USE_IMMEDIATELY, ATTEMPT_DOWNLOAD, UNAVAILABLE }

    companion object {

        /**
         * Returns an engine if this device supports AICore's Prompt API,
         * downloading the feature first if it's downloadable but not yet
         * present. Returns null (device unsupported, or download failed) if
         * it should not be used — callers should fall back to
         * [MediaPipeGemmaInferenceEngine] in that case.
         */
        suspend fun createIfAvailable(): AiCoreGemmaInferenceEngine? = withContext(Dispatchers.IO) {
            val model = Generation.getClient()
            when (decideAvailability(model.checkStatus())) {
                AvailabilityDecision.USE_IMMEDIATELY -> AiCoreGemmaInferenceEngine(model)
                AvailabilityDecision.UNAVAILABLE -> {
                    model.close()
                    null
                }
                AvailabilityDecision.ATTEMPT_DOWNLOAD -> {
                    model.download().collect { status ->
                        when (status) {
                            is DownloadStatus.DownloadStarted -> Log.d(TAG, "Gemini Nano download started")
                            is DownloadStatus.DownloadProgress ->
                                Log.d(TAG, "Gemini Nano download: ${status.totalBytesDownloaded} bytes")
                            is DownloadStatus.DownloadCompleted -> Log.d(TAG, "Gemini Nano download complete")
                            is DownloadStatus.DownloadFailed ->
                                Log.w(TAG, "Gemini Nano download failed", status.e)
                        }
                    }
                    if (model.checkStatus() == FeatureStatus.AVAILABLE) {
                        AiCoreGemmaInferenceEngine(model)
                    } else {
                        model.close()
                        null
                    }
                }
            }
        }

        /** Pure branching logic, kept separate from the real client so it's unit-testable. */
        internal fun decideAvailability(@FeatureStatus status: Int): AvailabilityDecision = when (status) {
            FeatureStatus.AVAILABLE -> AvailabilityDecision.USE_IMMEDIATELY
            FeatureStatus.DOWNLOADABLE, FeatureStatus.DOWNLOADING -> AvailabilityDecision.ATTEMPT_DOWNLOAD
            else -> AvailabilityDecision.UNAVAILABLE
        }
    }
}
