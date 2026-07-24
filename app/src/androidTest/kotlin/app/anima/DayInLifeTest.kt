package app.anima

import android.content.Context
import android.os.SystemClock
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import androidx.glance.appwidget.updateAll
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.anima.core.data.repo.IdentityRepository
import app.anima.core.data.repo.JournalRepository
import app.anima.core.data.repo.SoulRepository
import app.anima.core.data.repo.TimeCapsuleRepository
import app.anima.core.model.AnimaClock
import app.anima.core.model.CreatureConcept
import app.anima.core.model.JournalKind
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

        fun capsules(): TimeCapsuleRepository

        fun clock(): AnimaClock
    }

    @Test
    fun day_in_life_onboarding_chat_rest_recreate_widget_export() {
        compose.mainClock.autoAdvance = false

        // --- First launch: onboarding, hatch stage (tap skips the episode).
        waitForTag("onboarding.hatch")

        // v0.6: seed a capsule that is ALREADY DUE before Home's ViewModel
        // ever exists — delivery runs once in its init, and recreate() keeps
        // ViewModels alive, so this is the only honest seam. The letter must
        // then arrive with the first visit.
        val context0 = ApplicationProvider.getApplicationContext<Context>()
        val entry0 = EntryPointAccessors.fromApplication(context0, DayEntryPoints::class.java)
        runBlocking {
            val now = entry0.clock().nowMillis()
            entry0.capsules().write(CAPSULE_FROM_PAST, now - 60_000, now - HOLD_MILLIS)
        }

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

        // --- Home: the letter from the past arrives with the first visit.
        waitForTag("home.capsule.keep")
        waitForCondition("the capsule's own words are on screen") {
            nodes(hasText(CAPSULE_FROM_PAST, substring = true)).isNotEmpty()
        }
        compose.onNodeWithTag("home.capsule.keep").performClick()
        waitForCondition("capsule card retires after being kept") {
            nodes(hasTestTag("home.capsule.keep")).isEmpty()
        }

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

        // --- Diary: write a letter to the future; the creature holds it.
        compose.onNodeWithTag("home.diary").performClick()
        waitForTag("diary.capsule.input")
        // The capsule section sits below the fold of the diary's scroll
        // column. performScrollTo() DEADLOCKS under a paused mainClock (its
        // scroll animation waits for frames only this thread can pump —
        // caught live by jdb on the first v0.6 GMD run), so off-screen
        // controls are driven through their semantics actions instead.
        compose.onNodeWithTag("diary.capsule.input").performTextInput(CAPSULE_TO_FUTURE)
        waitForTag("diary.capsule.week")
        compose.onNodeWithTag("diary.capsule.week").performSemanticsAction(SemanticsActions.OnClick)
        // Semantics click: after IME + text input the gesture pipeline
        // proved unreliable under the paused clock (60s of pumped frames
        // without the pop landing); the semantics action invokes onBack
        // directly.
        compose.onNodeWithTag("diary.back").performSemanticsAction(SemanticsActions.OnClick)
        // The pop transition cross-fades both screens; wait until the diary
        // is genuinely gone, not merely until home is back.
        waitForCondition("diary fully leaves composition") {
            nodes(hasTestTag("diary.capsule.input")).isEmpty()
        }
        waitForTag("home.chat.input")

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

        // --- Soul import (the v0.1 promise, re-verified): paste another
        // AI's answer, find facts, keep one — only then is it persisted.
        compose.onNodeWithTag("home.soul").performClick()
        waitForTag("soul.list")
        // The import card is deep in a LazyColumn — not composed until the
        // list is scrolled there. Scroll by raw swipes + the frame pump:
        // scrollTo-style actions animate against the paused clock and
        // deadlock (jdb-caught on the first v0.6 GMD run).
        waitForCondition("import card composes after swiping down the soul list") {
            if (nodes(hasTestTag("soul.import.input")).isNotEmpty()) {
                true
            } else {
                compose.onNodeWithTag("soul.list").performTouchInput { swipeUp() }
                false
            }
        }
        compose
            .onNodeWithTag("soul.import.input")
            .performTextInput("## Preferences\n- $IMPORTED_FACT")
        waitForTag("soul.import.find")
        compose.onNodeWithTag("soul.import.find").performSemanticsAction(SemanticsActions.OnClick)
        // The candidate row lands BELOW the import card — swipe it into
        // composition the same way.
        waitForCondition("import candidate composes") {
            if (nodes(hasTestTag("soul.import.keep")).isNotEmpty()) {
                true
            } else {
                compose.onNodeWithTag("soul.list").performTouchInput { swipeUp() }
                false
            }
        }
        compose.onNodeWithTag("soul.import.keep").performSemanticsAction(SemanticsActions.OnClick)
        waitForCondition("imported fact leaves the candidate list") {
            nodes(hasTestTag("soul.import.keep")).isEmpty()
        }
        runBlocking {
            val imported =
                entry
                    .soul()
                    .liveFacts()
                    .first()
                    .first { it.text == IMPORTED_FACT }
            assertThat(imported.source.wire).isEqualTo("import_confirmed")
        }
        compose.onNodeWithTag("soul.back").performClick()
        waitForCondition("soul screen fully leaves composition") {
            nodes(hasTestTag("soul.import.input")).isEmpty()
        }
        waitForTag("home.chat.input")

        // --- The capsule ledger tells the whole story: the letter from the
        // past was opened, the letter to the future is still held.
        runBlocking {
            val clockNow = entry.clock().nowMillis()
            assertThat(entry.capsules().due(clockNow)).isEmpty()
            assertThat(entry.capsules().heldCount(clockNow).first()).isEqualTo(1)
            assertThat(entry.journal().countOf(JournalKind.CAPSULE_DELIVERED)).isEqualTo(1)
        }

        // --- Evening ritual: the test clock pins 21:30, so the moon is up.
        // The farewell line is deterministic (seed + epoch day), which lets
        // us assert the exact words the creature says back.
        val expectedGoodnight =
            runBlocking {
                val pool =
                    context.resources.getStringArray(
                        app.anima.feature.home.R.array.home_goodnight_pool,
                    )
                val epochDay =
                    entry.clock().nowMillis() /
                        app.anima.core.model.RelationshipStats.DAY_MILLIS
                val seed = (entry.identity().seed() ?: 0L) + epochDay
                pool[(seed % pool.size).toInt().let { if (it < 0) it + pool.size else it }]
            }
        waitForTag("home.goodnight")
        compose.onNodeWithTag("home.goodnight").performClick()
        waitForCondition("the creature says its goodnight line") {
            nodes(hasTestTag("home.chat.message") and hasText(expectedGoodnight, substring = true)).isNotEmpty()
        }
        waitForCondition("the moon retires — one farewell per evening") {
            nodes(hasTestTag("home.goodnight")).isEmpty()
        }
        runBlocking {
            assertThat(entry.journal().countOf(JournalKind.GOODNIGHT)).isEqualTo(1)
        }
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
        const val CAPSULE_FROM_PAST = "a letter from a braver morning"
        const val IMPORTED_FACT = "keeps a tiny succulent alive"
        const val CAPSULE_TO_FUTURE = "future us: remember this first day"
        const val HOLD_MILLIS = 7L * 24 * 60 * 60 * 1000

        // v0.7: 60s did not survive a COLD emulator boot from a slow disk
        // (GMD boots fresh every run; first-after-boot run needs >60s to the
        // first frame, warm runs pass in seconds — diagnosed live, audit-v06).
        // This is a wall-clock SAFETY NET only; progress is still driven
        // exclusively by the paused main clock, so a larger net changes
        // nothing about determinism — it only stops punishing cold starts.
        const val TIMEOUT_MILLIS = 240_000L
    }
}
