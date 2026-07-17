package app.anima.core.creature.engine

/**
 * The rig output every concept renders from. A single mutable holder reused
 * across frames — zero per-frame allocation by design. All values are
 * normalized; renderers scale to their own canvas.
 */
class CreaturePose {
    /** 0 = exhaled, 1 = full inhale. Asymmetric (inhale slower than exhale). */
    var breath: Float = 0f

    /** 0 = open, 1 = closed, per eye (left/right can differ: half-rouse). */
    var blinkLeft: Float = 0f
    var blinkRight: Float = 0f

    /** Additional lid droop from mood (0..1); renderers compose with blink. */
    var lidDroop: Float = 0f

    /** Gaze direction, -1..1 each axis (0,0 = straight ahead). */
    var gazeX: Float = 0f
    var gazeY: Float = 0f

    /** Body wander offset as a fraction of body size. */
    var offsetX: Float = 0f
    var offsetY: Float = 0f

    /** Volume-preserving squash: scaleY = 1 + squash, scaleX = 1/(1+squash). */
    var squash: Float = 0f

    /** Body tilt in degrees. */
    var tiltDeg: Float = 0f

    /** Mood master gain (from MoodProfile, eased between moods). */
    var energy: Float = 1f

    /** 0..1 flush amount (HOT), drives palette shifts in renderers. */
    var flush: Float = 0f

    /** 0..1 how much the creature leans toward the touch point while petted. */
    var petLean: Float = 0f

    /** True while the creature is "thinking with its body" (generation). */
    var thinking: Boolean = false

    /** Once-a-minute flourish: progress 0..1 (0 = none) and which flourish. */
    var flourishPhase: Float = 0f
    var flourishKind: Int = 0

    /**
     * Secondary-motion chain positions (tentacles/ears/leaves/wings), filled
     * by the engine's SpringChain; concepts interpret the links as they wish.
     * Fixed capacity, reused.
     */
    val secondaryX = FloatArray(MAX_CHAIN)
    val secondaryY = FloatArray(MAX_CHAIN)
    var secondaryCount: Int = 0

    companion object {
        const val MAX_CHAIN = 6
    }
}
