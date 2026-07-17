package app.anima

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
import app.anima.core.ui.theme.AnimaTheme
import app.anima.core.ui.theme.LocalAnimaColors
import app.anima.feature.home.BodyDiaryScreen
import app.anima.feature.home.HomeScreen
import app.anima.feature.notifications.NotificationsScreen
import app.anima.feature.onboarding.OnboardingScreen
import app.anima.feature.settings.MindScreen
import app.anima.feature.settings.SettingsScreen
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
fun AnimaRoot(viewModel: RootViewModel = hiltViewModel()) {
    AnimaTheme {
        val done by viewModel.onboardingDone.collectAsState()
        val colors = LocalAnimaColors.current
        when (done) {
            null -> Box(Modifier.fillMaxSize().background(colors.background))
            else -> AnimaNavHost(startAtHome = done == true)
        }
    }
}

@Composable
private fun AnimaNavHost(startAtHome: Boolean) {
    val nav = rememberNavController()
    NavHost(
        navController = nav,
        startDestination = if (startAtHome) Routes.HOME else Routes.ONBOARDING,
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
            )
        }
        composable(Routes.DIARY) {
            BodyDiaryScreen(onBack = { nav.popBackStack() })
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(
                onBack = { nav.popBackStack() },
                onOpenNotifications = { nav.navigate(Routes.NOTIFICATIONS) },
                onOpenMind = { nav.navigate(Routes.MIND) },
            )
        }
        composable(Routes.MIND) {
            MindScreen(onBack = { nav.popBackStack() })
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

private object Routes {
    const val ONBOARDING = "onboarding"
    const val HOME = "home"
    const val SETTINGS = "settings"
    const val NOTIFICATIONS = "notifications"
    const val SOUL = "soul"
    const val MIND = "mind"
    const val STORY = "story"
    const val DIARY = "diary"
}
