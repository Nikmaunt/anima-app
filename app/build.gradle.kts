// :app — composition root: DI wiring, navigation, MainActivity. No logic.
import java.util.Properties

plugins {
    alias(libs.plugins.anima.android.application)
    alias(libs.plugins.anima.android.compose)
    alias(libs.plugins.anima.hilt)
    alias(libs.plugins.baselineprofile)
    // v0.6: build-time license aggregation only (no runtime artifact).
    alias(libs.plugins.aboutlibraries)
}

// OSS licenses (docs/adr/…, settings screen): regenerate with
//   gradlew :app:exportLibraryDefinitions
// after any dependency change; DoD diffs the committed file against a
// fresh export. offlineMode keeps the build free of network calls.
aboutLibraries {
    offlineMode = true
    export {
        outputFile = file("src/main/res/raw/aboutlibraries.json")
        prettyPrint = true
    }
    collect {
        fetchRemoteLicense = false
        fetchRemoteFunding = false
    }
}

android {
    namespace = "app.anima"

    defaultConfig {
        applicationId = "app.anima"
        // Scheme: CHANGELOG.md header — pre-1.0 versionCode = run number.
        versionCode = 8
        versionName = "0.8.0"
        // v0.5 day-in-life E2E: Hilt swaps the mind for a deterministic fake.
        testInstrumentationRunner = "app.anima.HiltTestRunner"
    }

    // v0.6 release engineering: the upload key lives OUTSIDE the repo
    // (../anima-keys/, sibling of the checkout; see docs/release/signing.md).
    // Present → release signs locally; absent (CI, fresh clones) → release
    // builds unsigned and Play App Signing remains the only owner of the
    // app signing key. Path override: -PanimaKeystoreProps=<file>.
    val keystoreProps =
        (findProperty("animaKeystoreProps") as String?)
            ?.let(::File)
            ?: rootProject.file("../anima-keys/keystore.properties")
    val uploadSigning =
        if (keystoreProps.exists()) {
            val props =
                Properties().apply {
                    keystoreProps.inputStream().use { load(it) }
                }
            signingConfigs.create("upload") {
                storeFile = File(props.getProperty("storeFile"))
                storePassword = props.getProperty("storePassword")
                keyAlias = props.getProperty("keyAlias")
                keyPassword = props.getProperty("keyPassword")
            }
        } else {
            null
        }

    buildTypes {
        debug {
            // ADR-014: pseudolocale gate for the v0.5 string migration —
            // en-XA (expansion) and ar-XB (RTL) selectable in system
            // settings on debug builds.
            isPseudoLocalesEnabled = true
        }
        release {
            signingConfig = uploadSigning
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

    // GMD day-in-life suite (v0.5 Phase 0): full app on the ATD emulator with
    // a deterministic fake mind installed through Hilt. Compose BOM +
    // ui-test-junit4 already ride in via the compose convention plugin.
    androidTestImplementation(projects.core.model)
    androidTestImplementation(projects.core.data)
    androidTestImplementation(projects.core.mind)
    androidTestImplementation(projects.feature.widget)
    androidTestImplementation(libs.androidx.glance.appwidget)
    androidTestImplementation(libs.junit4)
    androidTestImplementation(libs.truth)
    androidTestImplementation(libs.kotlinx.coroutines.android)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.core.ktx)
    // v0.7: Espresso.closeSoftKeyboard in the E2E (IME vs paused clock).
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.hilt.android.testing)
    kspAndroidTest(libs.hilt.compiler)
}
