package app.anima

import com.google.common.truth.Truth.assertWithMessage
import org.junit.Test
import java.io.File

/**
 * v1.1b task 1d — the guard that makes the default-body class unrepeatable.
 *
 * The product has exactly one promise: this body belongs to this phone. Before
 * v1.1b there were fourteen places that quietly substituted `SPIRIT_ORB` when
 * identity was not loaded yet, six state fields that defaulted to it, and a
 * matching set of seed-`0L` defaults. Two of them wrote the substitute into
 * durable data. None of them failed a test, because each one looked like
 * ordinary defensive Kotlin at its own call site — the defect only exists as a
 * *class*, and a class can only be held by a rule.
 *
 * The rule: **no production source may name a creature concept as a fallback,
 * and no identity field may default to a seed.** Same source-reading shape as
 * `ChatDemotionTest` and `NetworkIsolationTest`, for the same reason — this is
 * a claim about what must not be in the sources, so the sources are what gets
 * read. This test lives in the ordinary `check` contour, not behind a flag.
 *
 * Where a body genuinely cannot be known, the answer is to draw and write
 * nobody. `docs/design/v11/phase1-wrong-body.md` records what each surface does
 * instead.
 */
class NoDefaultBodyTest {
    private val repoRoot: File by lazy {
        generateSequence(File(System.getProperty("user.dir"))) { it.parentFile }
            .first { File(it, "settings.gradle.kts").exists() }
    }

    /**
     * Production Kotlin only: `src/main` of every module. Tests are excluded on
     * purpose — a test may legitimately construct a named body, and
     * `SoulBackupIdentityTest` does exactly that.
     */
    private fun productionSources(): List<File> =
        listOf("core", "feature", "app/src/main")
            .map { File(repoRoot, it) }
            .flatMap { root -> root.walkTopDown().filter { it.isFile } }
            .filter { it.extension == "kt" }
            .filter { it.path.replace('\\', '/').let { p -> "/src/main/" in p || "/app/src/main/" in p } }
            .filterNot { it.path.replace('\\', '/').contains("/build/") }

    private fun stripComments(text: String): String =
        text
            .replace(Regex("/\\*.*?\\*/", RegexOption.DOT_MATCHES_ALL), "")
            .replace(Regex("//[^\n]*"), "")

    private fun relative(file: File): String =
        file.path
            .removePrefix(repoRoot.path)
            .replace('\\', '/')
            .trimStart('/')

    /**
     * Every concept name, so the guard cannot be walked around by picking a
     * different body. Kept as literals rather than read from the enum: this test
     * is about what the *text* of the sources says.
     */
    private val conceptNames =
        listOf(
            "SPIRIT_ORB",
            "FOX_KIT",
            "JELLY",
            "PIXEL_PET",
            "ROBOT",
            "SPROUT",
            "EMBER",
            "MOTH",
        )

    /**
     * The two files that are allowed to mention concepts next to `?:` or `=`,
     * because naming all eight is their job: the enum itself and the tables that
     * map every concept onto a renderer, a voice, a hue, a preset.
     */
    private val tablesThatMustNameEveryBody =
        setOf(
            "core/model/src/main/kotlin/app/anima/core/model/CreatureConcept.kt",
            "core/model/src/main/kotlin/app/anima/core/model/Personality.kt",
            "core/creature/src/main/kotlin/app/anima/core/creature/render/CreatureRenderer.kt",
            "core/creature/src/main/kotlin/app/anima/core/creature/render/RigScale.kt",
            "core/creature/src/main/kotlin/app/anima/core/creature/ConceptGallery.kt",
            "core/creature/src/main/kotlin/app/anima/core/creature/CreaturePreviews.kt",
            "core/ui/src/main/kotlin/app/anima/core/ui/theme/SignatureHue.kt",
            "core/voice/src/main/kotlin/app/anima/core/voice/VoiceCharacter.kt",
        )

    /**
     * `@Preview` composables are IDE tooling: never composed by the app, never
     * fed by identity, and their whole purpose is to pin arbitrary bodies and
     * seeds so a developer can look at all eight. Exempt from the seed rule as
     * well as the body rule, and named here rather than pattern-matched so the
     * exemption stays exactly one file wide.
     */
    private val previewsOnlyTooling =
        "core/creature/src/main/kotlin/app/anima/core/creature/CreaturePreviews.kt"

    @Test
    fun `no production source falls back to a named body`() {
        val offenders = mutableListOf<String>()
        productionSources().forEach { file ->
            val path = relative(file)
            if (path in tablesThatMustNameEveryBody) return@forEach
            stripComments(file.readText()).lines().forEachIndexed { index, line ->
                val names = conceptNames.filter { it in line }
                if (names.isEmpty()) return@forEachIndexed
                // `?: FOX_KIT` and `?: CreatureConcept.FOX_KIT` — the elvis shape
                // that made up thirteen of the original fourteen sites.
                if (Regex("""\?:\s*(CreatureConcept\.)?(${names.joinToString("|")})""").containsMatchIn(line)) {
                    offenders += "$path:${index + 1}: fallback to a named body — $line"
                }
                // `= CreatureConcept.FOX_KIT` — a state field or property whose
                // default is a body. The `?:` above catches substitution; this
                // catches the initial value, which is what Home actually drew.
                if (Regex("""=\s*CreatureConcept\.(${names.joinToString("|")})""").containsMatchIn(line)) {
                    offenders += "$path:${index + 1}: default body as an initial value — $line"
                }
            }
        }
        assertWithMessage(
            "A body may never be substituted or defaulted. Until identity is known, " +
                "draw and write nobody. See docs/design/v11/phase1-wrong-body.md.\n" +
                offenders.joinToString("\n"),
        ).that(offenders)
            .isEmpty()
    }

