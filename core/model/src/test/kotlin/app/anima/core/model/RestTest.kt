package app.anima.core.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class RestTest {
    @Test
    fun `running accrues only anchored time and completes exactly at plan`() {
        var phase: RestPhase = RestClock.start(plannedMin = 5, nowMs = 1_000L)
        phase = RestClock.tick(phase, nowMs = 1_000L + 4 * 60_000L)
        assertThat(phase).isInstanceOf(RestPhase.Running::class.java)
        phase = RestClock.tick(phase, nowMs = 1_000L + 5 * 60_000L)
        assertThat(phase).isEqualTo(RestPhase.Completed(5))
    }

    @Test
    fun `pause freezes time and resume continues without loss or gain`() {
        var phase: RestPhase = RestClock.start(plannedMin = 10, nowMs = 0L)
        // 3 minutes in, the user leaves the app.
        phase = RestClock.pause(phase, nowMs = 3 * 60_000L)
        assertThat(phase).isEqualTo(RestPhase.Waiting(10, 3 * 60_000L))
        // An hour passes — the wait costs nothing.
        phase = RestClock.resume(phase, nowMs = 63 * 60_000L)
        // 7 more minutes complete the session.
        phase = RestClock.tick(phase, nowMs = 70 * 60_000L)
        assertThat(phase).isEqualTo(RestPhase.Completed(10))
    }

    @Test
    fun `abandoning leaves idle and ticking idle stays idle`() {
        assertThat(RestClock.abandon()).isEqualTo(RestPhase.Idle)
        assertThat(RestClock.tick(RestPhase.Idle, 99L)).isEqualTo(RestPhase.Idle)
        assertThat(RestClock.pause(RestPhase.Idle, 99L)).isEqualTo(RestPhase.Idle)
    }

    @Test
    fun `completed is terminal for tick and pause`() {
        val done = RestPhase.Completed(5)
        assertThat(RestClock.tick(done, 1L)).isEqualTo(done)
        assertThat(RestClock.pause(done, 1L)).isEqualTo(done)
        assertThat(RestClock.resume(done, 1L)).isEqualTo(done)
    }

    @Test
    fun `detail codec round-trips and rejects garbage`() {
        assertThat(RestSessions.parseDetail(RestSessions.detail(15, 15))).isEqualTo(15 to 15)
        assertThat(RestSessions.parseDetail(null)).isNull()
        assertThat(RestSessions.parseDetail("nonsense")).isNull()
        assertThat(RestSessions.parseDetail("0:5")).isNull()
        assertThat(RestSessions.parseDetail("5:-1")).isNull()
    }

    @Test
    fun `rest sessions feed evolution but do not dominate it`() {
        val base = RelationshipStats(0L, conversationCount = 0, liveFactCount = 0, chargeCount = 0)
        val rested = base.copy(restSessionCount = 10)
        val now = 0L
        assertThat(Evolution.score(rested, now) - Evolution.score(base, now))
            .isWithin(0.001f)
            .of(10 * Evolution.REST_WEIGHT)
    }
}
