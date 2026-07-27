package app.anima

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.anima.core.ui.components.ActionRow
import app.anima.core.ui.components.EmptyState
import app.anima.core.ui.components.GhostButton
import app.anima.core.ui.components.Group
import app.anima.core.ui.components.GroupColumn
import app.anima.core.ui.components.HeroStat
import app.anima.core.ui.components.LabeledControl
import app.anima.core.ui.components.PillButton
import app.anima.core.ui.components.Plate
import app.anima.core.ui.components.StatRow
import app.anima.core.ui.components.enterStaggered
import app.anima.core.ui.theme.AnimaSpacing
import app.anima.core.ui.theme.AnimaTheme
import app.anima.core.ui.theme.LocalAnimaColors
import app.anima.core.ui.theme.Season
import app.anima.core.ui.theme.SignatureHue

/**
 * Debug-only. Renders the v1.1 design system on a real device so it can be
 * screenshotted and *looked at* rather than reasoned about
 * (docs/design-system-v11.md). Launch:
 *
 *   adb shell am start -n app.anima/app.anima.DesignCatalogActivity
 *
 * Optional extras: `--ez dark false`, `--ei hue 280`.
 */
class DesignCatalogActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val dark = intent.getBooleanExtra("dark", true)
        val hue = intent.getIntExtra("hue", -1)
        setContent {
            AnimaTheme(
                darkTheme = dark,
                seasonOverride = Season.SPRING,
                creatureHueDeg = if (hue >= 0) hue.toFloat() else null,
            ) {
                Catalog(night = dark)
            }
        }
    }
}

@Composable
private fun Catalog(night: Boolean) {
    val colors = LocalAnimaColors.current
    Box(
        Modifier
            .fillMaxSize()
            .background(colors.background)
            // Insets go on the viewport, not inside the scroll: put
            // statusBarsPadding() below verticalScroll and the content pads
            // once at scroll position 0 and slides under the clock after
            // that. Caught by looking at catalog-dark-2.png.
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = AnimaSpacing.m, vertical = AnimaSpacing.l),
        ) {
            GroupColumn {
                Text(
                    "Каталог v1.1",
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.enterStaggered(0),
                )

                Group("Типографика", Modifier.enterStaggered(1)) {
                    Text("44 / Light", style = MaterialTheme.typography.displayLarge)
                    Text("32 / Light — состояние", style = MaterialTheme.typography.displayMedium)
                    Text("24 — заголовок экрана", style = MaterialTheme.typography.headlineMedium)
                    Text("19 — так говорит существо", style = MaterialTheme.typography.headlineSmall)
                    Text("16 — обычный текст, ровный и спокойный.", style = MaterialTheme.typography.bodyLarge)
                    Text("15 — пояснение вполголоса.", style = MaterialTheme.typography.bodyMedium)
                    Text("12 — ПОДПИСЬ", style = MaterialTheme.typography.labelMedium)
                }

                Group("Герой и подпись — 3.67×", Modifier.enterStaggered(2)) {
                    StatRow {
                        HeroStat("1", "день")
                        HeroStat("0", "разговоров")
                        HeroStat("0", "воспоминаний")
                        HeroStat("0", "кормёжек")
                    }
                }

                Group("Действия", Modifier.enterStaggered(3)) {
                    ActionRow {
                        PillButton("Экспорт души", onClick = {})
                        GhostButton("Наша история…", onClick = {})
                        GhostButton("Открытка", onClick = {})
                    }
                }

                Group("Группировка воздухом", Modifier.enterStaggered(4)) {
                    Text(
                        "Между группами — 32dp, внутри — 8dp. Разрыв вчетверо " +
                            "больше внутреннего сам читается как граница, и рамка " +
                            "для этого не нужна.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    LabeledControl(
                        title = "Заголовок и подпись",
                        supporting = "Подпись больше не уезжает под элемент справа: у неё есть weight.",
                    ) {
                        Box(
                            Modifier
                                .size(52.dp, 30.dp)
                                .clip(RoundedCornerShape(50))
                                .background(colors.accent),
                        )
                    }
                }

                Group("Плашка — максимум две на экран", Modifier.enterStaggered(5)) {
                    Plate {
                        Text("Разум спит.", style = MaterialTheme.typography.bodyLarge)
                        Text(
                            "Плашка остаётся только там, где есть действие или чужой ввод.",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        PillButton("Принести разум", onClick = {})
                    }
                }

                Group("Пустое состояние", Modifier.enterStaggered(6)) {
                    EmptyState(
                        voice = "«Моя память пока — открытое поле. Расскажи мне что-нибудь.»",
                        creature = {
                            Box(
                                Modifier
                                    .size(140.dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(colors.accentSoft),
                            )
                        },
                    )
                }

                Group("Палитра существа", Modifier.enterStaggered(7)) {
                    Text(
                        "Тон один и тот же у фона и акцента — он приходит из генома.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(AnimaSpacing.s)) {
                        listOf(
                            colors.background to "фон",
                            colors.surface to "плашка",
                            colors.surfaceHigh to "выше",
                            colors.accent to "акцент",
                            colors.accentSoft to "мягкий",
                        ).forEach { (c, name) ->
                            Column(
                                Modifier.width(72.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Swatch(c, 56.dp)
                                Text(name, style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }

                Group("Все восемь тел — акцент каждого", Modifier.enterStaggered(8)) {
                    listOf(
                        "SPIRIT_ORB" to "Дух-огонёк",
                        "FOX_KIT" to "Лисёнок",
                        "JELLY" to "Желешка",
                        "PIXEL_PET" to "Пиксельный зверёк",
                        "ROBOT" to "Робот",
                        "SPROUT" to "Росток",
                        "EMBER" to "Уголёк",
                        "MOTH" to "Мотылёк",
                    ).forEach { (wire, label) ->
                        val h = SignatureHue.of(wire)
                        Row(
                            Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(AnimaSpacing.s),
                        ) {
                            Swatch(SignatureHue.background(h, night))
                            Swatch(SignatureHue.surface(h, night))
                            Swatch(SignatureHue.accent(h, night))
                            Text(
                                label,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f),
                            )
                            Text("${h.toInt()}°", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }

                Box(Modifier.height(AnimaSpacing.xxl))
            }
        }
    }
}

/**
 * A hairline is not decoration here: the background swatch is by definition
 * the same colour as the page, so without an outline it reads as a missing
 * swatch rather than as the point being made.
 */
@Composable
private fun Swatch(
    color: Color,
    size: Dp = 34.dp,
) {
    val colors = LocalAnimaColors.current
    Box(
        Modifier
            .size(size)
            .clip(RoundedCornerShape(AnimaSpacing.xs))
            .background(color)
            .border(1.dp, colors.outline, RoundedCornerShape(AnimaSpacing.xs)),
    )
}
