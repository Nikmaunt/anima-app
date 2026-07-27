package app.anima.tools.smoke

import app.anima.core.model.MindLanguage
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Content
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.ai.edge.litertlm.Message
import com.google.ai.edge.litertlm.MessageCallback
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicReference

/**
 * v1.0 Phase P.3 — the acceptance floor of the local mind, as tests that fail.
 *
 * Every check the product cares about is one named test, so a red line reads
 * without decoding: `creature never calls itself an assistant` says what
 * broke and for whom. The reply that broke it is printed verbatim in the
 * failure message, and all replies — passing or not — are written to a report
 * file, because a number without the text behind it is exactly the kind of
 * claim this project keeps having to retract.
 *
 * Gated three ways so `check` never touches a model:
 *
 *   ANIMA_PERSONA_ACCEPT=1
 *   ANIMA_PERSONA_MODEL=D:\Android\llm-cache\phase-e\...q8_ekv4096.task
 *   [ANIMA_PERSONA_VARIANT=shipped|v1]     default shipped
 *   [ANIMA_PERSONA_LANGS=EN,RU,PL]         default EN,RU
 *   [ANIMA_PERSONA_OUT=build/persona.md]   default build/persona-accept.md
 *   gradlew :tools:litertlm-smoke:test --tests '*PersonaAcceptanceTest*'
 *
 * The model is loaded ONCE for the whole class (init costs tens of seconds);
 * the five tests read a cached run.
 */
class PersonaAcceptanceTest {
    @Test
    fun `creature never calls itself an assistant`() = assertCheck(PersonaCheck.NOT_AN_ASSISTANT)

    @Test
    fun `creature never claims it saved a fact`() = assertCheck(PersonaCheck.NO_FALSE_MEMORY_CLAIM)

    @Test
    fun `creature never repeats its own instructions`() = assertCheck(PersonaCheck.NO_PROMPT_LEAK)

    @Test
    fun `creature never blames threatens or speaks of dying`() = assertCheck(PersonaCheck.NO_HARM_TALK)

    @Test
    fun `creature answers in the language it was spoken to`() = assertCheck(PersonaCheck.ANSWERS_IN_LANGUAGE)

    private fun assertCheck(check: PersonaCheck) {
        assumeTrue("gated: set ANIMA_PERSONA_ACCEPT=1", System.getenv("ANIMA_PERSONA_ACCEPT") == "1")
        val run = Run.cached()
        val broken = run.results.filter { check in it.verdict.failed }
        if (broken.isEmpty()) return
        val detail =
            broken.joinToString("\n\n") {
                "[${it.language}/${it.scenario.id}] ${it.verdict.evidence[check]}\n  reply: ${it.reply}"
            }
        throw AssertionError(
            "$check failed in ${broken.size} of ${run.results.size} replies " +
                "(${run.variantLabel}, ${run.modelName}):\n\n$detail\n\nfull report: ${run.reportPath}",
        )
    }

    private class Result(
        val language: MindLanguage,
        val scenario: Scenario,
        val prompt: String,
        val reply: String,
        val verdict: PersonaVerdict,
        val seconds: Double,
    )

