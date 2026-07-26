// :tools:litertlm-smoke — ADR-020 §5: the first inference contour that runs
// without a phone. A single JUnit test downloads the smallest ready-made
// .litertlm (gemma3-270m-it-q8, ~290 MiB) into the system temp dir (never
// the repo), initializes the LiteRT-LM JVM engine on CPU and asserts one
// prompt→reply round trip. Gated by ANIMA_JVM_LLM_SMOKE=1 — without it the
// test skips instantly, so `check` stays fast and CI never downloads models.
//
//   ANIMA_JVM_LLM_SMOKE=1 ./gradlew :tools:litertlm-smoke:test
//
// Host-tooling module: the download code lives in src/test by design — the
// app-side network budget (NetworkIsolationTest) scans main source sets and
// module build files; this module adds no network *library* anywhere.
plugins {
    alias(libs.plugins.anima.kotlin.jvm)
}

dependencies {
    testImplementation(libs.litertlm.jvm)
    testImplementation(libs.junit4)
    testImplementation(libs.truth)
    // v0.9 Phase B: the bench feeds the model the PRODUCT's prompts
    // (MindVoice.persona + MindPrompts.combine), not invented ones —
    // benchmarking wording we do not ship would measure the wrong thing.
    // :core:model is a pure JVM module with no network surface.
    testImplementation(projects.core.model)
    // The bench separates prefill from decode by streaming, which means
    // collecting the engine's Flow (getBenchmarkInfo is unreachable — see the
    // class KDoc), and that needs runBlocking at compile time.
    testImplementation(libs.kotlinx.coroutines.core)
}

tasks.withType<Test>().configureEach {
    // The smoke needs the env var at execution time; never cache a skip as
    // a pass or vice versa.
    inputs.property("animaJvmLlmSmoke", System.getenv("ANIMA_JVM_LLM_SMOKE") ?: "")
    testLogging {
        events("passed", "skipped", "failed", "standardOut")
        showStandardStreams = true
    }
}
