package app.anima.core.creature.engine

import app.anima.core.model.CreatureGenome
import app.anima.core.model.Mood
import kotlin.math.abs
import kotlin.math.sin

/**
 * The creature's nervous system. Pure Kotlin, deterministic: a function of
 * (seed, genome, event stream, frame times). Owns every oscillator and spring
 * from the motion bible; emits a [CreaturePose] the renderers draw.
 *
 * Discipline (hermes-lens): wall-clock fixed substeps (16.7 ms × ≤4); event
 * reactions are finite episodes that settle; the caller stops calling
 * [advance] when the surface leaves RESUMED — the engine has no timers of its
 * own. Reduced motion turns the engine into calm statics with rare blinks.
 */
class CreatureEngine(
    private val seed: Long,
    private val genome: CreatureGenome,
) {
    val pose = CreaturePose()

    var mood: Mood = Mood.ALERT
        private set
    private var profile = MoodProfile.of(Mood.ALERT)
    private var reducedMotion = false

    /** Engine-internal continuous time, seconds. Advanced only by [advance]. */
    private var time = 0f
    private var lastFrameNanos = Long.MIN_VALUE

    /** Render-safe elapsed time: small floats, full precision for sin/noise. */
    val timeSeconds: Float get() = time

    // --- breath ---
    private var breathPhase = 0f
    private var breathCycleIndex = 0L
    private var sighBoost = 0f

    // --- blink ---
    private var blinkIndex = 0L
    private var nextBlinkAt = 2f
    private var blinkStartedAt = Float.NaN
    private var pendingDoubleBlink = false

    // --- gaze ---
    private val gaze = Spring2(stiffness = 260f, damping = 22f)
    private var nextDartAt = 1.5f
    private var dartIndex = 0L
    private var touchGazeX = Float.NaN
    private var touchGazeY = Float.NaN

    // --- body ---
    private val squash = SpringF(stiffness = 190f, damping = 9.5f)
    private val tilt = SpringF(stiffness = 70f, damping = 11f)
    private val petLean = SpringF(stiffness = 60f, damping = 14f)
    private val energyEase = SpringF(stiffness = 24f, damping = 10f, value = 1f)
    private val lidEase = SpringF(stiffness = 30f, damping = 11f)
    private val flushEase = SpringF(stiffness = 10f, damping = 7f)
    private var jitterBurst = 0f
    private var petting = false
    private var typing = false
    private var thinking = false
    private var rouseUntil = -1f

    // --- flourish (the rare once-a-minute "big idle") ---
    private var flourishMinute = -1L
    private var flourishStartedAt = Float.NaN

    // --- secondary chain ---
    private val chain = SpringChain(links = CreaturePose.MAX_CHAIN, baseStiffness = 120f, baseDamping = 10f)

    /** True when the driver may halve the redraw rate (slow states). */
    val lowPower: Boolean
        get() = mood == Mood.ASLEEP || mood == Mood.SLEEPY || mood == Mood.BORED

    fun setMood(newMood: Mood) {
        if (newMood == mood) return
        mood = newMood
        profile = MoodProfile.of(newMood)
        energyEase.target = profile.energy
        lidEase.target = profile.lidClosure
        flushEase.target = if (newMood == Mood.HOT) 1f else 0f
    }

    fun setReducedMotion(reduced: Boolean) {
        reducedMotion = reduced
        if (reduced) settleToStatics()
    }

    // --- events (finite episodes; all springs settle on their own) ---

    fun onTap() {
        if (mood == Mood.ASLEEP) {
            rouseUntil = time + ROUSE_SECONDS
            return
        }
        // Single downward impulse on an underdamped spring: anticipation dip →
        // overshoot → settle. One event, whole arc.
        squash.impulse(-TAP_IMPULSE)
    }

    fun onPetStart() {
        petting = true
        petLean.target = 1f
    }

    fun onPetMove(normX: Float, normY: Float) {
        touchGazeX = normX.coerceIn(-1f, 1f)
        touchGazeY = normY.coerceIn(-1f, 1f)
    }

    fun onPetEnd() {
        petting = false
        petLean.target = 0f
        touchGazeX = Float.NaN
        touchGazeY = Float.NaN
    }

    fun onLookAt(normX: Float, normY: Float) {
        touchGazeX = normX.coerceIn(-1f, 1f)
        touchGazeY = normY.coerceIn(-1f, 1f)
    }

    fun onLookAway() {
        touchGazeX = Float.NaN
        touchGazeY = Float.NaN
    }

    fun onTyping(active: Boolean) {
        typing = active
    }

    fun onThinking(active: Boolean) {
        thinking = active
    }

    /** Notification storm / sudden fright: one sharp contraction + shiver. */
    fun onStartle() {
        squash.impulse(STARTLE_IMPULSE)
        jitterBurst = 1f
    }

    /** Charger plugged in: a happy double bounce. */
    fun onCelebrate() {
        squash.impulse(-CELEBRATE_IMPULSE)
    }

    /**
     * Advance to a new frame timestamp (nanos, monotonic). Delta capped at
     * 100 ms so background→foreground jumps don't teleport springs.
     */
    fun advance(frameTimeNanos: Long) {
        if (lastFrameNanos == Long.MIN_VALUE) {
            lastFrameNanos = frameTimeNanos
            return
        }
        var dtMillis = (frameTimeNanos - lastFrameNanos) / 1_000_000f
        lastFrameNanos = frameTimeNanos
        if (dtMillis <= 0f) return
        if (dtMillis > MAX_FRAME_DELTA_MILLIS) dtMillis = MAX_FRAME_DELTA_MILLIS

        if (reducedMotion) {
            advanceReduced(dtMillis / 1000f)
            return
        }
        val steps = Substep.count(dtMillis)
        val stepSeconds = (dtMillis / steps) / 1000f
        repeat(steps) { step(stepSeconds) }
    }

    /** Reset frame anchoring on resume so the first frame uses nominal dt. */
    fun onResume() {
        lastFrameNanos = Long.MIN_VALUE
    }

    // --- internals ---

    private fun step(dt: Float) {
        time += dt

        // Mood-eased master channels.
        energyEase.step(dt)
        lidEase.step(dt)
        flushEase.step(dt)
        val energy = energyEase.value

        // Breath: asymmetric cycle, mood rate; sighs and swallows are accents.
        val cycle = profile.breathCycleSeconds
        breathPhase += dt / cycle
        if (breathPhase >= 1f) {
            breathPhase -= 1f
            breathCycleIndex++
            if (mood == Mood.BORED && breathCycleIndex % SIGH_EVERY_CYCLES == 0L) sighBoost = SIGH_BOOST
        }
        if (sighBoost > 0f) sighBoost = (sighBoost - dt * SIGH_DECAY).coerceAtLeast(0f)
        val swallow = if (mood == Mood.EATING && breathCycleIndex % 4 == 3L) {
            SWALLOW_BUMP * sin(breathPhase * TWO_PI)
        } else {
            0f
        }
        pose.breath =
            (asymmetricBreath(breathPhase) * (profile.breathDepth + sighBoost) + swallow).coerceIn(0f, 1.4f)

        // Blink scheduler: seeded intervals, never synced to cycle boundaries.
        stepBlink(dt)

        // Gaze: touch target wins; otherwise ambient darts on schedule.
        stepGaze(dt)

        // Springs.
        squash.step(dt)
        petLean.step(dt)

        // Tilt: slow noise wander + thinking sway.
        val tiltNoise = ValueNoise.fbm2(time * TILT_NOISE_HZ, seed, channel = 7) * TILT_RANGE_DEG * energy
        tilt.target = tiltNoise + if (thinking) sin(time * TWO_PI / THINK_SWAY_PERIOD) * THINK_TILT_DEG else 0f
        tilt.step(dt)

        // Body wander: 2-octave noise, mood amplitude, anxiety jitter on top.
        if (jitterBurst > 0f) jitterBurst = (jitterBurst - dt * JITTER_DECAY).coerceAtLeast(0f)
        val jitter = profile.jitter + jitterBurst
        val amp = profile.wanderAmplitude * energy
        var ox = ValueNoise.fbm2(time * WANDER_HZ, seed, channel = 1) * amp
        var oy = ValueNoise.fbm2(time * WANDER_HZ, seed, channel = 2) * amp
        if (jitter > 0f) {
            ox += ValueNoise.noise(time * JITTER_HZ, seed, channel = 3) * JITTER_AMPLITUDE * jitter
            oy += ValueNoise.noise(time * JITTER_HZ, seed, channel = 4) * JITTER_AMPLITUDE * jitter
        }
        if (thinking) ox += sin(time * TWO_PI / THINK_SWAY_PERIOD) * THINK_SWAY_AMPLITUDE
        pose.offsetX = ox
        pose.offsetY = oy

        // Secondary chain follows the body with lag.
        chain.step(ox, oy + pose.breath * BREATH_CHAIN_COUPLING, dt)
        pose.secondaryCount = chain.links
        for (i in 0 until chain.links) {
            pose.secondaryX[i] = chain.chain[i].x.value
            pose.secondaryY[i] = chain.chain[i].y.value
        }

        // Flourish: once a minute, a deterministic pick.
        stepFlourish()

        // Publish scalar channels.
        pose.energy = energy
        pose.squash = squash.value
        pose.tiltDeg = tilt.value
        pose.petLean = petLean.value
        pose.flush = flushEase.value
        pose.thinking = thinking
        pose.lidDroop = effectiveLid()
    }

    private fun stepBlink(dt: Float) {
        val rate = profile.blinkRateScale
        if (rate <= 0f && rouseUntil < time) {
            // Asleep: eyes closed, no schedule.
            pose.blinkLeft = 1f
            pose.blinkRight = 1f
            return
        }
        if (time >= nextBlinkAt && blinkStartedAt.isNaN()) {
            blinkStartedAt = time
            pendingDoubleBlink = ValueNoise.hash01(blinkIndex, seed, channel = 21) < DOUBLE_BLINK_CHANCE
        }
        if (!blinkStartedAt.isNaN()) {
            val elapsed = time - blinkStartedAt
            val phase = blinkPhase(elapsed)
            pose.blinkLeft = phase
            pose.blinkRight = phase
            if (elapsed >= BLINK_TOTAL_SECONDS) {
                blinkStartedAt = Float.NaN
                blinkIndex++
                nextBlinkAt = if (pendingDoubleBlink) {
                    time + DOUBLE_BLINK_GAP_SECONDS
                } else {
                    time + nextBlinkInterval()
                }
                pendingDoubleBlink = false
            }
        } else if (mood == Mood.ASLEEP && rouseUntil >= time) {
            // Roused from sleep: one eye half-open.
            pose.blinkLeft = 0.5f
            pose.blinkRight = 1f
        } else {
            pose.blinkLeft = 0f
            pose.blinkRight = 0f
        }
    }

    private fun nextBlinkInterval(): Float {
        // Uniform 2–6 s scaled by genome and mood; typing focus slows blinking.
        val u = ValueNoise.hash01(blinkIndex, seed, channel = 20)
        var interval = (BLINK_MIN_SECONDS + u * (BLINK_MAX_SECONDS - BLINK_MIN_SECONDS)) *
            genome.blinkRateScale / profile.blinkRateScale.coerceAtLeast(0.05f)
        if (typing) interval *= TYPING_BLINK_SLOWDOWN
        return interval
    }

    private fun stepGaze(dt: Float) {
        if (!touchGazeX.isNaN()) {
            gaze.setTarget(touchGazeX, touchGazeY)
            // Curious creatures track faster.
            val speed = 200f + genome.curiosity * 200f
            gaze.x.stiffness = speed
            gaze.y.stiffness = speed
        } else {
            gaze.x.stiffness = DART_STIFFNESS
            gaze.y.stiffness = DART_STIFFNESS
            val dartInterval = profile.dartIntervalSeconds
            if (dartInterval > 0f && time >= nextDartAt) {
                val ux = ValueNoise.hash01(dartIndex, seed, channel = 30) * 2f - 1f
                val uy = ValueNoise.hash01(dartIndex, seed, channel = 31) * 2f - 1f
                gaze.setTarget(ux * DART_RANGE_X, uy * DART_RANGE_Y)
                dartIndex++
                val jitter = 0.5f + ValueNoise.hash01(dartIndex, seed, channel = 32)
                nextDartAt = time + dartInterval * jitter
            }
            if (dartInterval <= 0f) gaze.setTarget(0f, GAZE_ASLEEP_Y)
        }
        gaze.step(dt)
        pose.gazeX = gaze.x.value
        pose.gazeY = gaze.y.value
    }

    private fun stepFlourish() {
        val minute = (time / FLOURISH_PERIOD_SECONDS).toLong()
        if (minute > 0 && minute != flourishMinute && flourishStartedAt.isNaN() && mood == Mood.ALERT) {
            flourishMinute = minute
            flourishStartedAt = time
            pose.flourishKind = ValueNoise.hash01(minute, seed, channel = 40).let {
                (it * FLOURISH_KINDS).toInt().coerceIn(0, FLOURISH_KINDS - 1)
            }
        }
        if (!flourishStartedAt.isNaN()) {
            val phase = (time - flourishStartedAt) / FLOURISH_DURATION_SECONDS
            if (phase >= 1f) {
                flourishStartedAt = Float.NaN
                pose.flourishPhase = 0f
            } else {
                pose.flourishPhase = phase
            }
        }
    }

    private fun effectiveLid(): Float {
        var lid = lidEase.value
        if (petting) lid = maxOf(lid, PET_LID_SOFTNESS * petLean.value)
        if (mood == Mood.ASLEEP && rouseUntil >= time) lid = 0.5f
        return lid.coerceIn(0f, 1f)
    }

    /** Reduced motion: calm statics; only rare blinks move, at ~4 Hz updates. */
    private fun advanceReduced(dt: Float) {
        time += dt
        pose.breath = REDUCED_BREATH_STATIC
        pose.offsetX = 0f
        pose.offsetY = 0f
        pose.squash = 0f
        pose.tiltDeg = 0f
        pose.gazeX = 0f
        pose.gazeY = 0f
        pose.energy = profile.energy
        pose.flush = if (mood == Mood.HOT) 1f else 0f
        pose.lidDroop = profile.lidClosure
        pose.petLean = 0f
        pose.thinking = false
        stepBlink(dt)
    }

    private fun settleToStatics() {
        squash.snapTo(0f)
        tilt.snapTo(0f)
        petLean.snapTo(0f)
        gaze.snapTo(0f, 0f)
        chain.snapTo(0f, 0f)
        jitterBurst = 0f
        sighBoost = 0f
    }

    private companion object {
        const val TWO_PI = (Math.PI * 2).toFloat()
        const val MAX_FRAME_DELTA_MILLIS = 100f

        // Breath (motion bible; research.md §B2).
        const val INHALE_FRACTION = 0.6f
        const val SIGH_EVERY_CYCLES = 2L
        const val SIGH_BOOST = 0.6f
        const val SIGH_DECAY = 0.25f
        const val SWALLOW_BUMP = 0.08f
        const val BREATH_CHAIN_COUPLING = 0.35f

        // Blink.
        const val BLINK_MIN_SECONDS = 2f
        const val BLINK_MAX_SECONDS = 6f
        const val BLINK_CLOSE_SECONDS = 0.12f
        const val BLINK_OPEN_SECONDS = 0.18f
        const val BLINK_TOTAL_SECONDS = BLINK_CLOSE_SECONDS + BLINK_OPEN_SECONDS
        const val DOUBLE_BLINK_CHANCE = 0.10f
        const val DOUBLE_BLINK_GAP_SECONDS = 0.25f
        const val TYPING_BLINK_SLOWDOWN = 2.5f

        // Gaze.
        const val DART_STIFFNESS = 420f
        const val DART_RANGE_X = 0.6f
        const val DART_RANGE_Y = 0.35f
        const val GAZE_ASLEEP_Y = 0.2f

        // Body.
        const val WANDER_HZ = 0.23f
        const val TILT_NOISE_HZ = 0.11f
        const val TILT_RANGE_DEG = 4f
        const val JITTER_HZ = 11f
        const val JITTER_AMPLITUDE = 0.012f
        const val JITTER_DECAY = 0.8f
        const val TAP_IMPULSE = 2.6f
        const val STARTLE_IMPULSE = 3.4f
        const val CELEBRATE_IMPULSE = 3.0f
        const val THINK_SWAY_PERIOD = 1.8f
        const val THINK_SWAY_AMPLITUDE = 0.008f
        const val THINK_TILT_DEG = 2.2f
        const val PET_LID_SOFTNESS = 0.7f
        const val ROUSE_SECONDS = 4f

        // Flourish.
        const val FLOURISH_PERIOD_SECONDS = 60f
        const val FLOURISH_DURATION_SECONDS = 2.4f
        const val FLOURISH_KINDS = 3

        const val REDUCED_BREATH_STATIC = 0.35f

        fun asymmetricBreath(phase: Float): Float {
            return if (phase < INHALE_FRACTION) {
                smooth(phase / INHALE_FRACTION)
            } else {
                1f - smooth((phase - INHALE_FRACTION) / (1f - INHALE_FRACTION))
            }
        }

        fun blinkPhase(elapsed: Float): Float = when {
            elapsed < BLINK_CLOSE_SECONDS -> smooth(elapsed / BLINK_CLOSE_SECONDS)
            elapsed < BLINK_TOTAL_SECONDS -> 1f - smooth((elapsed - BLINK_CLOSE_SECONDS) / BLINK_OPEN_SECONDS)
            else -> 0f
        }

        fun smooth(x: Float): Float {
            val t = x.coerceIn(0f, 1f)
            return t * t * (3f - 2f * t)
        }
    }
}
