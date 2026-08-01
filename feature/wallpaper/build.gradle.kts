// :feature:wallpaper — the creature as a live wallpaper (ADR-012). v0.4
// ships the STRICTEST form of the budget: static mood pose, 0 fps steady
// state, redraws only on surface/visibility/state-change events (state ones
// debounced to one per 30 s), dead stop when invisible. No timers, no
// Choreographer, no wake locks, no WorkManager — enforced by WallpaperBudget
// and the absence of any frame-loop code in this module.
plugins {
    alias(libs.plugins.anima.android.library)
    alias(libs.plugins.anima.android.compose)
    alias(libs.plugins.anima.hilt)
}

android {
    namespace = "app.anima.feature.wallpaper"
}

dependencies {
    implementation(projects.core.model)
    implementation(projects.core.creature)
    implementation(projects.core.data)
    implementation(libs.androidx.core.ktx)
    implementation(libs.kotlinx.coroutines.android)

    testImplementation(libs.junit4)
    testImplementation(libs.truth)

    // v1.1c task 6: the wallpaper frame had never been looked at in eleven
    // runs. WallpaperFrameDumpTest renders every body to a real Skia canvas on
    // a device image and writes the PNGs out, so they can be opened.
    androidTestImplementation(libs.junit4)
    androidTestImplementation(libs.truth)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.core.ktx)
}
