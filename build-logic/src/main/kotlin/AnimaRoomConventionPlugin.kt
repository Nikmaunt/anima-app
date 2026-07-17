import com.google.devtools.ksp.gradle.KspExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies

/**
 * `anima.room` — Room persistence for :core:data. Exports the schema JSON to
 * `<module>/schemas` (committed to VCS) so every migration is reviewable.
 * Migration policy: exportSchema=true, schemas under version control, manual
 * Migration list keyed by a DB version constant.
 */
class AnimaRoomConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("com.google.devtools.ksp")

        extensions.configure<KspExtension> {
            arg("room.schemaLocation", "${projectDir}/schemas")
            arg("room.generateKotlin", "true")
        }

        dependencies {
            add("implementation", libs.findLibrary("room-runtime").get())
            add("implementation", libs.findLibrary("room-ktx").get())
            add("ksp", libs.findLibrary("room-compiler").get())
            add("testImplementation", libs.findLibrary("room-testing").get())
        }
    }
}
