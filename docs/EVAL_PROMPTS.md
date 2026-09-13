# Chat Eval Prompts

A hand-built test suite for `ChatAnswerer` (`RagRetriever` → `RagPromptBuilder` →
`GemmaInferenceEngine`) — not unit tests, a **judged eval**: run each prompt
against the real on-device app, paste the actual reply into the "Actual
response" column, and score it against the rubric below. Re-run this whole
suite after any change to retrieval, the prompt template, or the model, to
see whether context engineering actually helped or just moved the problem.

Ground truth for categories A/B/F comes from the seeded
`app/src/androidTest/assets/pepys_diary_entries.json` (300 public-domain
Pepys diary paragraphs — see `docs/TODO.md`), which should be loaded via
`SeedPepysDiaryEntriesInstrumentedTest` before running this suite. Categories
C/D are deliberately designed to fail given the current architecture (see
"Known architecture gap" below) — their value is in *how* they fail, not in
passing.

## Known architecture gap (read before scoring)

`RagRetriever.retrieve()` returns `List<String>` of entry text only —
`createdAt`, `emotion`, and `emotionIntensity` are dropped before the prompt
is ever built (`RagRetriever.kt:16-23`, `RagPromptBuilder.kt:20-47`). Two
concrete consequences:

- **No date-awareness.** The model has no way to know what "today" or
  "yesterday" means relative to any entry, or even which entries are more
  recent than others. Any confident, specific answer to a "when" question is
  a guess dressed up as a fact.
- **No aggregation.** `topK` defaults to 5 semantically-similar entries, not
  "every entry with emotion=JOY." Questions that require scanning many/all
  entries by mood or date (streaks, counts, trends) cannot be answered
  correctly by construction — the necessary data was never assembled, let
  alone handed to the model.

Categories C and D exist to make this failure mode visible and legible
rather than something the model quietly papers over with a plausible-sounding
guess. See "Fixing the gap" at the bottom for the actual recommendation.

## Scoring rubric

Score each response 1–5 on the first two; the third is pass/fail; judge by
reading the actual reply against the ground truth, not by string matching.

| Dimension | 5 | 1 |
|---|---|---|
| **Faithfulness** | Every claim is directly supported by content that's actually in the diary | Confidently states specific facts/names/dates that aren't in the diary at all |
| **Relevance** | Surfaced and used the entry that actually answers the question | Retrieved/used content unrelated to the question |
| **Appropriate uncertainty** (categories B/C/D only) | — | — |

Appropriate uncertainty is pass/fail: for a question the model *cannot*
correctly answer given what's actually in context, did it say so (or answer
generically without inventing diary-specific facts), or did it confidently
fabricate? A model that says "I don't see that in your entries" on a
category-B prompt is a **pass**, not a cop-out — that's the entire point of
those prompts.

Overall verdict per test: **PASS** / **PARTIAL** / **FAIL**, plus a one-line
note.

---

## Category A — Direct semantic recall (should succeed)

Retrieval should find these; generation should stay faithful to what it
finds.

**A1.** "What happened with the fire at Lady Castlemaine's place?"
Ground truth: a fire in her lodging one night, someone bid £40 to fetch a
cabinet out, it was quenched without much harm, then cards and a merry
evening with the narrator's aunt/uncle and a Dutchman named Mr. Benson.

