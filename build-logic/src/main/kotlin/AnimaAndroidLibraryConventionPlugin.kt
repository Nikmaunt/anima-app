import com.android.build.api.dsl.LibraryExtension
import com.android.build.api.variant.LibraryAndroidComponentsExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

/**
 * `anima.android.library` — Android library modules that need a real Android
 * runtime (:core:data for Room/SQLCipher, :core:creature for Compose, etc.).
 */
class AnimaAndroidLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        with(pluginManager) {
            apply("com.android.library")
            apply("org.jetbrains.kotlin.android")
        }

        extensions.configure<LibraryExtension> {
            configureKotlinAndroid(this)
            buildTypes {
                getByName("debug") {
                    // ADR-014 (v0.5): pseudolocales must be generated for
                    // library modules too, or Robolectric unit tests cannot
                    // resolve en-XA / ar-XB qualifiers against module strings.
                    // Debug only — release output is untouched.
                    isPseudoLocalesEnabled = true
                }
            }
        }
        extensions.configure<LibraryAndroidComponentsExtension> {
            // v0.7: modules without androidTest sources must not build a
            // test APK at all — an empty one lacks the instrumentation
            // runner dependency and DIES with ClassNotFoundException on the
            // device (surfaced by the GMD suite after the AGP 8.13.2 bump).
            beforeVariants {
                it.androidTest.enable = it.androidTest.enable &&
                    projectDir.resolve("src/androidTest").exists()
            }
        }
    }
}
