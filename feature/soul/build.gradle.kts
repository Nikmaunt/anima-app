// :feature:soul — the soul is portable. Export: one readable markdown file
// via the system share sheet. Import: extractor prompt (copy button) for
// another AI + paste → parse → per-fact user confirmation. Untrusted text
// never becomes a command; facts land only after explicit confirmation.
plugins {
    alias(libs.plugins.anima.android.library)
    alias(libs.plugins.anima.android.compose)
    alias(libs.plugins.anima.hilt)
}

android {
    namespace = "app.anima.feature.soul"
}

dependencies {
    implementation(projects.core.model)
    implementation(projects.core.data)
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
