package app.anima.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import app.anima.core.ui.theme.LocalAnimaColors

/** Primary action: a soft pill. One per screen, at most. */
@Composable
fun PillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val colors = LocalAnimaColors.current
    Button(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = RoundedCornerShape(50),
        colors =
            ButtonDefaults.buttonColors(
                containerColor = colors.accent,
                contentColor = colors.background,
            ),
        contentPadding =
            androidx.compose.foundation.layout.PaddingValues(
                horizontal = 28.dp,
                vertical = 14.dp,
            ),
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge)
    }
}

/**
 * Quiet secondary action.
 *
 * v1.1 adds [quiet]. A row of four identical accent links has no hierarchy —
 * it was defect D7 in miniature on Home, where "Отдых" and "Настройки" shouted
 * equally loudly despite being a daily thing and a chrome thing. `quiet` drops
 * the label to `textDim`, which keeps it a real target and a real affordance
 * while letting the accent mean "this is what you came for".
 */
@Composable
fun GhostButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    quiet: Boolean = false,
) {
    val colors = LocalAnimaColors.current
    TextButton(onClick = onClick, modifier = modifier, enabled = enabled) {
        Text(
            text,
            style = MaterialTheme.typography.labelLarge,
            color =
                when {
                    !enabled -> colors.textDim
                    quiet -> colors.textDim
                    else -> colors.accent
                },
        )
    }
}

/** Content card on the night sky; hairline border instead of elevation. */
@Composable
fun SectionCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = LocalAnimaColors.current
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.medium)
                .background(colors.surface)
                .border(1.dp, colors.outline.copy(alpha = 0.5f), MaterialTheme.shapes.medium)
                .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        content = content,
    )
}

/** Small all-caps section label. */
@Composable
fun SectionLabel(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        modifier = modifier,
    )
}