    @Test
    fun `no production source falls back to a default seed`() {
        val offenders = mutableListOf<String>()
        productionSources().forEach { file ->
            val path = relative(file)
            if (path == previewsOnlyTooling) return@forEach
            stripComments(file.readText()).lines().forEachIndexed { index, line ->
                if (!Regex("""\bseed\b""", RegexOption.IGNORE_CASE).containsMatchIn(line)) return@forEachIndexed
                // `identity.seed() ?: 0L` and `val seed: Long = 0L`.
                if (Regex("""\?:\s*0L""").containsMatchIn(line) ||
                    Regex("""\bseed\w*\s*:\s*Long\s*=\s*[-0-9]""", RegexOption.IGNORE_CASE).containsMatchIn(line) ||
                    Regex("""\bseed\w*\s*=\s*[-0-9]+L?\s*$""", RegexOption.IGNORE_CASE).containsMatchIn(line)
                ) {
                    offenders += "$path:${index + 1}: default seed — $line"
                }
            }
        }
        assertWithMessage(
            "A default seed is a different creature's personality, exactly as much as a " +
                "default body is a different creature. Until the seed is read, draw nobody.\n" +
                offenders.joinToString("\n"),
        ).that(offenders)
            .isEmpty()
    }

    /**
     * The one that would have caught the original defect on the screen the owner
     * saw it on. `stateIn` must be given an initial value, and that value is the
     * first frame of every launch — so `HomeUiState`'s identity fields have to be
     * nullable or the flash comes straight back.
     */
    @Test
    fun `the Home state cannot name a body or a seed before it reads one`() {
        val text =
            File(repoRoot, "feature/home/src/main/kotlin/app/anima/feature/home/HomeViewModel.kt")
                .readText()
                .let(::stripComments)
        assertWithMessage("HomeUiState.concept must be nullable — it is the first frame of every launch")
            .that(text)
            .contains("val concept: CreatureConcept? = null")
        assertWithMessage("HomeUiState.seed must be nullable for the same reason")
            .that(text)
            .contains("val seed: Long? = null")
    }

    /**
     * v1.1c task 2.3. A default *seed* is the same defect as a default body,
     * one level deeper: `deviceSeed()` used to fall back to the FNV-1a of the
     * literal string `"anima-fallback"`, so every phone whose `ANDROID_ID` came
     * back null got not merely the same body but the same life — same hue, same
     * size, same blink rate, same name. Roll once and remember; never a constant.
     */
    @Test
    fun `an unreadable device id does not become the same creature on every phone`() {
        val offenders = mutableListOf<String>()
        productionSources().forEach { file ->
            val text = stripComments(file.readText())
            if ("ANDROID_ID" !in text) return@forEach
            text.lines().forEachIndexed { index, line ->
                // `?: "anything"` on a line in a file that reads the device id.
                if (Regex("""\?:\s*"[^"]*"""").containsMatchIn(line)) {
                    offenders += "${relative(file)}:${index + 1}: constant device-id fallback — $line"
                }
            }
        }
        assertWithMessage(
            "A phone that cannot read its own id must roll a seed ONCE and remember it, not " +
                "share a constant with every other such phone.\n" + offenders.joinToString("\n"),
        ).that(offenders)
            .isEmpty()

        val deviceSeed =
            File(repoRoot, "core/data/src/main/kotlin/app/anima/core/data/identity/DeviceSeed.kt")
        assertWithMessage("the rolled seed must be written down, or a reinstall is a different creature")
            .that(deviceSeed.readText())
            .contains("rememberDeviceSeedFallback")
    }

    /**
     * v1.1c task 2.5 — the eight-body catalogue is unreachable from the UI.
     *
     * It is not deleted: `ConceptGallery` stays in `core:creature`, and so does
     * `IdentityRepository.switchConcept`. What is gone is every surface that
     * called them — the onboarding CHOOSE stage and the Settings body grid, in
     * which **one tap rewrote the body with no confirmation and no undo**. A
     * catalogue says the body is merchandise, and the body is not chosen.
     */
    @Test
    fun `no screen shows the catalogue of eight bodies`() {
        val gallery = "core/creature/src/main/kotlin/app/anima/core/creature/ConceptGallery.kt"
        assertWithMessage("ConceptGallery is demoted, not deleted — the same rule chat lives under")
            .that(File(repoRoot, gallery).exists())
            .isTrue()

        val callers =
            productionSources()
                .filterNot { relative(it) == gallery }
                .filter { "ConceptGallery(" in stripComments(it.readText()) }
                .map(::relative)
        assertWithMessage(
            "A screen composing ConceptGallery is a screen that lets the body be picked. " +
                "The creature passport replaced it: feature/settings/.../PassportScreen.kt.",
        ).that(callers)
            .isEmpty()
    }

    /**
     * The two durable write paths from task 1a. A read draws one wrong frame; a
     * write travels to the next phone inside the soul file.
     */
    @Test
    fun `the soul file refuses an unknown body instead of substituting one`() {
        val backup =
            File(repoRoot, "core/data/src/main/kotlin/app/anima/core/data/backup/SoulBackup.kt")
                .readText()
                .let(::stripComments)
        assertWithMessage("export must refuse rather than name a body it does not know")
            .that(backup)
            .contains("throw NoBodyToExport()")
        assertWithMessage("import must refuse before the first durable write")
            .that(backup)
            .contains("throw UnknownBodyInFile()")

        val concept =
            File(repoRoot, "core/model/src/main/kotlin/app/anima/core/model/CreatureConcept.kt")
                .readText()
                .let(::stripComments)
        assertWithMessage("fromWire must return null for an unknown wire value, never a body")
            .that(concept)
            .contains("fun fromWire(wire: String): CreatureConcept? =")
    }
}
