package app.anima.core.creature

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.anima.core.model.BodySignals
import app.anima.core.model.BodyState
import app.anima.core.model.CreatureConcept
import app.anima.core.model.Mood

/**
 * Design-review previews: every concept × key bodily states, on the night
 * background. Previews are static (reduced-motion path) — the pose is the
 * settled calm pose, which is exactly what reduced-motion users see.
 */
@Composable
private fun PreviewCell(concept: CreatureConcept, mood: Mood, seed: Long = 7L) {
    val controller = rememberCreature(concept, seed)
    controller.setBodyState(BodyState(BodySignals.Resting.copy(charging = mood == Mood.EATING), mood))
    Column {
        CreatureSurface(
            controller = controller,
            night = true,
            modifier = Modifier.size(120.dp),
            interactive = false,
            reducedMotionOverride = true,
        )
        Text(
            "${concept.wire} · ${mood.name.lowercase()}",
            color = Color(0xFFA8AEC6),
            fontSize = 9.sp,
        )
    }
}

@Composable
private fun ConceptRow(concept: CreatureConcept) {
    Row {
        PreviewCell(concept, Mood.ALERT)
        PreviewCell(concept, Mood.EATING)
        PreviewCell(concept, Mood.SLEEPY)
        PreviewCell(concept, Mood.ANXIOUS)
        PreviewCell(concept, Mood.ASLEEP)
        PreviewCell(concept, Mood.HOT)
        PreviewCell(concept, Mood.BORED)
    }
}

@Preview(widthDp = 900, heightDp = 300, backgroundColor = 0xFF0B0E1A, showBackground = true)
@Composable
private fun PreviewSpiritOrbStates() = Box(Modifier.background(Color(0xFF0B0E1A))) {
    ConceptRow(CreatureConcept.SPIRIT_ORB)
}

@Preview(widthDp = 900, heightDp = 300, backgroundColor = 0xFF0B0E1A, showBackground = true)
@Composable
private fun PreviewFoxKitStates() = Box(Modifier.background(Color(0xFF0B0E1A))) {
    ConceptRow(CreatureConcept.FOX_KIT)
}

@Preview(widthDp = 900, heightDp = 300, backgroundColor = 0xFF0B0E1A, showBackground = true)
@Composable
private fun PreviewJellyStates() = Box(Modifier.background(Color(0xFF0B0E1A))) {
    ConceptRow(CreatureConcept.JELLY)
}

@Preview(widthDp = 900, heightDp = 300, backgroundColor = 0xFF0B0E1A, showBackground = true)
@Composable
private fun PreviewPixelPetStates() = Box(Modifier.background(Color(0xFF0B0E1A))) {
    ConceptRow(CreatureConcept.PIXEL_PET)
}

@Preview(widthDp = 900, heightDp = 300, backgroundColor = 0xFF0B0E1A, showBackground = true)
@Composable
private fun PreviewRobotStates() = Box(Modifier.background(Color(0xFF0B0E1A))) {
    ConceptRow(CreatureConcept.ROBOT)
}

@Preview(widthDp = 900, heightDp = 300, backgroundColor = 0xFF0B0E1A, showBackground = true)
@Composable
private fun PreviewSproutStates() = Box(Modifier.background(Color(0xFF0B0E1A))) {
    ConceptRow(CreatureConcept.SPROUT)
}

@Preview(widthDp = 900, heightDp = 300, backgroundColor = 0xFF0B0E1A, showBackground = true)
@Composable
private fun PreviewEmberStates() = Box(Modifier.background(Color(0xFF0B0E1A))) {
    ConceptRow(CreatureConcept.EMBER)
}

@Preview(widthDp = 900, heightDp = 300, backgroundColor = 0xFF0B0E1A, showBackground = true)
@Composable
private fun PreviewMothStates() = Box(Modifier.background(Color(0xFF0B0E1A))) {
    ConceptRow(CreatureConcept.MOTH)
}

/** Seed variance: the same concept on eight different "phones". */
@Preview(widthDp = 900, heightDp = 160, backgroundColor = 0xFF0B0E1A, showBackground = true)
@Composable
private fun PreviewSeedVariance() = Box(Modifier.background(Color(0xFF0B0E1A))) {
    Row {
        for (s in 1L..8L) {
            val controller = rememberCreature(CreatureConcept.FOX_KIT, s * 1000003L)
            CreatureSurface(
                controller = controller,
                night = true,
                modifier = Modifier.size(110.dp),
                interactive = false,
                reducedMotionOverride = true,
            )
        }
    }
}
