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

## 9. Verifying the embedding model actually works on your phone

There's no chat/entry UI wired up yet, so the way to confirm
`MediaPipeEmbeddingEngine` genuinely works is an instrumented test — it runs
on the device itself and calls real MediaPipe inference, unlike the unit
tests in `app/src/test` which only run on the JVM against fakes.

1. Do step 8 above for the embedding model (push `embedding_model.tflite`).
2. With the phone connected and `adb devices` showing it:
   ```
   ./gradlew connectedDebugAndroidTest
   ```
   Or in Android Studio: open
   `app/src/androidTest/java/.../MediaPipeEmbeddingEngineInstrumentedTest.kt`
   and click the green ▶ next to the class.
3. Three checks run for real on your phone: the model returns a non-empty,
   finite vector; two semantically similar sentences embed closer together
   (higher cosine similarity) than an unrelated one; and embedding the same
   text twice is deterministic.

If the model file isn't pushed yet, this fails with the same "Embedding
model not found at ..." message `MediaPipeEmbeddingEngine` throws — that's
expected, not a bug.

## 9. Uninstalling

```
adb uninstall com.yashjayswal.dairy
```
