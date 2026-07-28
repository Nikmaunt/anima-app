package app.anima

import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Test
import java.io.File

/**
 * v1.1 structural guarantees, as a failing build rather than as a promise in
 * a doc. Same source-reading approach as NetworkIsolationTest, and for the
 * same reason: these are claims about what is *not* in the product, and the
 * only honest way to hold them is to look at the shipped sources.
 *
 * What is asserted:
 *
 *  1. Home carries no chat surface at all — the slot is gone from the
 *     scaffold, not merely hidden behind a flag at the call site.
 *  2. Chat is navigable from exactly one place, Settings.
 *  3. The launcher "talk" shortcut is gone.
 *  4. :mind-pack is not in the release bundle.
 */
class ChatDemotionTest {
    private val repoRoot: File by lazy {
        generateSequence(File(System.getProperty("user.dir"))) { it.parentFile }
            .first { File(it, "settings.gradle.kts").exists() }
    }

    private fun source(path: String): String {
        val f = File(repoRoot, path)
        assertWithMessage("missing source: $path").that(f.exists()).isTrue()
        return f.readText()
    }

    private fun stripComments(text: String): String =
        text
            .replace(Regex("/\\*.*?\\*/", RegexOption.DOT_MATCHES_ALL), "")
            .replace(Regex("//[^\n]*"), "")
            .replace(Regex("<!--.*?-->", RegexOption.DOT_MATCHES_ALL), "")

    private val homeScreen by lazy {
        stripComments(source("feature/home/src/main/kotlin/app/anima/feature/home/HomeScreen.kt"))
    }

    private val animaRoot by lazy {
        stripComments(source("app/src/main/kotlin/app/anima/AnimaRoot.kt"))
    }

    @Test
    fun `the home scaffold has no chat slot`() {
        // The v1.0 scaffold took a `chat` lambda and placed it under the
        // creature; that is the thing that made chat reachable from every
        // path that reached Home, i.e. all of them (audit-v10 0.2).
        assertWithMessage("HomeAdaptiveScaffold must not take a chat slot")
            .that(homeScreen)
            .doesNotContain("chat: @Composable")
        assertWithMessage("Home must not compose ChatPanel")
            .that(homeScreen.substringBefore("internal fun ChatPanel"))
            .doesNotContain("ChatPanel(")
    }

    @Test
    fun `chat is navigated to from settings and nowhere else`() {
        val navigations = Regex("""Routes\.CHAT""").findAll(animaRoot).count()
        // Exactly two mentions: the composable() destination and the single
        // onOpenChat callback wired from SettingsScreen.
        assertWithMessage("Routes.CHAT should appear once as a destination and once as a target")
            .that(navigations)
            .isEqualTo(2)
        assertThat(animaRoot).contains("onOpenChat = { nav.navigate(Routes.CHAT) }")
    }

    @Test
    fun `no other screen offers a way into chat`() {
        val callers =
            listOf(
                "feature/home/src/main/kotlin/app/anima/feature/home/HomeScreen.kt",
                "feature/home/src/main/kotlin/app/anima/feature/home/BodyDiaryScreen.kt",
                "feature/rest/src/main/kotlin/app/anima/feature/rest/RestScreen.kt",
                "feature/soul/src/main/kotlin/app/anima/feature/soul/SoulScreen.kt",
                "feature/onboarding/src/main/kotlin/app/anima/feature/onboarding/OnboardingScreen.kt",
            )
        callers.forEach { path ->
            assertWithMessage("$path must not expose an onOpenChat hook")
                .that(stripComments(source(path)))
                .doesNotContain("onOpenChat")
        }
    }

    @Test
    fun `the launcher talk shortcut is gone`() {
        val shortcuts = source("app/src/main/res/xml/shortcuts.xml")
        assertWithMessage("the talk shortcut opened Home, where chat no longer lives")
            .that(shortcuts)
            .doesNotContain("shortcutId=\"talk\"")
        // The other two still work and are still expected.
        assertThat(shortcuts).contains("shortcutId=\"diary\"")
        assertThat(shortcuts).contains("shortcutId=\"rest\"")
    }

    @Test
    fun `mind-pack is not in the release bundle`() {
        val buildFile = stripComments(source("app/build.gradle.kts"))
        assertWithMessage(
            "assetPacks += \":mind-pack\" puts ~1.38 GB (ADR-021) into every install " +
                "for a screen that is off by default",
        ).that(buildFile).doesNotContain("assetPacks")
        // The module itself is demoted, not deleted.
        assertThat(source("settings.gradle.kts")).contains("include(\":mind-pack\")")
        assertThat(File(repoRoot, "mind-pack").isDirectory).isTrue()
    }

    @Test
    fun `the chat screen refuses to dead-end without a mind`() {
        val chat = source("feature/home/src/main/kotlin/app/anima/feature/home/ChatScreen.kt")
        // Turning the toggle on with no mind file installed must land on an
        // honest state with a way forward, not on an input box that can
        // never answer.
        assertThat(chat).contains("state.mindStatus != MindStatus.READY")
        assertThat(chat).contains("chat.noMind")
        assertThat(chat).contains("chat.openMind")
    }
}
