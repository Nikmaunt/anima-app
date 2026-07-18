// Root build file: declares plugin versions once (apply false); modules apply
// them through the anima.* convention plugins from build-logic. Static
// analysis (detekt + ktlint) is wired here for every module at once.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.android.test) apply false
    alias(libs.plugins.baselineprofile) apply false
    alias(libs.plugins.detekt) apply false
    alias(libs.plugins.ktlint) apply false
    alias(libs.plugins.roborazzi) apply false
}

// Captured in root scope: the `libs` accessor does not resolve inside the
// subprojects {} lambda.
val ktlintEngineVersion = libs.versions.ktlintEngine.get()

subprojects {
    apply(plugin = "io.gitlab.arturbosch.detekt")
    apply(plugin = "org.jlleitschuh.gradle.ktlint")

    extensions.configure<io.gitlab.arturbosch.detekt.extensions.DetektExtension> {
        // Rule analysis only (no type resolution: that needs a detekt built
        // against Kotlin 2.2, which exists only in the 2.0 alphas).
        buildUponDefaultConfig = true
        parallel = true
        config.setFrom(rootProject.file("config/detekt/detekt.yml"))
    }

    extensions.configure<org.jlleitschuh.gradle.ktlint.KtlintExtension> {
        version.set(ktlintEngineVersion)
        android.set(true)
        filter {
            exclude { it.file.path.contains("${File.separator}generated${File.separator}") }
        }
    }
}
