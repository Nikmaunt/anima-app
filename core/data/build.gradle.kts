// :core:data — the soul's vault. SQLCipher-encrypted Room database; the key is
// a random passphrase wrapped by an Android Keystore AES key. Repositories for
// soul facts (append-only), conversations, body journal, notification events,
// relationship counters. DataStore for non-secret preferences.
plugins {
    alias(libs.plugins.anima.android.library)
    alias(libs.plugins.anima.room)
    alias(libs.plugins.anima.hilt)
}

android {
    namespace = "app.anima.core.data"

    // MigrationTestHelper reads the committed schema JSONs on-device.
    sourceSets["androidTest"].assets.srcDir("$projectDir/schemas")
}

dependencies {
    api(projects.core.model)
    implementation(libs.androidx.core.ktx)
    implementation(libs.kotlinx.coroutines.android)
    // Backup payload assembly (JsonObject builders only — no @Serializable,
    // so the compiler plugin isn't needed here).
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.sqlcipher.android)
    implementation(libs.androidx.sqlite.ktx)
    implementation(libs.androidx.datastore.preferences)

    testImplementation(libs.junit4)
    testImplementation(libs.truth)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core.ktx)

    // GMD suite (v0.3 Phase 0): the paths Robolectric cannot reach — real
    // SQLCipher opens, the AndroidKeyStore wrap, schema validation on-device.
    androidTestImplementation(libs.junit4)
    androidTestImplementation(libs.truth)
    androidTestImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.core.ktx)
    androidTestImplementation(libs.room.testing)
}
