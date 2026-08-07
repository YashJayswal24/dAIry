# Requirements

Everything in this doc is about what it costs to run an on-device Gemma model
for chat + RAG on Android. The diary storage/UI itself (Room, Compose) has no
unusual requirements — this is entirely about the LLM/embedding layer.

## APIs / SDKs

| Purpose | API | Notes |
|---|---|---|
| Run Gemma on-device | [MediaPipe LLM Inference API](https://ai.google.dev/edge/mediapipe/solutions/genai/llm_inference) (`com.google.mediapipe:tasks-genai`) | Default path, works on any device meeting the RAM/storage requirements below. Takes a `.task` model file you provide (see below). |
| Run Gemini Nano on-device (optional, when supported) | [ML Kit GenAI Prompt API](https://developers.google.com/ml-kit/genai/prompt/android) (`com.google.mlkit:genai-prompt`), backed by Android's AICore system service | Used automatically instead of MediaPipe when the device supports it — see `AiCoreGemmaInferenceEngine` / `GemmaInferenceEngineProvider`. No model file to manage; the OS handles download. **Flagship-tier chips only** (Pixel 8+, Galaxy S24+/Z Fold6+/Z Flip6+, some Snapdragon/Tensor/Dimensity flagships) — most devices will use the MediaPipe path instead. Requires Google Play services, and the calling app must be in the foreground when generating. |
| Text embeddings on-device | [MediaPipe Text Embedder API](https://ai.google.dev/edge/mediapipe/solutions/text/text_embedder/android) (`com.google.mediapipe:tasks-text`) | Used both to embed diary entries on save and to embed questions at query time. Takes a `.tflite` embedding model (e.g. a Universal Sentence Encoder or Gecko/EmbeddingGemma model converted for MediaPipe). |
| Local persistence | Room (AndroidX), backed by SQLite | Already bundled with Android — no extra runtime dependency. |
| Networking | None required at runtime | `INTERNET` permission exists solely for the one-time model download step. |

There is no cloud/inference API involved anywhere — no API keys, no
per-request cost, no rate limits, because inference happens entirely on the
device.

## Model options and their footprint

Gemma is shipped in multiple sizes; MediaPipe LLM Inference expects models
converted to `.task` format, typically quantized (int4/int8) for mobile.
Approximate figures (vary by exact variant/quantization — always check the
current model card before committing to one):

| Model | Approx. file size (int4) | Approx. RAM while running | Fits comfortably on |
|---|---|---|---|
| Gemma 3 1B | ~0.5–1 GB | ~1.5–2 GB | 4–6 GB RAM devices |
| Gemma 2 2B / Gemma 3n (small) | ~1.2–1.5 GB | ~2–3 GB | 6 GB+ RAM devices |
| Gemma 2 4B+ | ~2–3 GB+ | ~3.5–5 GB+ | 8 GB+ RAM devices, flagship-class |

"RAM while running" includes model weights plus the KV cache and runtime
overhead during generation — the actual peak is higher than the file size
alone.

## Device requirements

| Resource | Minimum | Recommended |
|---|---|---|
| RAM | ~4 GB (smallest model variant only, CPU delegate, expect slow generation) | 6–8 GB+ (comfortable headroom, GPU delegate available) |
| Storage | Model file size + ~200 MB for the app itself | Same, plus room for diary growth (see below) |
| Android version | API 26 (Android 8.0) | API 31+ (Android 12+) for better GPU delegate support |
| CPU/GPU | ARM64-v8a (arm64) device | Same, with a GPU that MediaPipe's GPU delegate supports for faster generation |
| Network | Only for the initial model download | — |

Physical devices are strongly recommended over emulators: emulated GPU
delegates are unreliable and emulator RAM ceilings often can't accommodate a
loaded Gemma model comfortably.

## Diary data storage footprint (not the model — your actual entries)

Separate from the model itself, here's what your own journaling data costs,
assuming a 768-dimension embedding per entry (float32, 3 KB/vector) plus a
few hundred bytes of text per entry:

| Entries | Approx. total local storage |
|---|---|
| 1,000 (~3 years daily) | ~3–5 MB |
| 10,000 | ~30–50 MB |
| 100,000 (extreme upper bound) | ~300–500 MB |

This is why brute-force cosine similarity retrieval (see
[ARCHITECTURE.md](ARCHITECTURE.md)) is sufficient: even at 100,000 entries,
a linear similarity scan stays well under 200ms, imperceptible next to LLM
generation time.

## Permissions

| Permission | Why |
|---|---|
| `INTERNET` | Only used for the one-time Gemma model download. Not required for writing entries or chatting with an already-downloaded model. |

No other permissions (storage, microphone, contacts, location, etc.) should
be required. If a future change needs a new permission, it should be
justified in this file before being added — see
[HUMAN_IN_THE_LOOP.md](HUMAN_IN_THE_LOOP.md).

## Where to get a Gemma `.task` model

Converted, ready-to-use Gemma models for MediaPipe LLM Inference are
published on Google's model hub / Kaggle Models and Hugging Face under the
Gemma license — check the current model card for size, quantization, and
license terms for each variant before bundling one into a build or device.
This step is only needed for the MediaPipe path — not needed if the device
uses the AICore path instead (see above).

**Format gotcha:** make sure the file you get is actually `.task`
(MediaPipe's bundle format), not `.litertlm` — a newer, different container
format used by some other tools (e.g. the Google AI Edge Gallery app
downloads `.litertlm`). Renaming a `.litertlm` file to `.task` does not make
it work; `MediaPipeGemmaInferenceEngine`/MediaPipe's native loader will fail
with `Unable to open file` because the container format itself doesn't
match, not because of the extension.

## Where to get an embedding model

MediaPipe's [Text Embedder models page](https://developers.google.com/edge/mediapipe/solutions/text/text_embedder/index#models)
lists two officially hosted options:

| Model | Format | Download |
|---|---|---|
| Universal Sentence Encoder (smaller, what `MediaPipeEmbeddingEngine` is set up for) | `.tflite` | `https://storage.googleapis.com/mediapipe-models/text_embedder/universal_sentence_encoder/float32/latest/universal_sentence_encoder.tflite` |
| EmbeddingGemma 300M (larger, better quality) | `.task` | `https://storage.googleapis.com/mediapipe-models/text_embedder/embedding_gemma/int4int8/latest/embedding_gemma.task` |

Download the Universal Sentence Encoder file, rename it to
`embedding_model.tflite` (the exact name `MediaPipeEmbeddingEngine` looks
for), and push it per [RUNNING_ON_DEVICE.md](RUNNING_ON_DEVICE.md) step 8.
