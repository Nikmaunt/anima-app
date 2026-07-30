package app.anima.feature.widget

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.Box
import androidx.glance.layout.ContentScale
import androidx.glance.layout.fillMaxSize
import app.anima.core.data.repo.IdentityRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.first
import java.util.Calendar

/**
 * ADR-007: the creature on the home screen as a STATIC snapshot. provideGlance
 * runs briefly (Glance session), renders one bitmap, and ends — no animation,
 * no inference, no network. Tap opens the app (launch intent, no custom
 * activity export).
 */
class AnimaWidget : GlanceAppWidget() {
    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface WidgetEntryPoint {
        fun identity(): IdentityRepository

        fun prefs(): app.anima.core.data.prefs.AnimaPrefs
    }

    override suspend fun provideGlance(
        context: Context,
        id: GlanceId,
    ) {
        val entry = EntryPointAccessors.fromApplication(context, WidgetEntryPoint::class.java)
        val identity = entry.identity()
        // v1.1b task 1c: the widget sits on the home screen next to the launcher
        // icons, where nobody inspects it — a substituted body here is believed
        // rather than reported. If the identity read fails, the widget draws
        // nothing at all (see the empty branch at the bottom of provideGlance).
        val concept = runCatching { identity.concept() }.getOrNull()
        val seed = runCatching { identity.seed() }.getOrNull()
        val name = runCatching { identity.name() }.getOrNull() ?: "Anima"
        // v0.4 milestones: the worn palette follows onto the home screen.
        val paletteShift =
            runCatching {
                val now = System.currentTimeMillis()
                val wire = entry.prefs().paletteVariant().first()
                app.anima.core.model.Milestones
                    .effectiveShiftDeg(wire, identity.stats(now), now)
            }.getOrDefault(0f)

        val vitals = WidgetSnapshot.readVitals(context)
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val night = hour >= NIGHT_FROM || hour < NIGHT_UNTIL
        val mood = WidgetSnapshot.moodFor(vitals, night)
        val launch = context.packageManager.getLaunchIntentForPackage(context.packageName)

        if (concept == null || seed == null) {
            // Empty, and still tappable so the owner can open the app and see
            // what is wrong. No text: the widget contract is "nothing to read"
            // (design-2026-07 §4.3), and a body is not invented to fill space.
            provideContent {
                val base = GlanceModifier.fillMaxSize()
                Box(if (launch != null) base.clickable(actionStartActivity(launch)) else base) {}
            }
            return
        }

        val bitmap =
            WidgetSnapshot.render(
                concept,
                seed,
                mood,
                vitals,
                night,
                growth = DEFAULT_GROWTH,
                paletteShiftDeg = paletteShift,
            )

        provideContent {
            val base = GlanceModifier.fillMaxSize()
            Image(
                provider = ImageProvider(bitmap),
                contentDescription =
                    context.getString(
                        R.string.widget_a11y_state,
                        name,
                        moodWord(context, mood),
                        vitals.batteryPercent,
                    ) + if (vitals.charging) context.getString(R.string.widget_a11y_eating_suffix) else "",
                contentScale = ContentScale.Fit,
                modifier = if (launch != null) base.clickable(actionStartActivity(launch)) else base,
            )
        }
    }

    private fun moodWord(
        context: Context,
        mood: app.anima.core.model.Mood,
    ): String =
        context.getString(
            when (mood) {
                app.anima.core.model.Mood.EATING -> R.string.widget_mood_eating
                app.anima.core.model.Mood.ASLEEP -> R.string.widget_mood_asleep
                app.anima.core.model.Mood.SLEEPY -> R.string.widget_mood_dozing
                else -> R.string.widget_mood_awake
            },
        )

    private companion object {
        const val NIGHT_FROM = 22
        const val NIGHT_UNTIL = 7
        const val DEFAULT_GROWTH = 0.5f
    }
}

class AnimaWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = AnimaWidget()
}
