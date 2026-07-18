package app.anima

import android.content.Context
import android.os.SystemClock
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.glance.appwidget.updateAll
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.anima.core.data.repo.IdentityRepository
import app.anima.core.data.repo.JournalRepository
import app.anima.core.data.repo.SoulRepository
import app.anima.core.model.CreatureConcept
import app.anima.core.model.SoulPort
import app.anima.feature.widget.AnimaWidget
import app.anima.feature.widget.WidgetRefresh
import app.anima.feature.widget.WidgetSnapshot
import com.google.common.truth.Truth.assertThat
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The v0.5 day-in-life scenario: one scripted pass over the SEAMS between
 * features — onboarding hands identity to Home, Home's chat lands in the
 * encrypted vault, Rest runs off the same identity, process recreation
 * rehydrates everything, and the widget + soul export read the very same
 * data through their production entry points.
 *
 * Clock discipline: CreatureSurface owns an endless withFrameNanos loop, so
 * the composition never reaches quiescence under an auto-advancing test
 * clock (the recorded v0.2 lesson). The test therefore pauses mainClock and
 * pumps virtual frames manually inside [waitForCondition] — no Thread.sleep
 * anywhere; real time passes only through the frame pump's own dispatch.
 */
@RunWith(AndroidJUnit4::class)
@HiltAndroidTest
class DayInLifeTest {
    @get:Rule(order = 0)
    val hilt = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val compose = createAndroidComposeRule<MainActivity>()

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface DayEntryPoints {
        fun identity(): IdentityRepository

        fun soul(): SoulRepository

        fun journal(): JournalRepository
    }

