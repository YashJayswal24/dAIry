# Running on a Device (Samsung phone)

This covers getting the current build onto a physical Samsung phone. Right
now the UI is a placeholder screen — this is about verifying the app
installs, runs, and (once implemented) can load a Gemma model, not about a
finished feature.

Use a physical device, not an emulator: on-device LLM inference needs real
RAM and a real GPU delegate, neither of which emulators reproduce well.

## 1. Enable Developer Options on the phone

1. Settings → About phone → tap **Build number** 7 times (Samsung sometimes
   nests this under Settings → About phone → Software information).
2. You'll see "Developer options is now enabled."

## 2. Enable USB debugging

1. Settings → Developer options → turn on **USB debugging**.
2. Samsung-specific extra toggle: also enable **Install via USB** in the same
   Developer options screen — some Samsung/One UI builds block sideloaded
   installs without it.
3. Plug the phone into your computer with a USB cable. On the phone, accept
   the **"Allow USB debugging?"** prompt (shows the computer's RSA
   fingerprint) — check "Always allow from this computer" so you don't get
   asked every time.

## 3. Verify the computer sees the phone

With Android platform-tools installed (Android Studio bundles these):

```
adb devices
```

You should see your phone listed as `device` (not `unauthorized` — if it
says that, look at the phone screen for the debugging prompt and accept it).

## 4. Check the phone meets the RAM requirement

Per [REQUIREMENTS.md](REQUIREMENTS.md), the smallest Gemma variant needs
~4 GB RAM minimum, 6-8 GB+ recommended. Check either:

- Settings → About phone → RAM (some Samsung models show this under
  Settings → About phone → Memory), or
- `adb shell cat /proc/meminfo` and look at `MemTotal`.

## 5. Build and install

Easiest path: open the project in Android Studio, let it sync (it will
offer to generate the Gradle wrapper on first open — accept that), pick your
phone from the device dropdown, and click **Run**.

Command-line equivalent, once the Gradle wrapper exists:

```
./gradlew installDebug
```

## 6. Watch logs

```
adb logcat --pid=$(adb shell pidof -s com.yashjayswal.dairy)
```

Useful while `ai/llm` and `ai/embedding` are being implemented — model load
failures and out-of-memory kills show up here.

## 7. A Samsung-specific gotcha: battery optimization

Samsung's battery management (Settings → Apps → dAIry → Battery) can kill
background work or throttle a long-running process it thinks is misbehaving —
which an in-progress LLM generation can look like. Once you're testing actual
model inference, set the app's battery usage to **Unrestricted** so a
generation isn't killed mid-response.

## 8. Getting models onto the device

There's no in-app downloader yet (see the Roadmap in the main
[README](../README.md)). Until there is, push model files directly into the
app's own external files directory so scoped storage doesn't get in the way.
Both `ai/embedding` (`MediaPipeEmbeddingEngine`) and, once implemented,
`ai/llm` read from the same `files/models/` directory:

```
adb shell mkdir -p /sdcard/Android/data/com.yashjayswal.dairy/files/models

# Embedding model — required now, MediaPipeEmbeddingEngine looks for this
# exact file name (see MediaPipeEmbeddingEngine.MODEL_FILE_NAME):
adb push embedding_model.tflite /sdcard/Android/data/com.yashjayswal.dairy/files/models/

# Gemma chat model — needed once ai/llm is implemented:
adb push gemma-model.task /sdcard/Android/data/com.yashjayswal.dairy/files/models/
```

**Note:** this is `getExternalFilesDir()` storage (`/sdcard/Android/data/...`),
not the app's internal storage — don't use `adb shell run-as ... mkdir`
here, that creates a directory under internal storage instead, a different
path than the one `adb push` targets above, and the push then fails with
`remote secure_mkdirs() failed`. Plain `adb shell mkdir -p ...` on the
`/sdcard/...` path (no `run-as`) is correct and doesn't need root.

**Git Bash on Windows:** MSYS silently rewrites any argument starting with
`/` (like `/sdcard/...`) into a Windows path before it reaches `adb.exe`,
breaking these commands. Prefix each `adb` command with `MSYS_NO_PATHCONV=1`
when running from Git Bash, e.g.
`MSYS_NO_PATHCONV=1 adb push embedding_model.tflite /sdcard/...`. PowerShell
doesn't have this problem.

