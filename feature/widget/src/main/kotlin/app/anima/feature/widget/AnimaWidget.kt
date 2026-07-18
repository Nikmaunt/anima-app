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
import androidx.glance.layout.ContentScale
import androidx.glance.layout.fillMaxSize
import app.anima.core.data.repo.IdentityRepository
import app.anima.core.model.CreatureConcept
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
        val concept = runCatching { identity.concept() }.getOrNull() ?: CreatureConcept.SPIRIT_ORB
        val seed = runCatching { identity.seed() }.getOrNull() ?: 0L
        val name = runCatching { identity.name() }.getOrNull() ?: "Anima"
        // v0.4 milestones: the worn palette follows onto the home screen.
        val paletteShift =
            runCatching {
                val now = System.currentTimeMillis()
                val wire = entry.prefs().paletteVariant().first()
                app.anima.core.model.Milestones.effectiveShiftDeg(wire, identity.stats(now), now)
            }.getOrDefault(0f)

        val vitals = WidgetSnapshot.readVitals(context)
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val night = hour >= NIGHT_FROM || hour < NIGHT_UNTIL
        val mood = WidgetSnapshot.moodFor(vitals, night)
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

        val launch = context.packageManager.getLaunchIntentForPackage(context.packageName)
        provideContent {
            val base = GlanceModifier.fillMaxSize()
            Image(
                provider = ImageProvider(bitmap),
                contentDescription =
                    "$name is ${moodWord(mood)}, battery ${vitals.batteryPercent}%" +
                        if (vitals.charging) ", eating" else "",
                contentScale = ContentScale.Fit,
                modifier = if (launch != null) base.clickable(actionStartActivity(launch)) else base,
            )
        }
    }

    private fun moodWord(mood: app.anima.core.model.Mood): String =
        when (mood) {
            app.anima.core.model.Mood.EATING -> "eating"
            app.anima.core.model.Mood.ASLEEP -> "asleep"
            app.anima.core.model.Mood.SLEEPY -> "dozing"
            else -> "awake"
        }

    private companion object {
        const val NIGHT_FROM = 22
        const val NIGHT_UNTIL = 7
        const val DEFAULT_GROWTH = 0.5f
    }
}

class AnimaWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = AnimaWidget()
}
