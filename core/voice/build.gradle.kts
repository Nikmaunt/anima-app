// :core:voice — the creature's voice over the SYSTEM TTS engine (ADR-013).
// Deliberately zero network dependencies: speech stays on-device by
// construction (offline voices only, checked per Voice), so this module sits
// firmly outside the two sanctioned INTERNET exceptions and must keep passing
// NetworkIsolationTest. android.speech.tts is the whole surface.
plugins {
    alias(libs.plugins.anima.android.library)
    alias(libs.plugins.anima.hilt)
}

android {
    namespace = "app.anima.core.voice"
}

dependencies {
    api(projects.core.model)
    implementation(libs.androidx.core.ktx)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.datastore.preferences)

    testImplementation(libs.junit4)
    testImplementation(libs.truth)
    testImplementation(libs.kotlinx.coroutines.test)
}
