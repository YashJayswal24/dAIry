# Add project-specific ProGuard rules here as dependencies require them.
# See docs/REQUIREMENTS.md before adding a rule for a new dependency —
# understand why it's needed, don't just paste one in.

# com.google.mlkit:genai-prompt (AICore) bundles AutoValue's annotation
# processor code into its runtime jar, which references javax.lang.model.*
# (compiler-only APIs, not present on Android, not part of the SDK).
# These classes are only reachable from AutoValue's *build-time* code
# generation path -- never actually invoked at runtime -- so -dontwarn
# (not -keep) is correct: R8-generated via missing_rules.txt.
-dontwarn javax.lang.model.SourceVersion
-dontwarn javax.lang.model.element.Element
-dontwarn javax.lang.model.element.ElementKind
-dontwarn javax.lang.model.element.Modifier
-dontwarn javax.lang.model.type.TypeMirror
-dontwarn javax.lang.model.type.TypeVisitor
-dontwarn javax.lang.model.util.SimpleTypeVisitor8

# com.google.mlkit:genai-prompt/genai-common (AICore) use Protobuf-Lite
# generated message classes internally, which look up their fields
# reflectively by name at runtime (mergeFrom/writeTo). Without this
# standard, widely-documented protobuf-lite rule, R8's default field
# renaming breaks that lookup -- reproduced on-device: sending a chat
# message on a release/minified build failed with "Field platform_ for
# S2.D not found" (a renamed GeneratedMessageLite field). This keeps
# field *names* only; classes/methods are still shrunk normally.
-keepclassmembers class * extends com.google.protobuf.GeneratedMessageLite {
    <fields>;
}

# com.google.mediapipe:tasks-genai / tasks-text: the native (JNI) layer
# does its own stack-based lookup of which Java class called it, and
# fails when R8 renames that class -- reproduced on-device: sending a
# chat message crashed the app with "IllegalStateException: no caller
# found on the stack for: A2.c" at
# com.google.mediapipe.framework.Graph.<clinit>. Keeping the whole
# package unobfuscated (not just -dontwarn -- this needs real name
# preservation, not just suppressed warnings) avoids any class-name
# mismatch between the native side and R8's renaming.
-keep class com.google.mediapipe.** { *; }
-dontwarn com.google.mediapipe.**
