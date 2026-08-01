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

/**
 * v1.1c task 1.1 — the CI contour, switched by one property.
 *
 * Three runs of this repo reported a green `check` that executed zero tests.
 * `-Proborazzi.test.verify=true` does not change a test task's inputs, so Gradle
 * keeps the task UP-TO-DATE, restores its result XML FROM-CACHE, and prints
 * BUILD SUCCESSFUL in six seconds. Every one of the three was caught by a human
 * reading task output, never by the build.
 *
 * `-Panima.ci=true` makes that impossible rather than unlikely: every `Test`
 * task loses the right to be up-to-date and the right to be served from the
 * cache. `settings.gradle.kts` adds the Roborazzi verify flag to the same
 * switch. `tools/ci-verify.py` then reads the log and the result files and
 * refuses a run where either guarantee failed to hold — belt as well as braces,
 * because a contour that cannot fail proves nothing about the day it is
 * misconfigured.
 */
val animaCiContour = providers.gradleProperty("anima.ci").orNull == "true"

if (animaCiContour) {
    // Nobody may run the CI contour with comparison switched off. The default
    // lives in gradle.properties; this refuses an explicit override, which is
    // the only remaining way to get a green run that measured no pixels.
    val verify = providers.gradleProperty("roborazzi.test.verify").orNull
    val record = providers.gradleProperty("roborazzi.test.record").orNull
    if (verify != "true" || record == "true") {
        throw GradleException(
            "-Panima.ci=true requires roborazzi.test.verify=true and record off. " +
                "Got verify=$verify record=$record. Goldens would be captured and never compared.",
        )
    }
    // The ground truth tools/ci-verify.py reads. Printed from the task graph, so
    // a task that is about to come back UP-TO-DATE announces itself anyway —
    // which is exactly the case the verifier has to be able to see.
    gradle.taskGraph.whenReady {
        allTasks.filterIsInstance<Test>().forEach { logger.lifecycle("ANIMA-CI-TEST-TASK ${it.path}") }
    }
}

subprojects {
    apply(plugin = "io.gitlab.arturbosch.detekt")
    apply(plugin = "org.jlleitschuh.gradle.ktlint")

    if (animaCiContour) {
        tasks.withType<Test>().configureEach {
            outputs.upToDateWhen { false }
            outputs.doNotCacheIf("the CI contour must execute tests, not recall them") { true }
        }
    }

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
