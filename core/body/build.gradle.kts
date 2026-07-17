// :core:body — the creature's senses. Reads permissionless device signals
// (battery, storage, connectivity kind, thermal status, clock) and exposes
// them as a cold-start-cheap StateFlow<BodySignals>. No background polling:
// signals are collected only while the UI is subscribed.
plugins {
    alias(libs.plugins.anima.android.library)
    alias(libs.plugins.anima.hilt)
}

android {
    namespace = "app.anima.core.body"
}

dependencies {
    api(projects.core.model)
    implementation(libs.androidx.core.ktx)
    implementation(libs.kotlinx.coroutines.android)

    testImplementation(libs.junit4)
    testImplementation(libs.truth)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core.ktx)
}
