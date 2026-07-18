// :feature:home — the creature's screen: full-scene creature + chat. The
// creature reacts to typing and "thinks with its body" during generation.
plugins {
    alias(libs.plugins.anima.android.library)
    alias(libs.plugins.anima.android.compose)
    alias(libs.plugins.anima.hilt)
}

android {
    namespace = "app.anima.feature.home"
}

dependencies {
    implementation(projects.core.model)
    implementation(projects.core.creature)
    implementation(projects.core.body)
    implementation(projects.core.data)
    implementation(projects.core.mind)
    implementation(projects.core.ui)
    implementation(projects.core.voice)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.hilt.navigation.compose)

    testImplementation(libs.junit4)
    testImplementation(libs.truth)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
}
