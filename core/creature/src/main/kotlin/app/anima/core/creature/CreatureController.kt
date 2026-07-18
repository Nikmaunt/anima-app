package app.anima.core.creature

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import app.anima.core.creature.engine.CreatureEngine
import app.anima.core.model.BodyState
import app.anima.core.model.CreatureConcept
import app.anima.core.model.CreatureGenome
import app.anima.core.model.Evolution
import app.anima.core.model.LifeStage

/**
 * The one handle a screen holds on its creature. Wraps the engine so feature
 * code never touches oscillator internals; all methods are main-thread calls
 * from composition effects or gesture handlers.
 */
class CreatureController(
    val concept: CreatureConcept,
    val seed: Long,
    val genome: CreatureGenome,
) {
    internal val engine = CreatureEngine(seed, genome)

    /** v0.4 milestones: unlocked palette rotation; 0 = true self. */
    var paletteShiftDeg: Float = 0f

    internal var batteryPercent: Int = 80
        private set
    internal var charging: Boolean = false
        private set
    internal var growth: Float = 0.3f
        private set
    internal var stageScale: Float = 1f
        private set

    /** Evolution (v0.2): life stage scales the whole body on top of the genome. */
    fun setStage(stage: LifeStage) {
        stageScale = Evolution.sizeScaleOf(stage)
    }

    fun setBodyState(state: BodyState) {
        engine.setMood(state.mood)
        batteryPercent = state.signals.batteryPercent
        charging = state.signals.charging
    }

    /** 0..1 — accumulated soul (drives the sprout, subtle glow elsewhere). */
    fun setGrowth(value: Float) {
        growth = value.coerceIn(0f, 1f)
    }

    fun onTyping(active: Boolean) = engine.onTyping(active)

    fun onThinking(active: Boolean) = engine.onThinking(active)

    fun onStartle() = engine.onStartle()

    fun onCelebrate() = engine.onCelebrate()
}

@Composable
fun rememberCreature(
    concept: CreatureConcept,
    seed: Long,
    genome: CreatureGenome = CreatureGenome.from(seed),
): CreatureController =
    remember(concept, seed, genome) {
        CreatureController(concept, seed, genome)
    }
