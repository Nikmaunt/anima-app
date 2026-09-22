// :core:ui — Anima's design system: palette (light/dark), typography, spacing,
// motion tokens, shared components. Intentional, not default-Material. The
// creature is the centre of every screen; the chrome recedes.
plugins {
    alias(libs.plugins.anima.android.library)
    alias(libs.plugins.anima.android.compose)
    // Screenshot goldens (Phase 0.4): the design system in light AND dark.
    alias(libs.plugins.roborazzi)
}

android {
    namespace = "app.anima.core.ui"
}

dependencies {
    implementation(libs.androidx.core.ktx)

    testImplementation(libs.junit4)
    testImplementation(libs.truth)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.ext.junit)
    testImplementation(libs.androidx.ui.test.junit4)
    testImplementation(libs.androidx.ui.test.manifest)
    testImplementation(projects.core.testing)
    testImplementation(libs.roborazzi)
    testImplementation(libs.roborazzi.compose)
}
