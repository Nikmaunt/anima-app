// :core:testing — test-only support shared by the golden (Roborazzi) tests of
// every module. Consumed as testImplementation only; nothing here ships.
plugins {
    alias(libs.plugins.anima.android.library)
}

android {
    namespace = "app.anima.core.testing"
}

dependencies {
    api(libs.roborazzi)
}
