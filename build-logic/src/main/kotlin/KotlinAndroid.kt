import com.android.build.api.dsl.CommonExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinAndroidProjectExtension

/**
 * Android + Kotlin config shared by the application and library conventions.
 * SDK levels are fixed here (single source of truth): compileSdk 36, minSdk 31.
 * minSdk 31 gives us dataExtractionRules (backup opt-out) and modern haptics;
 * the AGSL shader layer of the creature engine is runtime-gated to API 33+.
 * We compile to Java/Kotlin 17 bytecode on the installed JDK 21 without a
 * toolchain, so no extra JDK download is required on the build machine.
 */
internal fun Project.configureKotlinAndroid(
    commonExtension: CommonExtension<*, *, *, *, *, *>,
) {
    commonExtension.apply {
        compileSdk = 36

        defaultConfig {
            minSdk = 31
        }

        compileOptions {
            sourceCompatibility = JavaVersion.VERSION_17
            targetCompatibility = JavaVersion.VERSION_17
        }

        testOptions {
            // Robolectric-based DAO/engine tests run on the JVM under `check`.
            unitTests.isIncludeAndroidResources = true
            unitTests.isReturnDefaultValues = true
        }
    }

    extensions.configure<KotlinAndroidProjectExtension> {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }
}
