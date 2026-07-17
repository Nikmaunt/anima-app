import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.getByType

/**
 * Accessor for the shared `libs` version catalog from inside convention plugins.
 *
 * MUST be `internal`: a public `Project.libs` would leak onto the build-script
 * classpath of every module that applies a convention plugin and shadow Gradle's
 * generated type-safe `LibrariesForLibs` accessor. Keeping it internal confines
 * it to build-logic.
 */
internal val Project.libs: VersionCatalog
    get() = extensions.getByType<VersionCatalogsExtension>().named("libs")
