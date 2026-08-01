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

// v0.9 Phase L. The committed export above is NOT what the user sees. The
// plugin also generates a per-variant file into
// build/generated/aboutLibraries/<variant>/res/raw/, and at resource merge
// THAT one wins — the licenses screen renders it, and it is what ships.
// The two sets differ (measured 2026-07-26: committed 227, debug 202,
// release 192), so diffing only the committed export — as CI did through
// v0.8 — leaves the shipping attribution unchecked.
//
// Note the export tasks cannot substitute for this: `exportLibraryDefinitions`
// AND `exportLibraryDefinitionsRelease` both emit 227 (verified by running
// them), i.e. the plugin's variant filter does not apply to the export path.
// The generated resource is the only artifact carrying the shipping set.
//
// The invariant worth enforcing is legal, not cosmetic: every library that
// actually ships must be covered by the attribution we publish. A superset
// is fine (over-attribution harms nobody); a missing entry is a compliance
// hole. So this fails on shipped-minus-committed, and only reports the
// reverse.
val verifyReleaseLicenseAttribution by tasks.registering {
    description = "Fails if a library in the RELEASE artifact is missing from the committed attribution."
    group = "verification"
    // Ordering is not optional: run before the generator and the "shipped"
    // file is stale or absent, which is how this task first went red on a
    // tree that was actually clean.
    val committed = layout.projectDirectory.file("src/main/res/raw/aboutlibraries.json")
    val shipped =
        layout.buildDirectory.file("generated/aboutLibraries/release/res/raw/aboutlibraries.json")
    // Deliberately NOT wired with dependsOn/inputs.file on `shipped`. That
    // path is an output of the aboutlibraries plugin which
    // `packageReleaseResources` already consumes WITHOUT declaring a
    // dependency; pulling the generator into the same task graph as
    // `bundleRelease` makes Gradle fail the whole build on that pre-existing
    // plugin wiring rather than on anything this check does.
    // So this task reads what a release build has already produced, and says
    // so plainly if that has not happened. CI runs it after the bundle step.
    outputs.upToDateWhen { false }
    doLast {
        fun ids(f: File): Set<String> =
            Regex("\"uniqueId\"\\s*:\\s*\"([^\"]+)\"")
                .findAll(f.readText())
                .map { it.groupValues[1] }
                .toSet()

        val shippedFile = shipped.get().asFile
        if (!shippedFile.isFile) {
            throw GradleException(
                "No release attribution to check at $shippedFile — build the release " +
                    "variant first (gradlew :app:bundleRelease), then run this task.",
            )
        }
        val committedIds = ids(committed.asFile)
        val shippedIds = ids(shippedFile)
        val uncovered = (shippedIds - committedIds).sorted()
        logger.lifecycle(
            "license attribution: committed=${committedIds.size} shipped(release)=${shippedIds.size} " +
                "committed-only=${(committedIds - shippedIds).size}",
        )
        if (uncovered.isNotEmpty()) {
            throw GradleException(
                "Release ships ${uncovered.size} librar(ies) with no entry in the committed " +
                    "attribution — regenerate it (gradlew :app:exportLibraryDefinitions):\n" +
                    uncovered.joinToString("\n") { "  - $it" },
            )
        }
    }
}

android {
    namespace = "app.anima"

    defaultConfig {
        applicationId = "app.anima"
        // Scheme: CHANGELOG.md header. versionCode stays the run number — the
        // planned switch to MAJOR*10000+MINOR*100+PATCH is deferred at 1.0.0,
        // with the reasoning, not silently skipped.
        versionCode = 12
        versionName = "1.1.1"
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

    // v1.1: :mind-pack is NOT in the release bundle any more.
    //
    // ADR-010 shipped the model as a fast-follow asset pack, sized at
    // ~1.38 GB (ADR-021 measurement). With chat demoted to an off-by-default
    // experiment, that is over a gigabyte delivered to every install for a
    // screen almost nobody will turn on. The module stays in the repo and in
    // settings.gradle.kts — it is not deleted — it simply no longer rides
    // along, and the model chain falls through to the download/SAF paths
    // that already exist and are already what debug builds use.
    //
    // MindPackAbsentTest fails the build if this line comes back without a
    // deliberate decision.

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
