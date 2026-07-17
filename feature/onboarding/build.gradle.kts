// :feature:onboarding — hatching (the one long animation), concept gallery
// with live engine previews, naming, honest privacy screen, optional soul
// import and notification opt-in pointers.
plugins {
    alias(libs.plugins.anima.android.library)
    alias(libs.plugins.anima.android.compose)
    alias(libs.plugins.anima.hilt)
}

android {
    namespace = "app.anima.feature.onboarding"
}

dependencies {
    implementation(projects.core.model)
    implementation(projects.core.creature)
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
