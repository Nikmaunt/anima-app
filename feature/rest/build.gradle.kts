// :feature:rest — rest-together sessions (v0.4 core feature) + the QS tile
// (ADR-015: the tile is an ACTION — start/continue a rest — never a mood
// display). No background entities: the session clock ticks only while the
// screen is RESUMED; the tile service runs only for tile events.
plugins {
    alias(libs.plugins.anima.android.library)
    alias(libs.plugins.anima.android.compose)
    alias(libs.plugins.anima.hilt)
    alias(libs.plugins.roborazzi)
}

android {
    namespace = "app.anima.feature.rest"
}

dependencies {
    implementation(projects.core.model)
    implementation(projects.core.creature)
    implementation(projects.core.body)
    implementation(projects.core.data)
    implementation(projects.core.ui)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.kotlinx.coroutines.android)

    testImplementation(libs.junit4)
    testImplementation(libs.truth)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.ext.junit)
    testImplementation(libs.androidx.ui.test.junit4)
    testImplementation(libs.androidx.ui.test.manifest)
    testImplementation(libs.roborazzi)
    testImplementation(libs.roborazzi.compose)
}
