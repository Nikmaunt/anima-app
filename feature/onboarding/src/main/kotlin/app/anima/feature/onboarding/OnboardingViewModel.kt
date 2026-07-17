package app.anima.feature.onboarding

import android.annotation.SuppressLint
import android.content.Context
import android.provider.Settings
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.anima.core.data.prefs.AnimaPrefs
import app.anima.core.data.repo.IdentityRepository
import app.anima.core.data.repo.JournalRepository
import app.anima.core.model.CreatureConcept
import app.anima.core.model.JournalKind
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * v0.3 quick-win (product-research §2): three screens to the first
 * conversation. The honest contract rides the NAME screen in three lines;
 * the full story lives on the always-available trust page in Settings.
 */
enum class OnboardingStage { HATCH, CHOOSE, NAME }

data class OnboardingUiState(
    val stage: OnboardingStage = OnboardingStage.HATCH,
    val seed: Long = 0L,
    val concept: CreatureConcept? = null,
    val name: String = "",
)

@HiltViewModel
class OnboardingViewModel
    @Inject
    constructor(
        @ApplicationContext context: Context,
        private val identity: IdentityRepository,
        private val journal: JournalRepository,
        private val prefs: AnimaPrefs,
    ) : ViewModel() {
        private val state = MutableStateFlow(OnboardingUiState(seed = deviceSeed(context)))
        val uiState: StateFlow<OnboardingUiState> = state.asStateFlow()

        fun onHatched() {
            state.value = state.value.copy(stage = OnboardingStage.CHOOSE)
        }

        fun onConceptChosen(concept: CreatureConcept) {
            state.value = state.value.copy(concept = concept)
        }

        fun onConceptConfirmed() {
            if (state.value.concept != null) state.value = state.value.copy(stage = OnboardingStage.NAME)
        }

        fun onNameChanged(name: String) {
            state.value = state.value.copy(name = name.take(24))
        }

        fun complete(onDone: () -> Unit) {
            val snapshot = state.value
            val concept = snapshot.concept ?: return
            if (snapshot.name.isBlank()) return
            viewModelScope.launch {
                val now = System.currentTimeMillis()
                identity.hatch(snapshot.name, concept, snapshot.seed, now)
                journal.record(JournalKind.HATCHED, now, "hatched as ${concept.wire}")
                prefs.setOnboardingDone()
                onDone()
            }
        }

        /**
         * Every phone hatches its own creature: seed = FNV-1a of ANDROID_ID.
         * ANDROID_ID is per-app-signing scoped and needs no permission; a null or
         * flaky value degrades to a fixed seed rather than a crash.
         */
        @SuppressLint("HardwareIds")
        private fun deviceSeed(context: Context): Long {
            val id =
                runCatching {
                    Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
                }.getOrNull() ?: "anima-fallback"
            var hash = -0x340d631b7bdddcdbL
            for (ch in id) {
                hash = hash xor ch.code.toLong()
                hash *= 0x100000001b3L
            }
            return hash
        }
    }
