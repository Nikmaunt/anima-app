package app.anima.core.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.anima.core.ui.theme.AnimaMotion
import app.anima.core.ui.theme.AnimaRadius
import app.anima.core.ui.theme.AnimaSpacing
import app.anima.core.ui.theme.LocalAnimaColors

/**
 * Grouping by air, not by plate (docs/design-system-v11.md §3). A group is a
 * label plus its content, separated from the next group by `xl` — a gap four
 * times the inner spacing reads as a boundary on its own, and needs no border.
 *
 * This is the replacement for reaching for `SectionCard` every time something
 * needs to look like a block. Settings currently stacks eight identical
 * plates (docs/design/v11/before/13-settings-2.png); that is defect D7.
 */
@Composable
fun Group(
    label: String? = null,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(AnimaSpacing.s),
    ) {
        label?.let { SectionLabel(it) }
        content()
    }
}

/** The vertical rhythm between groups. Use this, not a hand-picked dp. */
@Composable
fun GroupColumn(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(AnimaSpacing.xl),
        content = content,
    )
}

/**
 * A hero number with its caption. The pair the type scale was rebuilt around:
 * 44sp Light over 12sp Medium, a 3.67x size contrast pulling in opposite
 * directions by weight (defect D8).
 *
 * `widthIn(min = …)` is not decoration — it is the fix for D1. A stat in a
 * row must never be squeezed thin enough to wrap its caption one letter per
 * line, which is exactly what "кормёжек" did on the Soul screen.
 */
@Composable
fun HeroStat(
    value: String,
    caption: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.widthIn(min = HERO_STAT_MIN_WIDTH)) {
        Text(value, style = MaterialTheme.typography.displayLarge, maxLines = 1)
        Text(caption.uppercase(), style = MaterialTheme.typography.labelMedium)
    }
}

/** Wide enough that the longest caption we ship keeps its word intact. */
val HERO_STAT_MIN_WIDTH = 104.dp

/**
 * A row of stats that wraps instead of crushing its last child. Four hero
 * stats do not fit one 1080px line at font_scale 1.3 — before v1.1 the
 * fourth one wrapped to a one-letter column instead of moving to line two.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StatRow(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(AnimaSpacing.l),
        verticalArrangement = Arrangement.spacedBy(AnimaSpacing.m),
        content = { content() },
    )
}

/**
 * A row of actions that wraps. Same defect class as StatRow: on the Soul
 * screen a bare `Row {}` of three buttons rendered "Открытка" as a vertical
 * column of single letters at the default font size.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ActionRow(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(AnimaSpacing.s),
        verticalArrangement = Arrangement.spacedBy(AnimaSpacing.xs),
        content = { content() },
    )
}

/**
 * A title and its supporting line, with a trailing control. The pattern the
 * Settings switches got wrong: the caption ran under the switch because
 * neither side had a weight.
 */
@Composable
fun LabeledControl(
    title: String,
    supporting: String?,
    modifier: Modifier = Modifier,
    trailing: @Composable () -> Unit,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AnimaSpacing.m),
    ) {
        Column(
            Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(AnimaSpacing.xs),
        ) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            supporting?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
        }
        trailing()
    }
}

/**
 * The one shape an empty state takes (docs/design-system-v11.md §4, from
 * NN/g's rule that an empty state reports status, teaches, and offers the
 * next step). Creature, then a line in its own voice, then at most one step.
 *
 * No decorative illustration: the creature *is* the illustration.
 * No button unless there is genuinely somewhere to go.
 */
@Composable
fun EmptyState(
    voice: String,
    modifier: Modifier = Modifier,
    creature: (@Composable () -> Unit)? = null,
    action: (@Composable () -> Unit)? = null,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(AnimaSpacing.l),
    ) {
        creature?.invoke()
        Text(
            voice,
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = AnimaSpacing.m),
        )
        action?.invoke()
    }
}

/**
 * A plate. Kept for exactly two jobs: a block that demands an action, and a
 * block holding someone else's content (an input, pasted text). At most two
 * per screen — see the norm in docs/design-system-v11.md §3.
 */
@Composable
fun Plate(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = LocalAnimaColors.current
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(AnimaRadius.l))
                .background(colors.surface)
                .border(1.dp, colors.outline.copy(alpha = 0.5f), RoundedCornerShape(AnimaRadius.l))
                .padding(AnimaSpacing.l),
        verticalArrangement = Arrangement.spacedBy(AnimaSpacing.s),
        content = content,
    )
}

/**
 * Press response (defect D11: nothing on this screen reacts to a finger).
 * Settles to 0.96 on `spatialFast` — the token M3 reserves for finger
 * response, damping 0.6 so it overshoots slightly on release.
 */
@Composable
fun Modifier.pressable(onClick: () -> Unit): Modifier {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) AnimaMotion.PRESS_SCALE else 1f,
        animationSpec = AnimaMotion.spatialFast(),
        label = "press",
    )
    return this
        .scale(scale)
        .pointerInput(onClick) {
            detectTapGestures(
                onPress = { offset ->
                    val press =
                        androidx.compose.foundation.interaction.PressInteraction
                            .Press(offset)
                    interaction.tryEmit(press)
                    tryAwaitRelease()
                    interaction.tryEmit(
                        androidx.compose.foundation.interaction.PressInteraction
                            .Release(press),
                    )
                },
                onTap = { onClick() },
            )
        }
}
