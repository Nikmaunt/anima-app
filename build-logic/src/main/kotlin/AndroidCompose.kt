import com.android.build.api.dsl.CommonExtension
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

/**
 * Enables Compose and wires the BOM-managed UI + tooling dependencies. Applied
 * to whichever Android extension (application or library) the module uses.
 */
internal fun Project.configureAndroidCompose(
    commonExtension: CommonExtension<*, *, *, *, *, *>,
) {
    commonExtension.apply {
        buildFeatures {
            compose = true
        }
    }

    dependencies {
        val bom = libs.findLibrary("androidx-compose-bom").get()
        add("implementation", platform(bom))
        add("androidTestImplementation", platform(bom))
        add("implementation", libs.findLibrary("androidx-ui").get())
        add("implementation", libs.findLibrary("androidx-ui-graphics").get())
        add("implementation", libs.findLibrary("androidx-ui-tooling-preview").get())
        add("implementation", libs.findLibrary("androidx-material3").get())
        add("debugImplementation", libs.findLibrary("androidx-ui-tooling").get())
        add("debugImplementation", libs.findLibrary("androidx-ui-test-manifest").get())
        add("androidTestImplementation", libs.findLibrary("androidx-ui-test-junit4").get())
    }
}
