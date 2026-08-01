package app.anima

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import app.anima.core.data.prefs.AnimaPrefs
import app.anima.core.ui.theme.AnimaMotion
import app.anima.core.ui.theme.AnimaTheme
import app.anima.core.ui.theme.LocalAnimaColors
import app.anima.feature.home.BodyDiaryScreen
import app.anima.feature.home.ChatScreen
import app.anima.feature.home.HomeScreen
import app.anima.feature.notifications.NotificationsScreen
import app.anima.feature.onboarding.OnboardingScreen
import app.anima.feature.rest.RestScreen
import app.anima.feature.rest.RestTileService
import app.anima.feature.settings.CrashLogScreen
import app.anima.feature.settings.LicensesScreen
import app.anima.feature.settings.MindScreen
import app.anima.feature.settings.PassportScreen
import app.anima.feature.settings.SettingsScreen
import app.anima.feature.settings.TrustScreen
import app.anima.feature.settings.WardrobeScreen
import app.anima.feature.soul.SoulScreen
import app.anima.feature.soul.StoryScreen
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class RootViewModel
    @Inject
    constructor(
        prefs: AnimaPrefs,
    ) : ViewModel() {
        /** null = still reading; avoids flashing onboarding for a hatched creature. */
        val onboardingDone: StateFlow<Boolean?> =
            prefs
                .onboardingDone()
                .map { it as Boolean? }
                .stateIn(viewModelScope, SharingStarted.Eagerly, null)
    }

@Composable
fun AnimaRoot(
    actions: kotlinx.coroutines.flow.Flow<String> = kotlinx.coroutines.flow.emptyFlow(),
    viewModel: RootViewModel = hiltViewModel(),
) {
    AnimaTheme {
        val done by viewModel.onboardingDone.collectAsState()
        val colors = LocalAnimaColors.current
        when (done) {
            null -> Box(Modifier.fillMaxSize().background(colors.background))
            else -> {
                // Frozen at first composition: when onboarding completes it
                // NAVIGATES home; flipping startDestination here as well
                // would rebuild the graph and cross-fade two Home screens
                // at once (caught by DayInLifeTest's duplicate-tag failure).
                val startAtHome = androidx.compose.runtime.remember { done == true }
                AnimaNavHost(startAtHome = startAtHome, actions = actions)
            }
        }
    }
}

