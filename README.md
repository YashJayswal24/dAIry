# dAIry

A private, offline-first journal for Android. You write entries with a text
and an emotion + intensity, everything is stored locally on your device, and
you can later *talk to your diary* — ask it questions and get answers grounded
in your own past entries — using a Gemma model running entirely on-device.

No entry, embedding, or question ever needs to leave the phone.

> **Status: early scaffold.** This repo is currently a shell/skeleton — project
> structure, build files, and placeholder classes with a clear architecture,
> not a working app yet. See [Roadmap](#roadmap).

## Features (target)

- Write entries with free text + an emotion label and 1–5 intensity.
- Everything stored locally (Room/SQLite) — no account, no server, no sync.
- Ask natural-language questions about your own diary; a RAG pipeline finds
  the most relevant past entries and an on-device Gemma model answers using
  only that retrieved context.
- Fully offline after the model is downloaded once.

## How it works

```
Write entry ──► embed text ──► store {text, emotion, intensity, embedding}
                                         (Room / SQLite, on-device)

Ask question ──► embed question ──► cosine similarity over stored
                                     embeddings (brute force, in-app)
                                         │
                                         ▼
                              top-k most relevant entries
                                         │
                                         ▼
                          prompt Gemma (MediaPipe LLM Inference)
                                         │
                                         ▼
                                    answer shown in chat
```

Full write-up in [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md).

## Tech stack

| Layer | Choice |
|---|---|
| Language / UI | Kotlin, Jetpack Compose, Material 3 |
| Local storage | Room (SQLite) — entries + their embeddings live in one table |
| Retrieval (RAG) | Brute-force cosine similarity, computed in-app (no vector DB dependency) |
| On-device LLM | [Google AI Edge — MediaPipe LLM Inference API](https://ai.google.dev/edge/mediapipe/solutions/genai/llm_inference) running a Gemma `.task` model, **or** [ML Kit GenAI Prompt API](https://developers.google.com/ml-kit/genai/prompt/android) (AICore/Gemini Nano) when the device supports it — see [GemmaInferenceEngineProvider](app/src/main/java/com/yashjayswal/dairy/ai/llm/GemmaInferenceEngineProvider.kt) |
| On-device embeddings | [MediaPipe Text Embedder API](https://ai.google.dev/edge/mediapipe/solutions/text/text_embedder/android) running a `.tflite` embedding model |
| Concurrency | Kotlin Coroutines |

Why brute-force cosine similarity instead of a vector database: at realistic
personal-diary scale (thousands to tens of thousands of entries) it's fast
enough (tens of milliseconds), has zero extra native/proprietary
dependencies, and is simple enough to read top-to-bottom — see
[docs/HUMAN_IN_THE_LOOP.md](docs/HUMAN_IN_THE_LOOP.md) for why that matters
for this project specifically.

## Requirements

Full breakdown (APIs, permissions, RAM, storage, min SDK) in
[docs/REQUIREMENTS.md](docs/REQUIREMENTS.md). Short version:

- Android 8.0 (API 26) minimum, Android 12+ recommended for GPU acceleration.
- **~4 GB RAM minimum, 6–8 GB+ recommended** to run a quantized Gemma model
  smoothly alongside the rest of the app.
- The Gemma model file (several hundred MB to a few GB depending on variant)
  is **not** bundled in the APK — it's downloaded/pushed to the device
  separately.
- One-time `INTERNET` permission use for the initial model download only;
  no network access is required for journaling or chatting with your diary.

## Getting started

1. Clone the repo and open it in Android Studio (Ladybug or newer). Android
   Studio will offer to generate the Gradle wrapper on first sync — accept it.
2. Obtain a Gemma `.task` model converted for MediaPipe LLM Inference (see
   [docs/REQUIREMENTS.md](docs/REQUIREMENTS.md) for where to get one and
   which size fits your target device's RAM).
3. Push the model to the device (e.g. `adb push gemma-model.task
   /data/local/tmp/dairy/`) — an in-app downloader/picker is on the roadmap.
4. Build and run `app` on a physical device (emulators are slow for on-device
   LLM inference and may not have enough RAM). Step-by-step device setup
   (Samsung phone, developer options, adb, pushing a model file, battery
   optimization gotchas) is in
   [docs/RUNNING_ON_DEVICE.md](docs/RUNNING_ON_DEVICE.md).

## Project structure

```
app/src/main/java/com/yashjayswal/dairy/
├── data/
│   ├── local/          # Room database, entity, DAO, embedding <-> bytes codec
│   └── repository/      # bridges domain models and Room
├── domain/model/         # DiaryEntry, Emotion
├── ai/
│   ├── embedding/        # EmbeddingEngine interface (text -> vector)
│   ├── llm/              # GemmaInferenceEngine interface (prompt -> text);
│   │                     # MediaPipe + AICore implementations, picked at
│   │                     # runtime by GemmaInferenceEngineProvider
│   └── rag/              # RagRetriever: cosine similarity over stored entries
└── ui/
    ├── entry/            # write-an-entry screen
    ├── chat/             # ask-your-diary screen
    └── theme/
```

## Roadmap

Short summary below — see [docs/TODO.md](docs/TODO.md) for the detailed,
working version (what each item actually involves, and notes for picking
work back up).

- [x] `EntryRepository` (embed-on-save, map rows back to domain model)
- [x] Unit tests for `EmbeddingCodec`, `RagRetriever`, `EntryRepository`
- [x] Implement `EmbeddingEngine` (`MediaPipeEmbeddingEngine`, MediaPipe Text Embedder)
- [x] Implement `GemmaInferenceEngine` — `MediaPipeGemmaInferenceEngine` (bundled `.task` model, any device) plus `AiCoreGemmaInferenceEngine` (Gemini Nano via AICore, flagship devices only), picked at runtime by `GemmaInferenceEngineProvider`
- [x] Emotion + intensity picker UI (`EntryScreen`: text field, emotion chips, 1–5 slider, save button wired to `EntryRepository`, past-entries list)
- [x] Prompt template for RAG — `RagPromptBuilder`, **simple version on purpose**: embeds the user's raw question directly and retrieves with it, no LLM-driven query rewriting/keyword-extraction step first. Dense embeddings already handle paraphrasing, and an extra LLM call would double on-device generation latency. Revisit only if real usage shows retrieval missing relevant entries, or once multi-turn follow-up questions are supported (that's the case query *condensation* — using conversation history to resolve an ambiguous follow-up — genuinely earns its cost, unlike single-shot keyword extraction).
- [x] Chat UI wired end-to-end — `ChatScreen` calls `ChatAnswerer` (`RagRetriever` → `RagPromptBuilder` → the cached `GemmaInferenceEngine`) on send; the wiring and error path are verified on a Galaxy S26 Ultra, real generated replies not yet read (see TODO.md)
- [x] Visual redesign — custom warm color theme (was untouched Material3 defaults), `Scaffold` with a top app bar + icon `NavigationBar`, card-based `EntryScreen` with emoji mood chips, verified on-device in dark mode (see TODO.md for the "My Diary"-inspired future feature list: calendar view, search, tags, etc.)
- [ ] Persist chat history (messages are still in-memory only — lost on navigating away or app restart)
- [ ] Finalize Room schema/migrations
- [ ] In-app model download/selection flow
- [ ] Verify `MediaPipeGemmaInferenceEngine` end-to-end with a real `.task` file — only `AiCoreGemmaInferenceEngine` has been confirmed generating on-device so far; the file pulled from AI Edge Gallery turned out to be `.litertlm`, a different format (see REQUIREMENTS.md)
- [ ] Confirm a successful entry save on a real device — `EntryScreen` → `EntryRepository` → Room is wired and unit-tested, but the on-device tap-to-save round-trip hasn't been confirmed yet (kept getting interrupted by the model file being wiped on reinstall mid-verification)

## Human in the loop

This project is scaffolded with AI assistance, but every design decision,
dependency, and line of logic is meant to be understood and controlled by
the maintainer — not just generated and merged. See
[docs/HUMAN_IN_THE_LOOP.md](docs/HUMAN_IN_THE_LOOP.md).

## License

Licensed under the [Apache License, Version 2.0](LICENSE). You're free to
use, modify, and redistribute this project — including commercially —
provided you retain the copyright/attribution notice (see [NOTICE](NOTICE))
and comply with the License's terms.

Copyright © 2026 [Yash Jayswal](https://github.com/YashJayswal24)