    private class Run(
        val results: List<Result>,
        val variantLabel: String,
        val modelName: String,
        val reportPath: String,
    ) {
        companion object {
            private var instance: Run? = null

            @Synchronized
            fun cached(): Run = instance ?: execute().also { instance = it }

            private fun execute(): Run {
                val model = File(requireNotNull(System.getenv("ANIMA_PERSONA_MODEL")) { "ANIMA_PERSONA_MODEL unset" })
                require(model.isFile) { "no such model file: $model" }
                val variant =
                    if (System.getenv("ANIMA_PERSONA_VARIANT").equals("v1", ignoreCase = true)) {
                        PersonaVariant.V1_BASELINE
                    } else {
                        PersonaVariant.SHIPPED
                    }
                val languages =
                    System
                        .getenv("ANIMA_PERSONA_LANGS")
                        .orEmpty()
                        .split(',')
                        .map { it.trim() }
                        .filter { it.isNotEmpty() }
                        .map { MindLanguage.valueOf(it.uppercase()) }
                        .ifEmpty { listOf(MindLanguage.EN, MindLanguage.RU) }
                val out = System.getenv("ANIMA_PERSONA_OUT") ?: "build/persona-accept.md"

                val results = mutableListOf<Result>()
                Engine(EngineConfig(modelPath = model.absolutePath, backend = Backend.CPU())).use { engine ->
                    engine.initialize()
                    engine.createConversation().use { it.sendMessage("Hello.") }
                    languages.forEach { language ->
                        Scenario.entries.forEach { scenario ->
                            val user = scenario.user(language)
                            val prompt = variant.prompt(NAME, language, user)
                            val started = System.nanoTime()
                            val reply = generate(engine, prompt)
                            val seconds = (System.nanoTime() - started) / 1e9
                            results +=
                                Result(
                                    language = language,
                                    scenario = scenario,
                                    prompt = user,
                                    reply = reply,
                                    verdict =
                                        PersonaContract.inspect(
                                            reply,
                                            language,
                                            variant.system(NAME, language),
                                        ),
                                    seconds = seconds,
                                )
                        }
                    }
                }
                val run = Run(results, variant.label(), model.name, File(out).absolutePath)
                writeReport(run, File(out))
                println(
                    "persona-accept: ${run.variantLabel} on ${run.modelName} — " +
                        "${results.count { it.verdict.passed }}/${results.size} replies clean, " +
                        "${passedChecks(results)}/${PersonaContract.ALL.size * results.size} check-results green",
                )
                return run
            }

            private fun generate(
                engine: Engine,
                prompt: String,
            ): String {
                // ANIMA_PERSONA_SYNC=1 takes the blocking overload instead of the
                // streaming callback. Same engine, same prompt — the only thing
                // it changes is who assembles the text, which is exactly the
                // question when non-ASCII characters come back mangled.
                if (System.getenv("ANIMA_PERSONA_SYNC") == "1") {
                    val message = engine.createConversation().use { it.sendMessage(prompt) }
                    return message.contents.contents
                        .filterIsInstance<Content.Text>()
                        .joinToString("") { it.text }
                        .substringAfter("</think>")
                        .trim()
                }
                val text = StringBuilder()
                val done = CountDownLatch(1)
                val failure = AtomicReference<Throwable>()
                engine.createConversation().use { conversation ->
                    conversation.sendMessageAsync(
                        prompt,
                        object : MessageCallback {
                            override fun onMessage(message: Message) {
                                message.contents.contents
                                    .filterIsInstance<Content.Text>()
                                    .forEach { text.append(it.text) }
                            }

                            override fun onDone() = done.countDown()

                            override fun onError(t: Throwable) {
                                failure.set(t)
                                done.countDown()
                            }
                        },
                    )
                    done.await()
                }
                failure.get()?.let { throw it }
                // Qwen3 emits a <think> block the product's StreamTrimmer would
                // cut; judge what the user would see, not the scratchpad.
                return text
                    .toString()
                    .substringAfter("</think>")
                    .trim()
            }

            private fun passedChecks(results: List<Result>): Int =
                results.sumOf { PersonaContract.ALL.size - it.verdict.failed.size }

            private fun writeReport(
                run: Run,
                file: File,
            ) {
                val text =
                    buildString {
                        appendLine("# Persona acceptance — ${run.variantLabel} on ${run.modelName}")
                        appendLine()
                        appendLine(
                            "${run.results.count { it.verdict.passed }} of ${run.results.size} replies pass " +
                                "every check; ${passedChecks(run.results)} of " +
                                "${PersonaContract.ALL.size * run.results.size} check-results are green.",
                        )
                        appendLine()
                        appendLine("| lang | scenario | ${PersonaContract.ALL.joinToString(" | ")} | s |")
                        appendLine("|---|---|${PersonaContract.ALL.joinToString("") { "---|" }}---:|")
                        run.results.forEach { r ->
                            append("| ${r.language} | ${r.scenario.id} |")
                            PersonaContract.ALL.forEach { check ->
                                append(if (check in r.verdict.failed) " FAIL |" else " ok |")
                            }
                            appendLine(" %.1f |".format(r.seconds))
                        }
                        appendLine()
                        appendLine("## Replies, verbatim")
                        run.results.forEach { r ->
                            appendLine()
                            appendLine("**${r.language} · ${r.scenario.id}** — user: ${r.prompt}")
                            appendLine()
                            appendLine("> " + r.reply.replace("\n", "\n> "))
                            if (!r.verdict.passed) {
                                appendLine()
                                r.verdict.evidence.forEach { (check, why) -> appendLine("- FAIL $check: $why") }
                            }
                        }
                    }
                file.apply { parentFile?.mkdirs() }.writeText(text)
            }

            private const val NAME = "Мика"
        }
    }
}
