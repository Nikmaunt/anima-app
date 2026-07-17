# R8 rules for the release build (v0.2). Debug remains unminified.
#
# Room, Hilt, Compose, kotlinx-serialization all ship consumer rules in their
# AARs — nothing manual needed for them. What follows is only what the JNI /
# reflection-heavy libraries genuinely require.

# --- SQLCipher (net.zetetic:sqlcipher-android) -------------------------------
# JNI: native methods are registered against exact class/member names, and the
# native side calls back into Java by name. Zetetic's guidance for the R8 era
# is to keep the whole namespace with descriptor classes.
-keep,includedescriptorclasses class net.zetetic.database.** { *; }
-keepclasseswithmembers class * { native <methods>; }

# --- ML Kit GenAI Prompt API (com.google.mlkit:genai-*) ----------------------
# Talks to the AICore system service over AIDL/reflection; keep the API and
# common surface. (The AARs carry consumer rules for their internals; this is
# a safety net over the public surface actually referenced at runtime.)
-keep class com.google.mlkit.genai.** { *; }
-dontwarn com.google.mlkit.genai.**

# --- MediaPipe tasks-genai (Gemma tier, :core:mind) --------------------------
# JNI-heavy; graph/task classes are looked up from native code by name.
-keep class com.google.mediapipe.** { *; }
-dontwarn com.google.mediapipe.**

# Protobuf lite (pulled by MediaPipe): field access via reflection.
-keep class com.google.protobuf.** { *; }
-dontwarn com.google.protobuf.**
