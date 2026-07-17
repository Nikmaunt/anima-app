// :feature:notifications — OPT-IN notification sense. Owns the
// NotificationListenerService (the app's ONLY background entity: it filters
// and writes events to the local encrypted DB — zero inference, zero network,
// zero UI), the enable screen with an honest explanation, the per-app
// allowlist editor, and storm detection feeding the creature's anxiety.
plugins {
    alias(libs.plugins.anima.android.library)
    alias(libs.plugins.anima.android.compose)
    alias(libs.plugins.anima.hilt)
}

android {
    namespace = "app.anima.feature.notifications"
}

dependencies {
    implementation(projects.core.model)
    implementation(projects.core.data)
    implementation(projects.core.ui)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.hilt.navigation.compose)

    testImplementation(libs.junit4)
    testImplementation(libs.truth)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
}
