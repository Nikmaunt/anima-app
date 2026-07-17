package app.anima.core.creature.render

import androidx.compose.ui.graphics.drawscope.DrawScope
import app.anima.core.model.CreatureConcept

/**
 * One engine, eight bodies. A renderer is a pure draw function of the
 * RenderContext: no state beyond reusable Path holders, no allocation in the
 * hot path, no time sources of its own.
 *
 * The caller has already applied the shared body transform (wander offset,
 * tilt, volume-preserving squash, breath macro-scale); renderers draw a
 * creature centred at (size/2, size/2) with body radius ~ size*0.28.
 */
interface CreatureRenderer {
    fun DrawScope.render(ctx: RenderContext)
}

object Renderers {
    fun forConcept(concept: CreatureConcept): CreatureRenderer =
        when (concept) {
            CreatureConcept.SPIRIT_ORB -> SpiritOrbRenderer()
            CreatureConcept.FOX_KIT -> FoxKitRenderer()
            CreatureConcept.JELLY -> JellyRenderer()
            CreatureConcept.PIXEL_PET -> PixelPetRenderer()
            CreatureConcept.ROBOT -> RobotRenderer()
            CreatureConcept.SPROUT -> SproutRenderer()
            CreatureConcept.EMBER -> EmberRenderer()
            CreatureConcept.MOTH -> MothRenderer()
        }
}
