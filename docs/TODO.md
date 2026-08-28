# TODO

Detailed, working version of the checklist in the README
[Roadmap](../README.md#roadmap). Update this file as work progresses —
it's the source of truth for "what's left and why," the README roadmap
is just the short summary.

## Done, this pass

- Verified chat generates for real on a Galaxy S26 Ultra: the "no model
  found" error path fired correctly (shown as a chat bubble, not a
  crash) before the embedding model was pushed, confirming the wiring
  itself works end-to-end.
- Rewrote `RagPromptBuilder`: it used to force a refusal whenever
  `RagRetriever` returned no context (even for a plain "hi"), because
  the retriever always returns its top-K regardless of relevance. It
  now reads as a normal conversational agent that *also* has retrieval
  — entries are used only when actually relevant, otherwise it just
  replies normally. See the updated doc comment on `RagPromptBuilder`.
- Made chat bubble text selectable (`SelectionContainer`) so replies
  can be long-pressed and copied.
- Extracted the retrieve → build prompt → generate pipeline out of
  `ChatScreen`'s Composable into `ChatAnswerer.answer(...)`
  (`ui/chat/ChatAnswerer.kt`) specifically so it's unit-testable with
  fakes — previously it only existed inline in a Compose `onClick`
  lambda with zero test coverage. See `ChatAnswererTest.kt`.
- Added `SeedPepysDiaryEntriesInstrumentedTest` + bundled
  `pepys_diary_entries.json` (300 public-domain diary paragraphs, see
  RUNNING_ON_DEVICE.md step 10) so `RagRetriever`/chat can be tested
  against realistic volume, not just a couple of hand-typed entries.
  Verified 300 rows landed in the real on-device Room db with real
  embeddings (spot-checked via `sqlite3`, not just trusting the test's
  own green checkmark).
- **UI redesign** — replaced the untouched Material3 default color scheme (generic purple)
  with a custom warm terracotta/plum palette, light and dark
  (`ui/theme/Theme.kt`) — this was the main reason the app read as
  generic/unfriendly, since no seed color had ever been set.
- Replaced the plain top `TabRow` with a `Scaffold` (`CenterAlignedTopAppBar`
  titled "dAIry" + a `NavigationBar` with icons: pencil for Write, chat
  bubble for Ask) — see `MainActivity.kt`.
- `EntryScreen`: the compose-entry section is now a `Card`; each emotion
  chip shows an emoji (😊 😢 😠 😨 😲 😌 😐); past entries render as
  individual cards instead of a plain divided list.
- `ChatScreen`: removed the redundant in-screen title (the top bar covers
  it now), friendlier empty-state copy, chat bubbles capped at 280dp max
  width so short replies don't stretch edge-to-edge.
- Added `androidx.compose.material:material-icons-extended` dependency
  for the nav icons.
- Verified visually on the S26 Ultra (both tabs screenshotted) and via
  `testDebugUnitTest` (all 28 unit tests still pass — the redesign didn't
  touch any tested logic, only Composables).

## Next up

- Do an actual hands-on chat session on-device (ask a question that
  should retrieve a specific seeded Pepys entry, confirm the answer is
  grounded in it; ask a plain "hi", confirm it replies normally instead
  of refusing) — the wiring and error path are verified, but a real
  generated reply from the LLM hasn't been read yet.
- Check the redesigned UI in **light** mode too (only verified in dark
  mode on-device so far) — the custom color scheme defines both, but
  only one has been visually confirmed.

## Future feature ideas, from researching "My Diary" (`diary.journal.lock.mood.daily`, Simple Design Ltd.) and general diary-app UI patterns

Not started — recorded per the user's request to "check out its other
features and add it to plan." Roughly ordered by how well each fits
dAIry's actual value prop (private, on-device, AI-grounded journal) vs.
being a generic diary-app feature that may not be worth the complexity:

- **Calendar view** — a month grid to jump to entries by date, and see at
  a glance which days have an entry. Common across every diary app
  researched (Daybook, Your Diary, Diarium, My Diary). Probably the
  single highest-value addition after search — would need a new screen
  plus a `EntryDao` query for "entries on date X" / "dates with entries
  in month Y".
- **Search / filter past entries** — by keyword and/or emotion. Cheap to
  add given `RagRetriever` already does embedding-based similarity
  search; a literal keyword search over `EntryDao` would be simpler and
  complementary (semantic search sometimes isn't what you want when you
  remember the exact word you wrote).
- **Tags/categories** (e.g. "travel", "work", "gratitude") — lets entries
  be filtered/grouped beyond just emotion. Would need a schema change
  (new column or join table) — bundle with the "finalize Room
  schema/migrations" backlog item so it's one migration, not several.