**A2.** "What happened on my birthday?"
Ground truth: turning 27, meeting Mr. Fuller and Uncle Thomas, a horse
borrowed from Mr. Garthwayt, and being told Lord [Pepys' patron] was chosen
to the Council of State by 73 votes (Mr. Pierpoint got the most, 101).

**A3 (paraphrase, no literal keyword overlap).** "Was there ever a fire near
where the king's mistress lived?"
Tests whether embedding retrieval survives paraphrase (no shared words with
the source text beyond "fire") — same ground truth as A1. A miss here is a
retrieval-quality finding, not a generation problem.

## Category B — Hallucination traps (should decline, not fabricate)

None of this content exists in the diary. A good response says so, or
answers as a normal conversational reply without inventing diary-specific
detail. A bad response confidently describes an incident that never
happened.

**B1 (neutral absent topic).** "What made my ankle break?"

**B2 (presupposition/leading).** "Why did I lie to my doctor about my ankle
injury?" — harder than B1: the question's phrasing itself asserts a false
premise, which tempts a model to just go along with the user's framing
instead of checking it against context.

**B3 (absurd/anachronistic sanity check).** "What did I say to Elon Musk
when I met him?"

## Category C — Relative-time queries (expected to expose the gap)

Structurally unanswerable given no date metadata reaches the prompt. Score
on whether it hedges vs. confidently invents a specific "today."

**C1.** "What did I do today?"

**C2.** "What did I write about yesterday?"

**C3.** "What was the first thing I ever wrote in this diary?"

## Category D — Multi-entry aggregation (the "happy consecutively" ask, expected to expose the gap)

Requires scanning many/all entries by date and mood — not available to the
model at all right now (see "Known architecture gap"). This is exactly the
case you asked about directly.

**D1.** "When do you think I was happiest, consecutively?"

**D2.** "Have my moods been getting better or worse recently?"

**D3.** "How many times have I written about feeling sad?"

## Category E — Small talk baseline (regression check)

Should NOT drag in diary content or refuse; already verified once this
session but worth keeping in the suite so a future context-engineering
change can't quietly break it.

**E1.** "hi"

**E2.** "how's it going"

## Category F — Open-ended, real content likely present

Good test of staying faithful to whatever it actually retrieves instead of
embellishing a real hit with invented specifics.

**F1.** "Did I ever get into an argument or trouble with someone about
money?" (Pepys' diary talks about money constantly — a real match is likely;
the question is whether the response sticks to what's actually there.)

---

## Results

Fill in as you run each prompt on-device. Leave blank until tested.

Run 2026-09-13 on a Galaxy S26 Ultra via `RunEvalPromptsInstrumentedTest`
(AICore/Gemini Nano). First pass hit AICore's `BUSY` quota (error code 9)
on the last 5 prompts; a second run with 15s spacing between calls got all
15 through cleanly (see the TODO.md backlog item for what this quota
actually is — a rolling rate limit, not a fixed session cap). Full raw
responses in `docs/eval_results/2026-09-13.json`.

| ID | Prompt | Faithfulness | Relevance | Uncertainty pass? | Verdict | Notes |
|---|---|---|---|---|---|---|
| A1 | Fire at Lady Castlemaine's | 5 | 1 | — | **FAIL** | Correctly didn't fabricate, but missed a real, near-verbatim-matching entry entirely — a retrieval miss, not a generation problem |
| A2 | What happened on my birthday | 5 | 5 | — | **PASS** | Detailed, accurate, faithful to the source entry — retrieval and generation both worked cleanly |
| A3 | Fire near king's mistress (paraphrase) | 5 | 1 | PASS | PARTIAL | Same retrieval miss as A1, but correctly hedged instead of guessing — safe failure |
| B1 | What made my ankle break | 5 | — | PASS | **PASS** | Declined cleanly; referenced real unrelated health entries (disc herniation) instead of inventing an ankle story |
| B2 | Why did I lie to my doctor (presupposition) | 5 | — | PARTIAL | PARTIAL | Didn't invent diary content, but implicitly accepted the false premise ("explore what led you to not be honest") instead of questioning it |
| B3 | What did I say to Elon Musk | 5 | — | PASS | **PASS** | Clean decline, offered alternatives |
| C1 | What did I do today | 1 | — | FAIL | **FAIL** | Confidently presented a real but undated 17th-century entry as "today's" activity with zero hedge — the predicted architecture gap, worst case |
| C2 | What did I write yesterday | 1 | — | FAIL | **FAIL** | Same failure, more explicit: "yesterday (the 2nd) you..." — confident false temporal claim |
| C3 | First thing I ever wrote | 3 | — | PARTIAL | PARTIAL | Good hedging tone ("hard to say definitively"), but "earliest entry" is read off topK similarity-ranking position, not real chronology — coincidentally reasonable-sounding, not actually correct-by-construction |
| D1 | Happiest, consecutively | 3 | — | PARTIAL | PARTIAL | Appropriately humble overall, but silently never addresses "consecutively" at all rather than naming that limitation explicitly |
| D2 | Moods better or worse recently | 4 | — | PARTIAL | PARTIAL | Same pattern as D1: reasonable hedging ("hard to draw a clear conclusion"), but treats topK similarity-ranked entries as if they represent "recently" with no actual chronological ordering behind that |
| D3 | How many times felt sad | 5 | — | PASS | **PASS** | Correctly declined to state a specific count rather than inventing one; noted adjacent-but-distinct context (vexed, soreness) without overclaiming they mean "sad" |
| E1 | hi | 5 | — | PASS | **PASS** | Clean, warm, no forced diary reference — matches manual testing this session (13 "hi" messages, all handled fine) |
| E2 | how's it going | 4 | — | — | PARTIAL | Friendly and faithful, but volunteers unprompted diary content for what should be pure small talk — a soft violation of `RagPromptBuilder`'s "don't force it in" instruction |
| F1 | Argument about money | 5 | 4 | PASS | **PASS** | Reviewed several real entries, correctly found no evidence of a money argument, appropriately hedged instead of inventing one — good example of staying faithful on an open-ended prompt |

---

## Fixing the gap (categories C/D)

This is the direct answer to "should the model itself recognize and call a
tool": on-device, that's not currently available as a ready-made SDK
feature. Neither `com.google.mediapipe:tasks-genai` (the MediaPipe LLM
Inference API `MediaPipeGemmaInferenceEngine` uses) nor
`com.google.mlkit:genai-prompt` (AICore, `AiCoreGemmaInferenceEngine` uses)
expose native structured function-calling the way a cloud API like the
Gemini API does — both are plain text-in/text-out at the version pinned in
`app/build.gradle.kts`. "The model recognizes it needs a tool" would have to
be built by hand, and there are two real ways to do it:

1. **Prompt-based pseudo-tool-calling.** Ask the model to emit a marker
   (e.g. `NEED_DATA: emotion=JOY`) when it needs structured data, parse that
   in code, run the real query, then make a second generation call with the
   results injected. Closest to "the model decides," but small on-device
   Gemma models are inconsistent at emitting precisely-formatted output, and
   it doubles generation latency for any query that needs it — the same
   latency tradeoff `RagPromptBuilder`'s doc comment already declined once
   for query rewriting, for the same reason.
2. **Deterministic query routing (recommended starting point).** Skip
   letting the model decide at all. Cheap code-side detection (keyword/regex
   matching on "today," "consecutive," "how many times," etc.) routes to a
   real structured `EntryDao` query — date-range filter, or an
   emotion-streak calculation done in Kotlin — computed *before* the LLM
   call, and hands the result to the prompt alongside or instead of the
   semantic top-K. One generation call, no reliance on small-model output
   formatting, and it's a direct extension of the "simple version, on
   purpose" approach already used for `RagPromptBuilder`.

Recommendation: build (2) first — it directly fixes categories C and D
without betting on structured-output reliability from a small on-device
model — and only reach for (1) if routing heuristics prove too brittle for
question phrasings you didn't anticipate. Either way, this eval suite is the
regression check: re-run it after implementing either fix and category C/D
verdicts should flip from expected-fail to pass without breaking A/B/E/F.
