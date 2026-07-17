package app.anima.core.creature

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.anima.core.model.BodyState
import app.anima.core.model.CreatureConcept

/**
 * The choosing gallery: live engine instances, not pictures. Cells animate
 * (each runs its own lifecycle-gated frame loop) but ignore gestures; the
 * whole cell is the pick target. Used by onboarding and Settings.
 */
@Composable
fun ConceptGallery(
    seed: Long,
    selected: CreatureConcept?,
    onSelect: (CreatureConcept) -> Unit,
    modifier: Modifier = Modifier,
    accent: Color,
    surface: Color,
    outline: Color,
    cellHeight: Dp = 148.dp,
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(CreatureConcept.entries, key = { it.wire }) { concept ->
            val isSelected = concept == selected
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(cellHeight)
                    .clip(RoundedCornerShape(20.dp))
                    .background(surface)
                    .border(
                        width = if (isSelected) 2.dp else 1.dp,
                        color = if (isSelected) accent else outline.copy(alpha = 0.4f),
                        shape = RoundedCornerShape(20.dp),
                    )
                    .clickable { onSelect(concept) }
                    .padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                val controller = rememberCreature(concept, seed)
                controller.setBodyState(BodyState.Resting)
                CreatureSurface(
                    controller = controller,
                    night = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1.25f),
                    interactive = false,
                )
                Text(
                    conceptName(concept),
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
    }
}

fun conceptName(concept: CreatureConcept): String = when (concept) {
    CreatureConcept.SPIRIT_ORB -> "Spirit orb"
    CreatureConcept.FOX_KIT -> "Fox kit"
    CreatureConcept.JELLY -> "Jelly"
    CreatureConcept.PIXEL_PET -> "Pixel pet"
    CreatureConcept.ROBOT -> "Robot"
    CreatureConcept.SPROUT -> "Sprout"
    CreatureConcept.EMBER -> "Ember"
    CreatureConcept.MOTH -> "Moth"
}
