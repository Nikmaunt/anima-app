// :core:model-delivery — THE single sanctioned network exception (ADR-005).
// Downloads or imports the Gemma model file for the mind's GEMMA tier.
// Deliberately dependency-free on the network side: platform
// HttpsURLConnection only, so the "no network stack in the version catalog"
// invariant survives. Its manifest is the ONLY source of the INTERNET
// permission in the merged app manifest — asserted by NetworkIsolationTest.
plugins {
    alias(libs.plugins.anima.android.library)
    alias(libs.plugins.anima.hilt)
}

android {
    namespace = "app.anima.core.modeldelivery"
}

dependencies {
    api(projects.core.model)
    implementation(libs.androidx.core.ktx)
    implementation(libs.kotlinx.coroutines.android)
    // Play Asset Delivery client (ADR-010): IPC to the Play Store app for
    // the :mind-pack fast-follow pack — not an HTTP stack.
    implementation(libs.play.asset.delivery.ktx)
    implementation(libs.androidx.datastore.preferences)

    testImplementation(libs.junit4)
    testImplementation(libs.truth)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core.ktx)
}
