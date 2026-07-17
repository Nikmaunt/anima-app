import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.CommonExtension
import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * `anima.android.compose` — opt-in Compose for an Android module. Requires the
 * module to already be an application or library (applied first). Applies the
 * Kotlin Compose compiler plugin and the BOM-managed Compose dependencies.
 */
class AnimaAndroidComposeConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("org.jetbrains.kotlin.plugin.compose")

        val commonExtension: CommonExtension<*, *, *, *, *, *> =
            extensions.findByType(ApplicationExtension::class.java)
                ?: extensions.getByType(LibraryExtension::class.java)

        configureAndroidCompose(commonExtension)
    }
}