    @Test
    fun day_in_life_onboarding_chat_rest_recreate_widget_export() {
        compose.mainClock.autoAdvance = false

        // --- First launch: onboarding, hatch stage (tap skips the episode).
        waitForTag("onboarding.hatch")
        compose.onNodeWithTag("onboarding.hatch").performClick()
        waitForTag("onboarding.meet")
        compose.onNodeWithTag("onboarding.meet").performClick()

        // --- Choose a body: first concept cell, then confirm.
        val conceptTag = "concept.${CreatureConcept.SPIRIT_ORB.wire}"
        waitForTag(conceptTag)
        compose.onNodeWithTag(conceptTag).performClick()
        waitForTag("onboarding.confirmConcept")
        compose.onNodeWithTag("onboarding.confirmConcept").performClick()

        // --- Name + the honest contract ride the same screen; begin.
        waitForTag("onboarding.name.input")
        compose.onNodeWithTag("onboarding.name.input").performTextInput(NAME)
        waitForCondition("begin button accepts the name") {
            nodes(hasTestTag("onboarding.begin")).isNotEmpty()
        }
        compose.onNodeWithTag("onboarding.begin").performClick()

        // --- Home: the fake mind is READY, so the chat input must be there.
        waitForTag("home.chat.input")
        compose.onNodeWithTag("home.chat.input").performTextInput(USER_LINE)
        compose.onNodeWithTag("home.chat.send").performClick()

        // The user's line lands in the thread, then the streamed reply
        // completes and is persisted as a creature message.
        waitForCondition("user message appears in the thread") {
            nodes(hasTestTag("home.chat.message") and hasText(USER_LINE, substring = true)).isNotEmpty()
        }
        waitForCondition("fake mind's streamed reply completes") {
            nodes(hasTestTag("home.chat.message") and hasText(FakeMind.REPLY, substring = true)).isNotEmpty()
        }

        // --- Rest: open, start the shortest session, see it actually running.
        compose.onNodeWithTag("home.rest").performClick()
        waitForTag("rest.start.5")
        compose.onNodeWithTag("rest.start.5").performClick()
        waitForTag("rest.timer")

        // Leave mid-session (guilt-free pause is the product contract).
        compose.onNodeWithTag("rest.back").performClick()
        waitForTag("home.chat.input")

        // --- Exit/return: recreate the activity; state must be alive.
        compose.activityRule.scenario.recreate()
        waitForTag("home.chat.input")
        assertThat(nodes(hasTestTag("onboarding.hatch"))).isEmpty()
        waitForCondition("chat history survives recreation") {
            nodes(hasTestTag("home.chat.message") and hasText(FakeMind.REPLY, substring = true)).isNotEmpty()
        }

        // --- Widget snapshot: the exact provideGlance data path, off-screen.
        val context = ApplicationProvider.getApplicationContext<Context>()
        val entry = EntryPointAccessors.fromApplication(context, DayEntryPoints::class.java)
        val identity = entry.identity()
        runBlocking {
            assertThat(identity.name()).isEqualTo(NAME)
            val concept = identity.concept() ?: CreatureConcept.SPIRIT_ORB
            val seed = identity.seed() ?: 0L
            val vitals = WidgetSnapshot.readVitals(context)
            val mood = WidgetSnapshot.moodFor(vitals, night = false)
            val bitmap =
                WidgetSnapshot.render(
                    concept = concept,
                    seed = seed,
                    mood = mood,
                    vitals = vitals,
                    night = false,
                    growth = 0.5f,
                )
            assertThat(bitmap.width).isEqualTo(WidgetSnapshot.SIZE_PX)
            assertThat(bitmap.height).isEqualTo(WidgetSnapshot.SIZE_PX)
            val pixels = IntArray(bitmap.width * bitmap.height)
            bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
            assertThat(pixels.count { (it ushr 24) != 0 }).isGreaterThan(0)
            bitmap.recycle()
            // The production refresh path itself must not crash: triggers
            // re-arm and a Glance update pass over (zero) instances.
            WidgetRefresh.armTriggers(context)
            AnimaWidget().updateAll(context)
        }

        // --- Soul export: the same assembly SoulViewModel.buildExportIntent
        // performs, through the production repositories.
        val markdown =
            runBlocking {
                val now = System.currentTimeMillis()
                SoulPort.export(
                    creatureName = identity.name() ?: error("creature has no name"),
                    concept = identity.concept() ?: CreatureConcept.SPIRIT_ORB,
                    stats = identity.stats(now),
                    facts = entry.soul().liveFacts().first(),
                    journal = entry.journal().recent(SoulPort.MAX_EXPORT_MOMENTS).first(),
                    nowMillis = now,
                )
            }
        assertThat(markdown.toByteArray().isNotEmpty()).isTrue()
        assertThat(markdown).contains(SoulPort.FORMAT_MARKER)
        assertThat(markdown).contains(NAME)
        // The hatch moment recorded during onboarding must surface in export.
        assertThat(markdown).contains("hatched")
    }

    private fun nodes(matcher: SemanticsMatcher) =
        compose
            .onAllNodes(matcher)
            .fetchSemanticsNodes(atLeastOneRootRequired = false)

    private fun waitForTag(tag: String) = waitForCondition("node '$tag' exists") { nodes(hasTestTag(tag)).isNotEmpty() }

    /**
     * Pumps one virtual frame per poll until [condition] holds. Deadline is
     * wall-clock only as a safety net; progress is driven exclusively by the
     * paused main clock, keeping the run deterministic for UI state.
     */
    private fun waitForCondition(
        what: String,
        timeoutMillis: Long = TIMEOUT_MILLIS,
        condition: () -> Boolean,
    ) {
        val deadline = SystemClock.uptimeMillis() + timeoutMillis
        while (!condition()) {
            if (SystemClock.uptimeMillis() > deadline) {
                throw AssertionError("Timed out after ${timeoutMillis}ms waiting until: $what")
            }
            compose.mainClock.advanceTimeByFrame()
        }
    }

    private companion object {
        const val NAME = "Kiki"
        const val USER_LINE = "hello little one, this is our first day"
        const val TIMEOUT_MILLIS = 60_000L
    }
}
