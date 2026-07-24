// :core:mind — the creature's voice. MindEngine interface + on-device Gemini
// Nano implementation (ML Kit GenAI) + deterministic fake. The Nano dependency
// is confined to this module; everything upstream talks to the interface.
// Honest degradation: when Nano is unavailable the creature lives, "the mind
// sleeps".
plugins {
    alias(libs.plugins.anima.android.library)
    alias(libs.plugins.anima.hilt)
}

android {
    namespace = "app.anima.core.mind"
}

dependencies {
    api(projects.core.model)
    implementation(libs.androidx.core.ktx)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)
    // On-device only: ML Kit GenAI Prompt API (AICore / Gemini Nano). This is
    // NOT a network library — inference runs in the AICore system service.
    implementation(libs.mlkit.genai.prompt)
    // On-device only: MediaPipe LLM inference over a local Gemma file
    // (ADR-005 GEMMA tier). The model file arrives via :core:model-delivery;
    // this module sees it through the MindModelLocator interface only.
    implementation(libs.mediapipe.tasks.genai)
    // ADR-020: LiteRT-LM as the OPTIONAL second local runtime — debug builds
    // only (developer flag, default OFF); release APKs never carry it. Like
    // tasks-genai it runs a local model file and is NOT a network library;
    // NetworkIsolationTest pins this module's dependency allowlist and the
    // merged-manifest permission budget.
    debugImplementation(libs.litertlm.android)

    testImplementation(libs.junit4)
    testImplementation(libs.truth)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)

    // Bake-off harness (v0.5 Phase 1C): instrumented prompt-set runs against
    // whatever model files were pushed to the device — the artifact that
    // makes S24 model comparisons a command instead of a ritual.
    androidTestImplementation(libs.junit4)
    androidTestImplementation(libs.truth)
    androidTestImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.core.ktx)
}