@Composable
private fun AnimaNavHost(
    startAtHome: Boolean,
    actions: kotlinx.coroutines.flow.Flow<String>,
) {
    val nav = rememberNavController()
    // QS tile / app shortcuts land here as intent actions (ADR-015). Only a
    // hatched creature can navigate; pre-onboarding actions are ignored.
    androidx.compose.runtime.LaunchedEffect(startAtHome) {
        if (!startAtHome) return@LaunchedEffect
        actions.collect { action ->
            when (action) {
                RestTileService.ACTION_OPEN_REST -> nav.navigate(Routes.REST)
                ACTION_OPEN_DIARY -> nav.navigate(Routes.DIARY)
                else -> Unit
            }
        }
    }
    // v1.1 (defect D11: nothing moved anywhere). Screens slide a short
    // distance and cross-fade rather than being swapped instantly. Spring
    // tokens are Material 3 Expressive's own numbers, transcribed in
    // AnimaMotion; the fade uses the effects token, which has damping 1.0 and
    // therefore never overshoots past full opacity.
    NavHost(
        navController = nav,
        startDestination = if (startAtHome) Routes.HOME else Routes.ONBOARDING,
        enterTransition = {
            slideInHorizontally(AnimaMotion.spatialDefaultOffset()) { it / NAV_SLIDE_FRACTION } +
                fadeIn(AnimaMotion.effectsDefault())
        },
        exitTransition = {
            slideOutHorizontally(AnimaMotion.spatialDefaultOffset()) { -it / NAV_SLIDE_FRACTION } +
                fadeOut(AnimaMotion.effectsDefault())
        },
        popEnterTransition = {
            slideInHorizontally(AnimaMotion.spatialDefaultOffset()) { -it / NAV_SLIDE_FRACTION } +
                fadeIn(AnimaMotion.effectsDefault())
        },
        popExitTransition = {
            slideOutHorizontally(AnimaMotion.spatialDefaultOffset()) { it / NAV_SLIDE_FRACTION } +
                fadeOut(AnimaMotion.effectsDefault())
        },
    ) {
        composable(Routes.ONBOARDING) {
            OnboardingScreen(onFinished = {
                nav.navigate(Routes.HOME) { popUpTo(Routes.ONBOARDING) { inclusive = true } }
            })
        }
        composable(Routes.HOME) {
            HomeScreen(
                onOpenSettings = { nav.navigate(Routes.SETTINGS) },
                onOpenSoul = { nav.navigate(Routes.SOUL) },
                onOpenDiary = { nav.navigate(Routes.DIARY) },
                onOpenRest = { nav.navigate(Routes.REST) },
            )
        }
        composable(Routes.DIARY) {
            BodyDiaryScreen(onBack = { nav.popBackStack() })
        }
        composable(Routes.REST) {
            RestScreen(onBack = { nav.popBackStack() })
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(
                onBack = { nav.popBackStack() },
                onOpenNotifications = { nav.navigate(Routes.NOTIFICATIONS) },
                onOpenMind = { nav.navigate(Routes.MIND) },
                onOpenTrust = { nav.navigate(Routes.TRUST) },
                onOpenCrashLog = { nav.navigate(Routes.CRASHLOG) },
                onOpenPassport = { nav.navigate(Routes.PASSPORT) },
                onOpenLicenses = { nav.navigate(Routes.LICENSES) },
                onOpenChat = { nav.navigate(Routes.CHAT) },
            )
        }
        composable(Routes.CHAT) {
            ChatScreen(
                onBack = { nav.popBackStack() },
                onOpenMind = { nav.navigate(Routes.MIND) },
            )
        }
        composable(Routes.LICENSES) {
            LicensesScreen(onBack = { nav.popBackStack() })
        }
        // v1.1c task 5.2: the passport replaced the eight-body grid in Settings.
        // Palettes are reachable only from inside it (task 5.3) — the body and
        // its shades are one subject, and Settings should not list them twice.
        composable(Routes.PASSPORT) {
            PassportScreen(
                onBack = { nav.popBackStack() },
                onOpenSoul = { nav.navigate(Routes.SOUL) },
                onOpenPalettes = { nav.navigate(Routes.WARDROBE) },
            )
        }
        composable(Routes.WARDROBE) {
            WardrobeScreen(onBack = { nav.popBackStack() })
        }
        composable(Routes.MIND) {
            MindScreen(onBack = { nav.popBackStack() })
        }
        composable(Routes.TRUST) {
            TrustScreen(onBack = { nav.popBackStack() })
        }
        composable(Routes.CRASHLOG) {
            CrashLogScreen(onBack = { nav.popBackStack() })
        }
        composable(Routes.NOTIFICATIONS) {
            NotificationsScreen(onBack = { nav.popBackStack() })
        }
        composable(Routes.SOUL) {
            SoulScreen(
                onBack = { nav.popBackStack() },
                onOpenStory = { nav.navigate(Routes.STORY) },
            )
        }
        composable(Routes.STORY) {
            StoryScreen(onBack = { nav.popBackStack() })
        }
    }
}

/** Shortcut action mirrored in res/xml/shortcuts.xml. */
const val ACTION_OPEN_DIARY = "app.anima.action.DIARY"

/** An eighth of the screen: enough to read as movement, not as a swipe. */
private const val NAV_SLIDE_FRACTION = 8

private object Routes {
    const val ONBOARDING = "onboarding"
    const val HOME = "home"
    const val REST = "rest"
    const val SETTINGS = "settings"
    const val NOTIFICATIONS = "notifications"
    const val SOUL = "soul"
    const val MIND = "mind"
    const val STORY = "story"
    const val DIARY = "diary"
    const val TRUST = "trust"
    const val CRASHLOG = "crashlog"
    const val WARDROBE = "wardrobe"
    const val PASSPORT = "passport"
    const val LICENSES = "licenses"

    /**
     * v1.1: chat has a route of its own instead of a slot on Home, and it
     * is only ever navigated to from Settings while the experimental toggle
     * is on. Nothing else in the graph points here.
     */
    const val CHAT = "chat"
}
