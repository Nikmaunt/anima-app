// :feature:widget — the creature on the home screen (ADR-007). STRICTLY a
// static snapshot: the rig is rendered to one Bitmap per update, no frame
// loop, no inference, no network. Background entities added here are only
// the widget triggers the DoD allows: the Glance receiver + WorkManager's
// charge-edge one-shots.
plugins {
    alias(libs.plugins.anima.android.library)
    alias(libs.plugins.anima.android.compose)
    alias(libs.plugins.anima.hilt)
}

android {
    namespace = "app.anima.feature.widget"
}

dependencies {
    implementation(projects.core.model)
    implementation(projects.core.creature)
    implementation(projects.core.data)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.glance.appwidget)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.kotlinx.coroutines.android)

    testImplementation(libs.junit4)
    testImplementation(libs.truth)
    testImplementation(libs.kotlinx.coroutines.test)
}
