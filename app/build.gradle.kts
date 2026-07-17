// :app — composition root: DI wiring, navigation, MainActivity. No logic.
plugins {
    alias(libs.plugins.anima.android.application)
    alias(libs.plugins.anima.android.compose)
    alias(libs.plugins.anima.hilt)
}

android {
    namespace = "app.anima"

    defaultConfig {
        applicationId = "app.anima"
        versionCode = 1
        versionName = "0.1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            // R8 config is a post-v0.1 task; debug is the verification target.
            isMinifyEnabled = false
        }
    }

    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
}

dependencies {
    implementation(projects.core.model)
    implementation(projects.core.body)
    implementation(projects.core.creature)
    implementation(projects.core.data)
    implementation(projects.core.mind)
    implementation(projects.core.ui)
    implementation(projects.feature.onboarding)
    implementation(projects.feature.home)
    implementation(projects.feature.notifications)
    implementation(projects.feature.soul)
    implementation(projects.feature.settings)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.androidx.datastore.preferences)

    testImplementation(libs.junit4)
    testImplementation(libs.truth)
    testImplementation(libs.kotlinx.coroutines.test)
}
