// :app — composition root: DI wiring, navigation, MainActivity. No logic.
plugins {
    alias(libs.plugins.anima.android.application)
    alias(libs.plugins.anima.android.compose)
    alias(libs.plugins.anima.hilt)
    alias(libs.plugins.baselineprofile)
}

android {
    namespace = "app.anima"

    defaultConfig {
        applicationId = "app.anima"
        versionCode = 2
        versionName = "0.2.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            // v0.2: full R8 + resource shrinking. JNI/reflection keeps live
            // in proguard-rules.pro; everything else relies on consumer rules.
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }

    lint {
        // lintVitalAnalyzeRelease deadlocks on this machine while scanning
        // the MediaPipe tasks-genai AAR (reproduced twice, 15–40 min hangs).
        // Release lint is disabled for the *build*; run `gradlew lint`
        // explicitly when needed. Recorded in the v0.2 report.
        checkReleaseBuilds = false
    }
}

// NetworkIsolationTest asserts against the merged debug manifest and fails
// when it is absent (the v0.1 vacuous-pass gap). Guarantee the input exists
// whenever unit tests run, even for a bare `gradlew :app:test`.
tasks.withType<Test>().configureEach {
    dependsOn("processDebugManifestForPackage")
}

dependencies {
    baselineProfile(projects.baselineprofile)
    implementation(libs.androidx.profileinstaller)
    implementation(projects.core.modelDelivery)
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
    implementation(projects.feature.widget)

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
