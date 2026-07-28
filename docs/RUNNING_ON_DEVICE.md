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

## 8. Getting a model onto the device (once `ai/llm` is implemented)

There's no in-app downloader yet (see the Roadmap in the main
[README](../README.md)). Until there is, push a `.task` model file directly
into the app's own external files directory so scoped storage doesn't get in
the way:

```
adb shell run-as com.yashjayswal.dairy mkdir -p files/models
adb push gemma-model.task /sdcard/Android/data/com.yashjayswal.dairy/files/models/
```

Where to get a `.task` model and which size fits your phone's RAM is covered
in [REQUIREMENTS.md](REQUIREMENTS.md).

## 9. Uninstalling

```
adb uninstall com.yashjayswal.dairy
```
