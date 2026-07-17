// :baselineprofile — generator module (ADR-006). Generation is device-bound:
// run `gradlew :app:generateBaselineProfile` with the S24 (or any API 33+
// device) connected. No profile is committed until generated on hardware.
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    // Versions come from the root plugins block (apply false there); naming
    // them again here trips "already on the classpath" resolution.
    id("com.android.test")
    id("org.jetbrains.kotlin.android")
    id("androidx.baselineprofile")
}

android {
    namespace = "app.anima.baselineprofile"
    compileSdk = 36

    defaultConfig {
        minSdk = 31
        targetSdk = 36
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    targetProjectPath = ":app"
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

baselineProfile {
    // The owner's S24 (API 34+, non-rooted) is a valid generation device.
    useConnectedDevices = true
}

dependencies {
    implementation(libs.androidx.test.ext.junit)
    implementation(libs.androidx.espresso.core)
    implementation(libs.androidx.uiautomator)
    implementation(libs.androidx.benchmark.macro.junit4)
}
