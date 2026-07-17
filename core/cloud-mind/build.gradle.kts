// :core:cloud-mind — the second and LAST sanctioned network exception
// (ADR-011). The user-keyed (BYOK) cloud mind: OpenAI-compatible endpoint
// over platform HttpsURLConnection — deliberately no HTTP client library,
// so the "no network stack in the version catalog" invariant survives.
// Its manifest and :core:model-delivery's are the ONLY two sources of the
// INTERNET permission — asserted by NetworkIsolationTest v3, which also
// pins this module's dependency list and bans logging here (the key and
// the user's words must never reach a log).
plugins {
    alias(libs.plugins.anima.android.library)
    alias(libs.plugins.anima.hilt)
}

android {
    namespace = "app.anima.core.cloudmind"
}

dependencies {
    api(projects.core.model)
    implementation(libs.androidx.core.ktx)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.androidx.datastore.preferences)

    testImplementation(libs.junit4)
    testImplementation(libs.truth)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core.ktx)
}
