package com.yashjayswal.dairy.ai.embedding

import android.content.Context
import com.google.mediapipe.tasks.text.textembedder.TextEmbedder
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * [EmbeddingEngine] backed by MediaPipe's Text Embedder task. The model file
 * is never bundled in the APK (see .gitignore / docs/REQUIREMENTS.md) — it's
 * expected at [resolveModelFile] on the device, pushed there per
 * docs/RUNNING_ON_DEVICE.md step 8.
 */
class MediaPipeEmbeddingEngine(private val context: Context) : EmbeddingEngine {

    private val initLock = Mutex()
    private var textEmbedder: TextEmbedder? = null

    override suspend fun embed(text: String): FloatArray = withContext(Dispatchers.IO) {
        val embedder = textEmbedder ?: initLock.withLock {
            textEmbedder ?: createTextEmbedder().also { textEmbedder = it }
        }
        embedder.embed(text).embeddingResult().embeddings().first().floatEmbedding()
    }

    private fun createTextEmbedder(): TextEmbedder {
        val modelFile = requireModelFile(resolveModelFile(context.getExternalFilesDir(MODELS_SUBDIR)))
        return TextEmbedder.createFromFile(context, modelFile)
    }

    companion object {
        const val MODEL_FILE_NAME = "embedding_model.tflite"
        private const val MODELS_SUBDIR = "models"

        /** Pure path logic, kept separate from Context/MediaPipe so it's unit-testable. */
        internal fun resolveModelFile(modelsDir: File?): File {
            checkNotNull(modelsDir) {
                "External storage is unavailable; cannot locate the embedding model."
            }
            return File(modelsDir, MODEL_FILE_NAME)
        }

        internal fun requireModelFile(modelFile: File): File {
            check(modelFile.exists()) {
                "Embedding model not found at ${modelFile.absolutePath}. " +
                    "See docs/RUNNING_ON_DEVICE.md step 8 for how to push it to the device."
            }
            return modelFile
        }
    }
}
