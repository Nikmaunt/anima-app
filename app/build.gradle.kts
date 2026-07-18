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
        versionCode = 3
        versionName = "0.3.0"
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

    // ADR-010: the Gemma model rides a fast-follow asset pack in the AAB.
    // Plain APK builds (debug deploys) carry no packs — the model chain
    // falls through to the downloaded/SAF paths.
    assetPacks += ":mind-pack"

    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }

    lint {
        // v0.3: release lint is back ON. v0.2 disabled it after 15–40 min
        // "deadlocks" scanning the MediaPipe tasks-genai AAR; with the
        // daemon at -Xmx6g (gradle.properties) the full lintVitalRelease
        // chain runs green in under a minute and the hang does not
        // reproduce (docs/research-v3.md §C.1). If a genuine hang ever
        // returns, the fallback is a targeted baseline + ADR, never
        // checkReleaseBuilds=false.
        abortOnError = true
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
    implementation(projects.core.cloudMind)
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
    implementation(projects.feature.rest)
    implementation(projects.feature.wallpaper)

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
