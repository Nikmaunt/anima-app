import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

/**
 * `anima.hilt` — Hilt DI with KSP code generation. Applied by :app and any
 * Android module that owns injected bindings (e.g. :core:data repositories).
 */
class AnimaHiltConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        with(pluginManager) {
            apply("com.google.devtools.ksp")
            apply("com.google.dagger.hilt.android")
        }

        dependencies {
            add("implementation", libs.findLibrary("hilt-android").get())
            add("ksp", libs.findLibrary("hilt-compiler").get())
        }
    }
}
