package app.anima.core.creature

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.anima.core.model.BodyState
import app.anima.core.model.CreatureConcept

/**
 * v0.3 quick-win (product-research §3): no screen goes empty without the
 * creature in it. A small resting rig (the REAL engine, non-interactive)
 * plus one line in its voice.
 */
@Composable
fun CreatureEmptyState(
    concept: CreatureConcept,
    seed: Long,
    line: String,
    modifier: Modifier = Modifier,
    night: Boolean = true,
) {
    Column(modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        val controller = rememberCreature(concept, seed)
        controller.setBodyState(BodyState.Resting)
        CreatureSurface(
            controller = controller,
            night = night,
            interactive = false,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(150.dp),
        )
        Text(
            line,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 6.dp),
        )
    }
}
