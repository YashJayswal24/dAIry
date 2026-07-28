# Architecture

## Layers

- **`ui/`** — Jetpack Compose screens. `entry/` for writing, `chat/` for
  talking to the diary. No business logic here — screens call into
  `data/repository` and `ai/`.
- **`domain/model/`** — plain Kotlin data classes (`DiaryEntry`, `Emotion`)
  with no Android or Room dependency. This is the shape the rest of the app
  thinks in.
- **`data/local/`** — Room database. `EntryEntity` is the on-disk row shape
  (includes the embedding as a `ByteArray`); `EntryDao` is the only thing
  allowed to touch SQL. `EmbeddingCodec.kt` converts between `FloatArray`
  (what the rest of the app uses) and `ByteArray` (what Room stores).
- **`data/repository/`** — the only bridge between `domain` and `data/local`.
  Owns the "embed on save" step: when a `DiaryEntry` is saved, the repository
  calls `EmbeddingEngine` before writing the row.
- **`ai/embedding/`** — `EmbeddingEngine` interface: text in, vector out.
  Used both when saving an entry and when asking a question.
- **`ai/llm/`** — `GemmaInferenceEngine` interface: prompt in, generated text
  out. Wraps the MediaPipe LLM Inference API so nothing else in the app needs
  to know about MediaPipe directly.
- **`ai/rag/`** — `RagRetriever`: given a question, embeds it, does a
  brute-force cosine-similarity scan over every stored entry's embedding,
  and returns the top-k most relevant entries' text. This is the entire
  "retrieval" half of RAG — deliberately just a loop and a sort, no vector
  database.

## Data flow

**Writing an entry**

```
UI (EntryScreen)
  -> EntryRepository.save(text, emotion, intensity)
       -> EmbeddingEngine.embed(text)            // ai/embedding
       -> EntryDao.insert(EntryEntity(...))       // data/local
```

**Asking a question**

```
UI (ChatScreen)
  -> RagRetriever.retrieve(question, topK)
       -> EmbeddingEngine.embed(question)
       -> EntryDao.getAllForSearch()
       -> cosine similarity + sort + take(topK)
  -> build prompt: instructions + retrieved entries + question
  -> GemmaInferenceEngine.generate(prompt)
  -> answer rendered in ChatScreen
```

## Why no vector database

At the scale a personal diary actually reaches (thousands to tens of
thousands of entries over years), a linear scan computing cosine similarity
in plain Kotlin finishes in tens of milliseconds — imperceptible next to the
seconds an on-device LLM takes to generate a response. Bringing in a vector
database (e.g. ObjectBox's HNSW index) would buy sub-millisecond retrieval at
the cost of a proprietary native dependency, extra APK size, and a much less
auditable "black box" index. Given [the project's human-in-the-loop
requirement](HUMAN_IN_THE_LOOP.md), the extra speed isn't worth what it costs
in transparency at this scale. This can be revisited if real usage ever shows
the linear scan is actually a bottleneck.

## Model boundary

Nothing in `ai/` assumes a specific Gemma checkpoint. `GemmaInferenceEngine`
and `EmbeddingEngine` are interfaces precisely so the model file itself (which
model, which quantization, which size) is a runtime/deployment concern, not a
compile-time one — see [REQUIREMENTS.md](REQUIREMENTS.md) for current model
options and their tradeoffs.
