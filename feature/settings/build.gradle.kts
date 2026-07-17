// :feature:settings — concept gallery (live engine previews), creature name,
// privacy overview, notification-sense controls, soul export/import entry
// points, reduced-motion note.
plugins {
    alias(libs.plugins.anima.android.library)
    alias(libs.plugins.anima.android.compose)
    alias(libs.plugins.anima.hilt)
}

android {
    namespace = "app.anima.feature.settings"
}

dependencies {
    implementation(projects.core.model)
    implementation(projects.core.modelDelivery)
    implementation(projects.core.cloudMind)
    implementation(projects.core.creature)
    implementation(projects.core.body)
    implementation(projects.core.data)
    implementation(projects.core.mind)
    implementation(projects.core.ui)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.hilt.navigation.compose)

    testImplementation(libs.junit4)
    testImplementation(libs.truth)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
}