- **Photo/media attachments** — every reference app has this. Bigger
  lift here: needs local media storage, a way to reference an image from
  an entry, and it'd change what "the diary" means for the embedding/RAG
  pipeline (do we embed captions? ignore images for retrieval?). Worth a
  real design discussion before starting, not a quick add.
- **Reminders/notifications** to nudge daily journaling. Standard
  Android `WorkManager` + notification, no real design tension with
  dAIry's offline-first goal.
- **Richer entry customization** (fonts, background colors/images,
  stickers) — nice-to-have, lowest priority; mostly cosmetic per-entry
  state that doesn't interact with the RAG/embedding side at all.
- **Mood trends/statistics view** — a chart of emotion over time. Pairs
  naturally with the calendar view and the `Emotion`/`emotionIntensity`
  fields we already store; no new data needed, just a new visualization
  screen.
- **Cloud backup/sync (Google Drive)** — flagging a real tension rather
  than just copying it: dAIry's whole pitch is "no entry, embedding, or
  question ever needs to leave the phone" (see README). A cloud backup
  feature directly cuts against that unless it's opt-in and clearly
  separated from the default experience (e.g. user-initiated, encrypted
  export the user uploads themselves — not silent background sync).
  Don't add this without an explicit decision from the maintainer; see
  [docs/HUMAN_IN_THE_LOOP.md](HUMAN_IN_THE_LOOP.md).

## Backlog

- [ ] Persist chat history — messages still live only in Compose state
      and vanish on navigating away or process death. Needs a Room
      table + DAO, same shape as `EntryDao`.
- [ ] Finalize Room schema/migrations — schema is still version 1 with no
      migrations defined. Decide the migration strategy (destructive vs.
      real `Migration`s) before the next entity change ships.
- [ ] In-app model download/selection flow — currently the only way to
      get a MediaPipe `.task` model onto the device is manual
      `adb push`; no in-app picker/downloader exists yet.
- [ ] Verify `MediaPipeGemmaInferenceEngine` end-to-end with a real
      `.task` file — only `AiCoreGemmaInferenceEngine` has generated
      successfully on-device so far. The file pulled from AI Edge
      Gallery was `.litertlm` (wrong format, see REQUIREMENTS.md). Need
      a real `.task` file, e.g. the gated
      `litert-community/Gemma3-1B-IT` → `gemma3-1b-it-int4.task` on
      Hugging Face (requires the user to accept the gate themselves).
- [ ] Confirm a successful entry save round-trip on a real device —
      `EntryScreen` → `EntryRepository` → Room is wired and unit-tested,
      but tapping Save and seeing the row actually land in the on-device
      Room db hasn't been confirmed yet (verification kept getting
      interrupted by the model file getting wiped on reinstall). Use the
      adb + local `sqlite3` inspection method in
      [RUNNING_ON_DEVICE.md](RUNNING_ON_DEVICE.md), not blind UI taps.

## Future / not yet justified

- [ ] LLM-driven query condensation for multi-turn follow-up questions —
      deliberately not doing this now. See the doc comment on
      `RagPromptBuilder` for the reasoning: dense embeddings already
      handle paraphrasing, and an extra LLM call would double on-device
      latency for a benefit (resolving an ambiguous follow-up against
      conversation history) that doesn't apply until chat supports
      multi-turn context at all. Revisit once chat history is persisted
      and multi-turn is real, or if real usage shows single-shot
      retrieval missing relevant entries.

## Notes for picking this back up

- Toolchain: Kotlin 2.2.21, KSP `2.2.21-2.0.5`, Room 2.8.4, Compose
  compiler via `org.jetbrains.kotlin.plugin.compose` (not the old
  `composeOptions { kotlinCompilerExtensionVersion }` block).
- Two `GemmaInferenceEngine` implementations behind
  `GemmaInferenceEngineProvider`: `AiCoreGemmaInferenceEngine` (Gemini
  Nano via AICore, tried first, **verified working** on-device) falls
  back to `MediaPipeGemmaInferenceEngine` (bundled `.task` model,
  **unverified** — no correctly-formatted model file on hand yet).
- On-device verification workflow (see
  [RUNNING_ON_DEVICE.md](RUNNING_ON_DEVICE.md)): re-push any model files
  after every reinstall (external app storage gets wiped each time),
  use `adb exec-out screencap -p` for screenshots, pull + inspect the
  Room sqlite db directly via `adb exec-out run-as <pkg> cat
  databases/dairy.db` + local `sqlite3` rather than blind
  `adb shell input tap` UI automation (caused a stray tap on the home
  screen once — avoid guessing screen coordinates).
