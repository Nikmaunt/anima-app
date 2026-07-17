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
}

dependencies {
    api(projects.core.model)
    implementation(libs.androidx.core.ktx)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.sqlcipher.android)
    implementation(libs.androidx.sqlite.ktx)
    implementation(libs.androidx.datastore.preferences)

    testImplementation(libs.junit4)
    testImplementation(libs.truth)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core.ktx)
}
