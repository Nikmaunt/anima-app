// :core:model — pure JVM domain model + pure logic (mood derivation, seeds,
// motion math constants, soul export formatting). Android-free by construction.
plugins {
    alias(libs.plugins.anima.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
}

dependencies {
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.serialization.json)

    testImplementation(libs.junit4)
    testImplementation(libs.truth)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
}
