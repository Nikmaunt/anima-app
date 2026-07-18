package app.anima.feature.rest

import app.anima.core.data.repo.JournalRepository
import app.anima.core.model.JournalKind
import app.anima.core.model.RestClock
import app.anima.core.model.RestPhase
import app.anima.core.model.RestSessions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Holds the one rest session across screens and process-visible time. NOT a
 * background entity: nothing here ticks by itself — the rest screen drives
 * [tick] while RESUMED, [pause] fires when the screen stops. If the process
 * dies mid-session the session simply evaporates (no journal write, no
 * guilt — the covenant's "abandoning leaves no trace" applies).
 */
@Singleton
class RestSessionManager
    @Inject
    constructor(
        private val journal: JournalRepository,
    ) {
        private val state = MutableStateFlow<RestPhase>(RestPhase.Idle)
        val phase: StateFlow<RestPhase> = state.asStateFlow()

        /** True once per completion: the moment the journal entry was written. */
        private val completionRecorded = MutableStateFlow(false)

        fun start(
            plannedMin: Int,
            nowMs: Long,
        ) {
            completionRecorded.value = false
            state.value = RestClock.start(plannedMin, nowMs)
        }

        /**
         * UI clock beat. On the Running→Completed edge the session is written
         * to the body journal exactly once (kind REST_SESSION, detail
         * "planned:actual").
         */
        suspend fun tick(nowMs: Long) {
            val before = state.value
            val after = RestClock.tick(before, nowMs)
            state.value = after
            if (before is RestPhase.Running && after is RestPhase.Completed && !completionRecorded.value) {
                completionRecorded.value = true
                journal.record(
                    kind = JournalKind.REST_SESSION,
                    atMillis = nowMs,
                    detail = RestSessions.detail(after.plannedMin, after.plannedMin),
                )
            }
        }

        fun pause(nowMs: Long) {
            state.value = RestClock.pause(state.value, nowMs)
        }

        fun resume(nowMs: Long) {
            state.value = RestClock.resume(state.value, nowMs)
        }

        fun reset() {
            state.value = RestClock.abandon()
        }
    }
