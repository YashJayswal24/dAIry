# TODO

Detailed, working version of the checklist in the README
[Roadmap](../README.md#roadmap). Update this file as work progresses —
it's the source of truth for "what's left and why," the README roadmap
is just the short summary.

## Incident: a real entry was briefly lost during the title-field migration (2026-09-13)

While verifying the `MIGRATION_1_2` (title column) on-device, the user's
real entry id 302 ("fucked her" / JOY / 5, written a day earlier) vanished
after an `installDebug` + app launch — 301 other rows (1 real, 300 seeded)
all survived untouched, only that one was gone. Ruled out a stale-WAL
read artifact (force-stopped the app and re-pulled cleanly; still absent).
The `ALTER TABLE ... ADD COLUMN` migration itself is a safe, additive
operation and doesn't explain a single-row loss.

Best-supported theory, not fully confirmed: that entry was written in an
earlier session that may not have closed the app gracefully, so it could
have sat un-checkpointed in the Room WAL file for a while rather than
being flushed into the main `.db` file (everything else was already
checkpointed, from being read/written by later sessions). The
migration's schema-altering transaction likely forced a WAL checkpoint
that mishandled that one stale segment.

**Recovered in full**: the exact text/emotion/intensity/`createdAt` were
still in a local db pull taken minutes earlier for verification, so the
entry was re-saved through the real `EntryRepository` (correct recomputed
embedding, not a placeholder) via a one-time instrumented test, deleted
immediately after. New row id is 303, not 302 (autoincrement doesn't
reuse ids) — cosmetic only, all other data identical.

**Takeaway for future device work on this project**: back up
(`adb exec-out run-as ... cat databases/dairy.db[-wal][-shm]` to a local
file) before *any* reinstall once real personal entries exist on the
device, not just before schema migrations — this one wasn't even
triggered by the migration path with certainty. Cheap insurance either
way.

## Done, this pass

- **Edit and delete entries.** `EntryDao`/`EntryRepository` gained real
  `update`/`delete` (re-embeds on update, `createdAt` untouched, `DELETE`
  removes the row including its embedding). UI: tapping an entry opens a
  full-screen `EntryDetailScreen` (not inline icons) with Close/Edit/
  Delete, a confirm dialog before deleting, and Previous/Next to browse
  neighbouring entries without returning to the list — shared by both
  `EntryScreen`'s list and `CalendarScreen`'s day view. Verified on-device:
  edit pre-fills correctly, Cancel discards local changes (confirmed
  against the real db), delete confirmation cancels safely.
- **Title field.** Real schema change — `EntryEntity`/`DiaryEntry` gained
  `title`, via a genuine Room `Migration(1, 2)` (not destructive fallback,
  since real personal entries exist on-device). `DiaryEntry.displayTitle()`
  falls back to a derived title from the entry text for pre-migration
  entries. Embeddings now include the title when present.
- **Backdating entries.** `EntryRepository.save()` takes an optional
  `createdAt`, defaulting to now — a new full-screen `EntryComposeScreen`
  (separate from the list, opened via a FAB) has a date row that opens a
  Material3 `DatePickerDialog` for writing about a different day.
- **Mood picker redesigned** to match the reference "My Diary" screenshots
  (analyzed 2026-09-13, not stored in this repo): a circular
  `EmotionAvatarButton` opens `EmotionPickerSheet`, a bottom sheet grid of
  our existing 7 emotions — replaces the old inline `FilterChip` row, used
  consistently in both the compose and detail/edit screens.
- **Borderless text fields.** Built `PlainTextField` (a bare
  `BasicTextField` + manual placeholder overlay) after feedback that
  Material3 `TextField`'s reserved chrome/padding read as "screen wastage"
  against the reference's minimal look. Applied to both
  `EntryComposeScreen` and `EntryDetailScreen`'s edit mode.
- **Real unit tests for all of the above**: `EntryRepository` update/delete
  (re-embed, `createdAt` preserved, full row removal, title in embeddable
  text), `EntryDetailScreen`'s `previousEntry`/`nextEntry` navigation
  (6 cases: older/newer/boundary/not-found/single-entry), `CalendarScreen`'s
  day-of-week ordering and date conversion. 46 unit tests total, all
  passing.
- **Judged chat eval suite** (`docs/EVAL_PROMPTS.md`) — a debug-only
  instrumented test harness (`RunEvalPromptsInstrumentedTest`) calls the
  real `ChatAnswerer` pipeline directly, no UI automation, writing results
  to a pulled JSON file. All 15 prompts run and scored; found two real
  bugs (dates/emotions never reaching the chat prompt at all, and
  AICore's `BUSY` quota needing retry-with-backoff — see the dedicated
  backlog items below).
- **Calendar view** — month grid (`com.kizitonwose.calendar:compose`,
  pinned to 2.6.1 to avoid an unrelated compileSdk 35 bump), mood emoji
  per day, tap a day to see its entries via the same detail screen.
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

- Fix the AICore `BUSY` quota handling and the date/mood chat-context gap
  — both fully diagnosed and documented in the Backlog below, from
  running the eval suite. These are the two highest-value chat fixes.
- Check the redesigned UI in **light** mode too (only verified in dark
  mode on-device so far) — the custom color scheme defines both, but
  only one has been visually confirmed.
- Photo/media attachments, mood statistics charts (`co.yml:ycharts`), and
  search/tags are still just planned (see "Future feature ideas" below) —
  calendar view and entry edit/delete/title/backdating are now done.

## Future feature ideas, from "My Diary" reference screenshots + general diary-app UI patterns

Not started. Originally researched via the Play Store listing for
`diary.journal.lock.mood.daily` (Simple Design Ltd.); on 2026-09-13 the
user shared 3 real screenshots of that app in use (Home/stats, Calendar,
Entries list), which gave a much more concrete picture than the store
listing alone. **The screenshots themselves are not saved anywhere in this
repo** — one of them showed real, readable personal diary text from the
user's other app, and this repo is public, so only the UI/feature
patterns are described here, never the content. Roughly ordered by how
well each fits dAIry's actual value prop (private, on-device, AI-grounded
journal) vs. being a generic diary-app feature that may not be worth the
complexity.

