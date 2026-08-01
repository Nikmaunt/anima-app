package app.anima

import com.google.common.truth.Truth.assertWithMessage
import org.junit.Test
import java.io.File

/**
 * v1.1c task 1 — the guard on the guard.
 *
 * Three times this repository produced a green result that measured nothing:
 * `PersonaAcceptanceTest` sitting in SKIPPED, goldens captured but never
 * compared, and finally every test task in the tree returning UP-TO-DATE and
 * then FROM-CACHE while `check` printed BUILD SUCCESSFUL in six seconds. Every
 * one was found by a person reading task output. None was found by the build.
 *
 * The fix is three files — `gradle.properties`, the root `build.gradle.kts`,
 * `.github/workflows/ci.yml` — plus `tools/ci-verify.py`. Any of them can be
 * edited back to the comfortable version in a line, and the result would be a
 * badge that goes green faster. This test is what makes that a build failure
 * rather than a quiet improvement in build times.
 *
 * Source-reading, on purpose, like `NetworkIsolationTest`, `ChatDemotionTest`
 * and `NoDefaultBodyTest`: these are claims about what the build configuration
 * says, and the configuration text is the only place that answer lives.
 */
class CiContourTest {
    private val repoRoot: File by lazy {
        generateSequence(File(System.getProperty("user.dir"))) { it.parentFile }
            .first { File(it, "settings.gradle.kts").exists() }
    }

    private fun read(path: String): String {
        val file = File(repoRoot, path)
        assertWithMessage("$path must exist").that(file.exists()).isTrue()
        return file.readText()
    }

    @Test
    fun `golden comparison is on by default for everyone`() {
        val props = read("gradle.properties")
        assertWithMessage(
            "roborazzi.test.verify=true must be the repository default. Without it " +
                "captureRoboImage takes the picture and asserts nothing, which is how three " +
                "run reports in docs/ came to claim green goldens from runs that compared none.",
        ).that(props.lines().map { it.trim() })
            .contains("roborazzi.test.verify=true")
    }

    @Test
    fun `the CI contour strips test tasks of the right to be recalled`() {
        val root = read("build.gradle.kts")
        assertWithMessage("the -Panima.ci switch must exist")
            .that(root)
            .contains("""providers.gradleProperty("anima.ci")""")
        assertWithMessage(
            "test tasks must lose up-to-dateness in the CI contour — this is the defect " +
                "that produced a six-second green check",
        ).that(root)
            .contains("outputs.upToDateWhen { false }")
        assertWithMessage("and the right to be served from the build cache")
            .that(root)
            .contains("outputs.doNotCacheIf(")
        assertWithMessage(
            "the contour must refuse to run with golden comparison switched off, or the " +
                "one switch does not actually imply the other flag",
        ).that(root)
            .contains("""providers.gradleProperty("roborazzi.test.verify")""")
        assertWithMessage(
            "the build must declare its real Test tasks in the log; tools/ci-verify.py " +
                "cannot tell a lifecycle `:app:test` from a JVM `:core:model:test` without it",
        ).that(root)
            .contains("ANIMA-CI-TEST-TASK")
    }

    @Test
    fun `CI runs the whole check contour under the flag, and then verifies the run`() {
        val ci = read(".github/workflows/ci.yml")
        assertWithMessage(
            "CI must run `check`, not `testDebugUnitTest`: the latter skips every JVM " +
                "module (core:model, 116 executions) and the entire release variant.",
        ).that(ci)
            .contains("-Panima.ci=true check")
        assertWithMessage("CI must not go back to the variant-only test task")
            .that(ci.lines().none { it.trim() == "run: ./gradlew testDebugUnitTest" })
            .isTrue()
        assertWithMessage(
            "CI must run the verifier, or nothing checks that the tests in the previous " +
                "step were executed rather than recalled",
        ).that(ci)
            .contains("tools/ci-verify.py")
    }

    @Test
    fun `the verifier still checks all four things it exists for`() {
        val verifier = read("tools/ci-verify.py")
        listOf(
            "UP-TO-DATE" to "a recalled test task must fail the run",
            "finalizeTestRoborazzi" to "a SKIPPED finalize task means the goldens were never compared",
            "results-summary.json" to "the comparison result must be read from the run's own files",
            "EXECUTIONS" to "the census must name its number, so two runs' numbers stay comparable",
        ).forEach { (needle, why) ->
            assertWithMessage(why).that(verifier).contains(needle)
        }
    }
}
