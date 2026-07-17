package app.anima.core.creature.engine

import kotlin.math.min

/**
 * Damped spring integrated with semi-implicit Euler at a fixed substep.
 * Pure math — no Android imports (hermes-lens pure-engine rule). Springs are
 * velocity-preserving and interruptible: retargeting mid-flight keeps motion
 * continuous.
 */
class SpringF(
    var stiffness: Float,
    var damping: Float,
    value: Float = 0f,
) {
    var value: Float = value
        private set
    var velocity: Float = 0f
        private set
    var target: Float = value

    fun snapTo(newValue: Float) {
        value = newValue
        target = newValue
        velocity = 0f
    }

    fun impulse(velocityDelta: Float) {
        velocity += velocityDelta
    }

    /** One fixed substep (dt in seconds, already clamped by the caller). */
    fun step(dt: Float) {
        val force = (target - value) * stiffness - velocity * damping
        velocity += force * dt
        value += velocity * dt
    }

    /** True when close enough to rest to stop feeding the spring system. */
    fun isSettled(epsilon: Float = 0.0005f): Boolean =
        kotlin.math.abs(value - target) < epsilon && kotlin.math.abs(velocity) < epsilon
}

/** 2D spring built from two independent axes. */
class Spring2(
    stiffness: Float,
    damping: Float,
) {
    val x = SpringF(stiffness, damping)
    val y = SpringF(stiffness, damping)

    fun setTarget(tx: Float, ty: Float) {
        x.target = tx
        y.target = ty
    }

    fun snapTo(vx: Float, vy: Float) {
        x.snapTo(vx)
        y.snapTo(vy)
    }

    fun impulse(ix: Float, iy: Float) {
        x.impulse(ix)
        y.impulse(iy)
    }

    fun step(dt: Float) {
        x.step(dt)
        y.step(dt)
    }

    fun isSettled(): Boolean = x.isSettled() && y.isSettled()
}

/**
 * A chain of spring links for secondary motion (tentacles, ears, leaves,
 * wings): link N targets link N-1's current value, with stiffness easing off
 * down the chain so the tip lags and overshoots — follow-through for free.
 */
class SpringChain(
    val links: Int,
    baseStiffness: Float,
    baseDamping: Float,
    falloff: Float = 0.72f,
) {
    val chain: List<Spring2> = List(links) { index ->
        val factor = 1f + index * (1f - falloff)
        Spring2(baseStiffness / factor, baseDamping / (1f + index * 0.12f))
    }

    /** Drives the head; the rest follow their predecessor. */
    fun step(headX: Float, headY: Float, dt: Float) {
        var tx = headX
        var ty = headY
        for (link in chain) {
            link.setTarget(tx, ty)
            link.step(dt)
            tx = link.x.value
            ty = link.y.value
        }
    }

    fun snapTo(headX: Float, headY: Float) {
        chain.forEach { it.snapTo(headX, headY) }
    }
}

/** Shared substep constants (hermes-lens discipline). */
object Substep {
    const val MILLIS = 16.7f
    const val MAX_STEPS = 4

    /** Number of fixed substeps for an elapsed wall-clock delta. */
    fun count(dtMillis: Float): Int =
        min(MAX_STEPS, kotlin.math.max(1, (dtMillis / MILLIS + 0.5f).toInt()))
}