### What the screenshots actually showed

**Navigation**: 5-item bottom bar — Entries list, Calendar, a centered
circular **+** (elevated above the bar, for new entry), Photos/Gallery,
Profile. The calendar icon shows the current day-of-month as a small
badge on the icon itself.

**Home/stats screen ("Mine")**: account card (avatar, email, backup
status, "Back up now"), a horizontally-scrollable app-theme carousel,
a "try diary templates" promo card, an achievements/badge section with a
progress bar, then three distinct mood visualizations stacked: a **bar
chart** ("Mood Statistics," per day, last-week filter), a **line/area
chart** ("Mood Stability," 0-100% over days of the week), and a **donut/
gauge chart** ("Mood Percentage," big % + entry count in the center, with
a Good/Normal/Bad legend).

**Calendar screen**: month-picker dropdown, full month grid (S-S header),
each date cell shows a small mascot/avatar icon that's colored and
detailed for days *with* an entry and muted/gray for days *without* one
— a more characterful stand-in for what's usually just a dot indicator.
Selected day highlighted; below the grid, a speech-bubble-style card
shows that day's entry preview or "No entries on this day."

**Entries list screen**: promo card at top (same template prompt as
Home), then a plain vertical list of entry cards — each shows the day
number large, month/year + weekday as a subheading, a small mood-avatar
icon top-right, a bold entry title, and a truncated body preview.

**Cross-cutting**: a single illustrated night-sky/mountain parallax
background used behind every screen (not a plain color), a cute animal
mascot (rabbit) used as the recurring mood-avatar motif instead of plain
emoji, purple/violet gradient theme with frosted-glass card surfaces.

### Premade resources to build these with (verified real libraries, not guesses)

