plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.yashjayswal.dairy"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.yashjayswal.dairy"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "0.1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        debug {
            // Standard Android convention: debug and release get distinct
            // application ids so they install side-by-side as separate
            // apps with separate data, instead of silently sharing one
            // data directory (they used to -- see docs/TODO.md incident
            // notes for why that's dangerous with a real personal-data app).
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
        release {
            isMinifyEnabled = true
            // Non-optimizing base rules (proguard-android.txt, not
            // -optimize.txt): R8's aggressive inlining was eliminating
            // stack frames that MediaPipe's native/JNI layer depends on for
            // its own caller-class lookup, crashing with "no caller found
            // on the stack for: <renamed class>" from two different call
            // paths (chat send, entry save) even after keep rules for the
            // specific classes involved -- see docs/TODO.md. This keeps
            // shrinking + renaming (so -keep rules below still matter) but
            // disables the optimize/inline passes that were breaking it.
            proguardFiles(getDefaultProguardFile("proguard-android.txt"), "proguard-rules.pro")
            // Signed with the debug keystore -- installable for personal
            // use/testing, NOT a Play-Store-ready or properly identified
            // signed release. See docs/TODO.md for what a real release
            // keystore would involve.
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }
}

ksp {
    // DairyDatabase has exportSchema = true; this is where Room writes the
    // JSON schema snapshots that migrations get tested against later.
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.4")
    implementation("androidx.activity:activity-compose:1.9.1")

    implementation(platform("androidx.compose:compose-bom:2024.06.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.ui:ui-tooling-preview")

    // Calendar tab month grid (see ui/calendar). 2.10.1 is the actual
    // latest stable, but every version from 2.7.0 up pulls in a Compose
    // UI transitive dependency that requires compileSdk 35 (we're on 34)
    // -- pinned to 2.6.1 (Dec 2023), the newest version still compatible,
    // to avoid an unrelated toolchain upgrade for this one feature. See
    // docs/TODO.md for the compileSdk/AGP bump this should revisit once
    // that's a deliberate decision, not a side effect.
    implementation("com.kizitonwose.calendar:compose:2.6.1")

    // Local storage: diary entries + their embeddings (see data/local)
    implementation("androidx.room:room-runtime:2.8.4")
    implementation("androidx.room:room-ktx:2.8.4")
    ksp("androidx.room:room-compiler:2.8.4")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    // On-device Gemma inference (Google AI Edge) — see ai/llm
    implementation("com.google.mediapipe:tasks-genai:0.10.14")

    // On-device text embeddings (Google AI Edge) — see ai/embedding
    implementation("com.google.mediapipe:tasks-text:0.10.14")

    // On-device Gemini Nano via AICore — used when the device supports it
    // (flagship-tier chips only), falling back to MediaPipe otherwise. See
    // ai/llm/AiCoreGemmaInferenceEngine and ai/llm/GemmaInferenceEngineProvider.
    implementation("com.google.mlkit:genai-prompt:1.0.0-beta2")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")

    // Instrumented tests: run on a real device/emulator (needed for anything
    // that touches actual MediaPipe/on-device inference — see
    // ai/embedding/MediaPipeEmbeddingEngineInstrumentedTest).
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test:runner:1.6.2")
    androidTestImplementation("androidx.test:core:1.6.1")
    androidTestImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")
}