Where to get these models and which size fits your phone's RAM is covered in
[REQUIREMENTS.md](REQUIREMENTS.md). If the embedding model isn't there yet,
`MediaPipeEmbeddingEngine` throws a clear error naming the exact path it
looked at rather than failing silently.

**Re-push after every reinstall:** `/sdcard/Android/data/<package>/files/`
gets wiped whenever the app is reinstalled (e.g. every `installDebug` or
`connectedDebugAndroidTest` run) — observed repeatedly on a Samsung Galaxy
S24. Don't assume a model you pushed earlier is still there; re-run step 8
after any rebuild+reinstall before trusting a "model not found" failure to
mean anything beyond "it's not there right now."

## 9. Verifying the embedding and Gemma engines actually work on your phone

There's no chat/entry UI wired up yet, so the way to confirm
`MediaPipeEmbeddingEngine`, `MediaPipeGemmaInferenceEngine`, and
`AiCoreGemmaInferenceEngine` genuinely work is an instrumented test — it
runs on the device itself and calls real on-device inference, unlike the
unit tests in `app/src/test` which only run on the JVM against fakes.

With the phone connected and `adb devices` showing it, run either
individually or both together:

```
./gradlew connectedDebugAndroidTest
```

Or in Android Studio: open the relevant test class under
`app/src/androidTest/java/.../ai/embedding/` or `.../ai/llm/` and click the
green ▶ next to it.

**Embedding engine** (needs `embedding_model.tflite` pushed, step 8 above):
three checks run for real on your phone — the model returns a non-empty,
finite vector; two semantically similar sentences embed closer together
(higher cosine similarity) than an unrelated one; and embedding the same
text twice is deterministic.

**Gemma inference engine (MediaPipe)** (needs `gemma-model.task` pushed,
step 8 above): one check — a real prompt gets a non-blank generated
response. This is a much heavier test than the embedding one (loading a
multi-hundred-MB-to-GB model and running actual generation), so expect it
to take noticeably longer and give it real time before assuming it's stuck.

**Gemma inference engine (AICore)** — `AiCoreGemmaInferenceEngineInstrumentedTest`:
no model file needed at all, since AICore/Gemini Nano is managed entirely by
the OS. On a supported device (see REQUIREMENTS.md) this launches
`MainActivity` (AICore's Prompt API refuses background calls), checks
availability, downloads Gemini Nano if needed, and generates a real
response — verified working end-to-end on a Galaxy S24 in ~6 seconds once
the model was already downloaded. On an unsupported device this test
passes trivially (skips itself) rather than failing, since "unsupported" is
expected there.

If a model file isn't pushed yet, the MediaPipe-backed tests fail with the
same "model not found at ..." message the engine itself throws — that's
expected, not a bug.

## 10. Seeding realistic test data

`SeedPepysDiaryEntriesInstrumentedTest` (`app/src/androidTest/java/.../testdata/`)
loads 300 real diary-style paragraphs (public domain, excerpted from Samuel
Pepys' diary, Project Gutenberg #4200 — bundled as
`app/src/androidTest/assets/pepys_diary_entries.json`) into the actual
on-device database via the real `EntryRepository`, computing real embeddings.
Useful for exercising `RagRetriever`/chat against realistic volume instead of
a couple of hand-typed entries. Every seeded entry is saved as
`Emotion.NEUTRAL` / intensity 3 (Pepys' diary has no emotion labels — this is
a placeholder, not a claim).

**Important:** by default, `./gradlew connectedDebugAndroidTest` uninstalls
both the app-under-test and the test APK immediately after the run — which
wipes any data the test just wrote (this is what caused the app to
mysteriously "disappear" a few times during development; it's normal AGP
behavior, not a device or Samsung security quirk). To seed data that
actually survives for you to use afterward, add
`-Pandroid.injected.androidTest.leaveApksInstalledAfterRun=true`:

```
./gradlew connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.yashjayswal.dairy.testdata.SeedPepysDiaryEntriesInstrumentedTest \
  -Pandroid.injected.androidTest.leaveApksInstalledAfterRun=true
```

Re-running appends the same 300 entries again rather than deduplicating, so
don't run it as part of the routine full test suite — target it explicitly
as shown above.

## 11. Uninstalling

```
adb uninstall com.yashjayswal.dairy
```
