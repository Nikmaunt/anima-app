// :core:creature — the character engine. Pure deterministic motion core
// (oscillators, springs, noise, rig) + Compose renderer + the concept gallery.
// Animation runs ONLY while the composable is visible and the lifecycle is
// RESUMED; reduced-motion renders calm statics with rare blinks.
plugins {
    alias(libs.plugins.anima.android.library)
    alias(libs.plugins.anima.android.compose)
    // Screenshot goldens (Phase 0.4): record/verifyRoborazziDebug; goldens
    // in src/test/screenshots, visual diff is part of the DoD.
    alias(libs.plugins.roborazzi)
}

android {
    namespace = "app.anima.core.creature"
}

dependencies {
    api(projects.core.model)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.kotlinx.coroutines.android)

    testImplementation(libs.junit4)
    testImplementation(libs.truth)
    // Compose UI tests run on the JVM under Robolectric (v0.2 debt closure):
    // no device needed, so `gradlew test` exercises creature states in CI.
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.ext.junit)
    testImplementation(libs.androidx.ui.test.junit4)
    testImplementation(libs.androidx.ui.test.manifest)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(projects.core.testing)
    testImplementation(libs.roborazzi)
    testImplementation(libs.roborazzi.compose)
}
