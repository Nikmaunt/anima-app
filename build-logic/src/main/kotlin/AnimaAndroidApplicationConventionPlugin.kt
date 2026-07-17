import com.android.build.api.dsl.ApplicationExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

/**
 * `anima.android.application` — the single :app module. Fixes the SDK/JVM
 * config and the targetSdk; feature-level concerns (Compose, Hilt) are opted
 * into per module via the other conventions.
 */
class AnimaAndroidApplicationConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        with(pluginManager) {
            apply("com.android.application")
            apply("org.jetbrains.kotlin.android")
        }

        extensions.configure<ApplicationExtension> {
            configureKotlinAndroid(this)
            defaultConfig.targetSdk = 36
        }
    }
}
