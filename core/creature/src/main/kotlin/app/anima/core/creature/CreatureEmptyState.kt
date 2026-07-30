package app.anima.core.creature

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
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
 *
 * v1.1b task 1c: [concept] and [seed] are nullable, and null means the identity
 * row has not been read yet. The rig then keeps its 150dp of height and draws
 * nothing in it. Every caller used to pass a `?: SPIRIT_ORB` here, i.e. every
 * empty state in the app could show a body belonging to no phone; the fallback
 * is removed at this end so the callers cannot reintroduce it one at a time.
 */
@Composable
fun CreatureEmptyState(
    concept: CreatureConcept?,
    seed: Long?,
    line: String,
    modifier: Modifier = Modifier,
    night: Boolean = true,
) {
    Column(modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        if (concept != null && seed != null) {
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
        } else {
            Spacer(
                Modifier
                    .fillMaxWidth()
                    .height(150.dp),
            )
        }
        Text(
            line,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 6.dp),
        )
    }
}