- **Calendar grid**: [`com.kizitonwose.calendar:compose`](https://github.com/kizitonwose/Calendar)
  (Maven Central) — the most widely-used Compose calendar library, month
  view backed by `LazyRow`/`LazyColumn`, fully custom day-cell content
  (exactly what's needed to put a mood emoji/mascot per day instead of a
  plain number). Verify the current version against Maven Central before
  adding — don't trust a remembered version number.
- **Charts**: [`co.yml:ycharts`](https://github.com/yml-org/YCharts) (Maven
  Central) supports bar, line, and donut/pie charts in one library —
  covers all three Home-screen chart types. Alternatives if YCharts turns
  out under-maintained when actually checked:
  [`io.github.ehsannarmani:compose-charts`](https://github.com/ehsannarmani/ComposeCharts)
  (animated, simpler API) or
  [`patrykandpatrick/vico`](https://github.com/patrykandpatrick/vico)
  (more powerful/extensible, Compose Multiplatform). Check real recent
  commit activity before committing to one — chart libraries in this
  space have a track record of going stale.
- **Mascot/illustration assets**: the one piece that isn't just "add a
  library" — a custom illustrated character (like the rabbit) is a real
  art-asset cost (licensed pack or commissioned), not a code dependency.
  Cheaper honest alternative: keep the existing emoji-based
  `Emotion.emoji()` approach (already shipped) rather than commit to a
  custom mascot, unless the maintainer specifically wants to source/buy
  character art.

### Prioritized replication plan

1. **Calendar view** — DONE, 2026-09-13. `ui/calendar/CalendarScreen.kt`,
   third bottom-nav tab. Uses `com.kizitonwose.calendar:compose:2.6.1`
   (pinned old — see build.gradle.kts comment: 2.7.0+ needs compileSdk 35,
   we're on 34), groups `entryRepository.observeAll()` client-side by
   `LocalDate` (no new DAO query, no schema change), shows the day's first
   entry's `Emotion.emoji()` on in-month days that have one, tapping a day
   shows its entries below the grid via the same card style as
   `EntryScreen`. Verified on-device: month grid renders, day-with-entry
   shows the right emoji, tapping selects and shows the real entry.
2. **Entries list redesign** — DONE, 2026-09-13, as part of the title
   field / edit-delete work: `EntryScreen`'s cards now show
   `DiaryEntry.displayTitle()` (bold) + a truncated body preview, and the
   detail page (`EntryDetailScreen`) is the actual date-prominent view
   (day number, weekday, mood avatar) the reference showed.
3. **Mood statistics** (`co.yml:ycharts` or alternative) — high value,
   uses `Emotion`/`emotionIntensity` already stored, needs new aggregation
   queries (entries-per-day, mood-percentage-breakdown) but no schema
   change.
4. **Achievements/gamification, theme marketplace, Pro subscription/
   backup** — explicitly deprioritized: gamification and cosmetic themes
   don't serve dAIry's actual value prop (private, on-device, AI-grounded
   journal) and would be scope creep; cloud backup specifically conflicts
   with dAIry's "nothing leaves the phone" pitch (see the existing note
   below) and needs an explicit maintainer decision, not a default build.
5. **Illustrated mascot/parallax background** — nice-to-have, real
   asset-sourcing cost either way (buy/license art or invest real design
   time), defer until the functional pieces above are done.

Also still relevant from the original research pass:

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
- **Cloud backup/sync (Google Drive)** — flagging a real tension rather
  than just copying it: dAIry's whole pitch is "no entry, embedding, or
  question ever needs to leave the phone" (see README). A cloud backup
  feature directly cuts against that unless it's opt-in and clearly
  separated from the default experience (e.g. user-initiated, encrypted
  export the user uploads themselves — not silent background sync).
  Don't add this without an explicit decision from the maintainer; see
  [docs/HUMAN_IN_THE_LOOP.md](HUMAN_IN_THE_LOOP.md).

## Backlog

- [ ] Handle AICore's `BUSY` quota (error code 9) in
      `AiCoreGemmaInferenceEngine` — running the eval suite
      (`RunEvalPromptsInstrumentedTest`) 15 calls back-to-back (~5-7s
      apart) hit `GenAiException [ErrorCode 9]` after 10 calls, twice.
      Initially misread as a fixed ~10-call session cap (wrong — see
      below); confirmed against a real user session sending "hi" 13
      times successfully, and against Google's own error code docs and
      a matching report on `googlesamples/mlkit` (issue #1070): error 9
      is `BUSY`, a **rolling rate/quota window**, distinct from error 30
      `BACKGROUND_USE_BLOCKED` (a different code entirely — foreground
      was never the issue here). Other developers hit it after ~36-41
      rapid requests; pacing ~1s apart avoided it; the quota replenishes
      after ~2.5 minutes; the exception carries a `getRetryDelay()`.
      Verified a 15s gap avoids it entirely (all 15 eval prompts passed)
      but that's almost certainly more spacing than necessary and isn't
      the right fix shape anyway. Real fix, per Google's own
      `BUSY` docstring: catch `GenAiException` where
      `errorCode == GenAiException.ErrorCode.BUSY`, read
      `getRetryDelay()`, retry with exponential backoff (or fall back to
      `MediaPipeGemmaInferenceEngine` on repeated failure) instead of
      surfacing a dead-end error to the user. A normal human typing
      cadence in the real UI is unlikely to ever trip this in practice —
      it mainly bit the eval harness's rapid automated calls.
- [ ] Fix the "no date or mood awareness in chat" gap found while
      designing [docs/EVAL_PROMPTS.md](EVAL_PROMPTS.md) — `RagRetriever`
      returns only entry text (`createdAt`/`emotion`/`emotionIntensity`
      never reach the prompt), so relative-time questions ("what did I
      do today") and mood-aggregation questions ("when was I happiest,
      consecutively") are unanswerable by construction, not just hard.
      Recommended fix (see EVAL_PROMPTS.md "Fixing the gap"): cheap
      code-side keyword/regex detection routes matching questions to a
      real structured `EntryDao` query (date-range filter, or an
      emotion-streak calculation in Kotlin) computed *before* the LLM
      call, rather than betting on the on-device model reliably emitting
      a structured tool-call itself — neither `tasks-genai` nor
      `genai-prompt` expose native function-calling at the pinned
      versions. Re-run the eval suite's category C/D prompts after this
      lands; they should flip from expected-fail to pass.
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
