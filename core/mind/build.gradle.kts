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

    testImplementation(libs.junit4)
    testImplementation(libs.truth)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
}
