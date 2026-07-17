// :core:creature — the character engine. Pure deterministic motion core
// (oscillators, springs, noise, rig) + Compose renderer + the concept gallery.
// Animation runs ONLY while the composable is visible and the lifecycle is
// RESUMED; reduced-motion renders calm statics with rare blinks.
plugins {
    alias(libs.plugins.anima.android.library)
    alias(libs.plugins.anima.android.compose)
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
    testImplementation(libs.kotlinx.coroutines.test)
}
