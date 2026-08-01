package app.anima.feature.onboarding

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.anima.core.data.identity.DeviceSeed
import app.anima.core.data.prefs.AnimaPrefs
import app.anima.core.data.repo.IdentityRepository
import app.anima.core.data.repo.JournalRepository
import app.anima.core.model.CreatureConcept
import app.anima.core.model.CreatureName
import app.anima.core.model.JournalKind
import app.anima.core.model.assignedTo
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * v1.1c task 5.4 — hatch, then the body and the name appear, then the widget.
 *
 * What this replaces: HATCH → **CHOOSE** (a gallery of all eight bodies) →
 * NAME (a text field). Both middle steps contradicted the product's one
 * promise. A gallery says the body is merchandise; a name field asks the owner
 * to invent a relationship before meeting one. The body has always been a
 * function of the phone — it just was not, in the code, until now.
 */
enum class OnboardingStage { HATCH, REVEAL, WIDGET }

data class OnboardingUiState(
    val stage: OnboardingStage = OnboardingStage.HATCH,
    /**
     * v1.1b task 1c: no default. It was `0L`, and although the view model has
     * always constructed this state with the real device seed, a default here
     * means a future second construction site can hatch seed 0 without the
     * compiler saying anything.
     *
     * v1.1c: nullable, because reading the seed is now a suspend call — the
     * fallback path may have to write a rolled seed to storage. Null means the
     * egg has not finished being read yet, and the egg is what is on screen.
     */
    val seed: Long? = null,
    val concept: CreatureConcept? = null,
    val name: String = "",
    /** False when the launcher refuses pinning (most third-party launchers). */
    val canPinWidget: Boolean = false,
)

@HiltViewModel
class OnboardingViewModel
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val deviceSeed: DeviceSeed,
        private val identity: IdentityRepository,
        private val journal: JournalRepository,
        private val prefs: AnimaPrefs,
    ) : ViewModel() {
        private val state = MutableStateFlow(OnboardingUiState())
        val uiState: StateFlow<OnboardingUiState> = state.asStateFlow()

        init {
            viewModelScope.launch {
                val seed = deviceSeed.value()
                state.value =
                    state.value.copy(
                        seed = seed,
                        // The two facts that arrive together, from the same number.
                        concept = CreatureConcept.assignedTo(seed),
                        name = CreatureName.forSeed(seed),
                        canPinWidget =
                            AppWidgetManager.getInstance(context)?.isRequestPinAppWidgetSupported == true,
                    )
            }
        }

        /**
         * The shell opens. Writes identity immediately — the creature exists
         * from this moment, and the remaining steps are introductions, not a
         * form the owner can fail to submit.
         */
        fun onHatched() {
            val snapshot = state.value
            val seed = snapshot.seed ?: return
            val concept = snapshot.concept ?: return
            viewModelScope.launch {
                val now = System.currentTimeMillis()
                identity.hatch(snapshot.name, concept, seed, now)
                identity.markIdentityAssigned()
                journal.record(JournalKind.HATCHED, now, "hatched as ${concept.wire}")
                state.value = state.value.copy(stage = OnboardingStage.REVEAL)
            }
        }

        fun onRevealAcknowledged() {
            state.value = state.value.copy(stage = OnboardingStage.WIDGET)
        }

        /**
         * Asks the launcher to place the widget. The system shows its own
         * confirmation; a refusal or an unsupported launcher is not an error and
         * the owner can always add it by hand later.
         */
        fun onPinWidgetRequested() {
            val manager = AppWidgetManager.getInstance(context) ?: return
            if (!manager.isRequestPinAppWidgetSupported) return
            runCatching {
                manager.requestPinAppWidget(
                    ComponentName(context, "app.anima.feature.widget.AnimaWidgetReceiver"),
                    null,
                    null,
                )
            }
        }

        fun onFinished(onDone: () -> Unit) {
            viewModelScope.launch {
                prefs.setOnboardingDone()
                onDone()
            }
        }
    }
