# Human-in-the-Loop Policy

This project is scaffolded and written with help from an AI coding assistant.
That does not change who is responsible for it, or who needs to understand it.

## The rule

The human maintainer must, at all times:

- Be able to explain what every module does and why it exists, unprompted —
  no component gets merged just because an AI assistant produced it and it
  compiled.
- Retain full control over what runs on-device: which model is loaded, what
  data (if any) leaves the device, and what every dependency is actually
  for. Nothing here should be a black box to the person who owns the project.
- Be able to reproduce, debug, and modify any part of the codebase without
  depending on the AI tool that helped write it. If the tool disappeared
  tomorrow, the maintainer should still be able to carry the project forward.

## In practice

- Prefer explicit, readable code over clever abstractions — every non-trivial
  file should be understandable end-to-end without needing an explanation
  from whatever generated it.
- New dependencies — especially anything touching the network, storage
  location, model inference, or permissions — get read and understood by the
  maintainer before being added, not just accepted because a tool suggested
  them. See [REQUIREMENTS.md](REQUIREMENTS.md) for the currently-justified
  set.
- Architectural tradeoffs (e.g. why cosine similarity is brute-forced instead
  of using a vector database, see [ARCHITECTURE.md](ARCHITECTURE.md)) are
  recorded with their reasoning, not just their conclusion, so the "why"
  survives past the conversation that produced it.
- One explicit, deliberate exception to "retain full control over which
  model is loaded": `AiCoreGemmaInferenceEngine` uses Android's AICore
  system service (Gemini Nano), where the OS — not this app — controls the
  exact model version and update timing. This was a knowing tradeoff, made
  by the maintainer with the alternative laid out first (see
  [REQUIREMENTS.md](REQUIREMENTS.md)): it's an *optional, secondary* path
  used only on devices that support it, purely to skip manual model-file
  management on flagship hardware. `MediaPipeGemmaInferenceEngine` (an
  explicit, app-controlled `.task` file) remains the default and is what
  every other device falls back to — see `GemmaInferenceEngineProvider`.
- AI-assisted contributions are reviewed the same way any other contribution
  would be: understanding comes before merging, not after.

This file exists so this policy survives beyond any single conversation with
an AI assistant, and applies to anyone who contributes to this project going
forward.
