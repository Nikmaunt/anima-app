// :core:ui — Anima's design system: palette (light/dark), typography, spacing,
// motion tokens, shared components. Intentional, not default-Material. The
// creature is the centre of every screen; the chrome recedes.
plugins {
    alias(libs.plugins.anima.android.library)
    alias(libs.plugins.anima.android.compose)
}

android {
    namespace = "app.anima.core.ui"
}

dependencies {
    implementation(libs.androidx.core.ktx)

    testImplementation(libs.junit4)
    testImplementation(libs.truth)
}
