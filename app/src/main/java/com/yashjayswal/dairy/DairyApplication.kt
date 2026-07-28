package com.yashjayswal.dairy

import android.app.Application

class DairyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // TODO: construct DairyDatabase, EmbeddingEngine and GemmaInferenceEngine
        // singletons here once implemented, and pass them down to the UI layer.
    }
}
