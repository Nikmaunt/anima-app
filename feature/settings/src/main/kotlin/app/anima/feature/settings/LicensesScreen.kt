package app.anima.feature.settings

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.anima.core.ui.components.GhostButton
import app.anima.core.ui.components.SectionCard
import app.anima.core.ui.theme.LocalAnimaColors
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import javax.inject.Inject

/**
 * v0.6 release engineering: the OSS attribution screen. Data comes from
 * `res/raw/aboutlibraries.json`, exported at BUILD time by the
 * AboutLibraries gradle plugin (`:app:exportLibraryDefinitions`) — no
 * runtime library, no network, no embedded browser; the screen is our own
 * list over the app's design system. The raw resource lives in :app, so it is
 * resolved by name at runtime rather than by a compile-time R reference.
 */
data class OssLibrary(
    val name: String,
    val version: String,
    val licenses: List<String>,
)

data class LicensesUiState(
    val libraries: List<OssLibrary> = emptyList(),
    /** license name → license text (or url as fallback). */
    val licenseTexts: Map<String, String> = emptyMap(),
)

@HiltViewModel
class LicensesViewModel
    @Inject
    constructor(
        @ApplicationContext private val appContext: Context,
    ) : ViewModel() {
        private val state = MutableStateFlow(LicensesUiState())
        val uiState: StateFlow<LicensesUiState> = state.asStateFlow()

        init {
            viewModelScope.launch(Dispatchers.IO) { state.value = load() }
        }

        private fun load(): LicensesUiState {
            val id =
                appContext.resources.getIdentifier(
                    "aboutlibraries",
                    "raw",
                    appContext.packageName,
                )
            if (id == 0) return LicensesUiState()
            val root =
                appContext.resources
                    .openRawResource(id)
                    .use { stream ->
                        Json.parseToJsonElement(stream.readBytes().decodeToString())
                    }.jsonObject
            val licenseById =
                root["licenses"]
                    ?.jsonObject
                    ?.mapValues { (_, v) ->
                        val obj = v.jsonObject
                        val name = obj["name"]?.jsonPrimitive?.content ?: "?"
                        val content =
                            obj["content"]?.jsonPrimitive?.content
                                ?: obj["url"]?.jsonPrimitive?.content.orEmpty()
                        name to content
                    }.orEmpty()
            val libraries =
                root["libraries"]
                    ?.jsonArray
                    .orEmpty()
                    .map { element ->
                        val obj = element.jsonObject
                        OssLibrary(
                            name =
                                obj["name"]?.jsonPrimitive?.content
                                    ?: obj["uniqueId"]?.jsonPrimitive?.content ?: "?",
                            version = obj["artifactVersion"]?.jsonPrimitive?.content.orEmpty(),
                            licenses =
                                obj["licenses"]?.jsonArray.orEmpty().mapNotNull { licId ->
                                    licenseById[licId.jsonPrimitive.content]?.first
                                },
                        )
                    }.sortedBy { it.name.lowercase() }
            return LicensesUiState(
                libraries = libraries,
                licenseTexts =
                    licenseById.values
                        .associate { (name, content) -> name to content },
            )
        }

        private fun kotlinx.serialization.json.JsonElement?.orEmpty() =
            this?.jsonArray ?: kotlinx.serialization.json.JsonArray(emptyList())
    }

@Composable
fun LicensesScreen(
    onBack: () -> Unit,
    viewModel: LicensesViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val colors = LocalAnimaColors.current
    var openLicense by remember { mutableStateOf<String?>(null) }

    Column(
        Modifier
            .fillMaxSize()
            .background(colors.background)
            .statusBarsPadding()
            .padding(horizontal = 20.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(vertical = 8.dp),
        ) {
            GhostButton(
                stringResource(R.string.settings_back),
                onClick = onBack,
                modifier = Modifier.testTag("licenses.back"),
            )
            Text(
                stringResource(R.string.licenses_title),
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(start = 8.dp),
            )
        }

        openLicense?.let { name ->
            // One license text at a time; back returns to the list.
            SectionCard {
                Text(
                    name,
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.text,
                )
                GhostButton(
                    stringResource(R.string.licenses_close_text),
                    onClick = { openLicense = null },
                    modifier = Modifier.testTag("licenses.closeText"),
                )
            }
            LazyColumn(modifier = Modifier.testTag("licenses.text")) {
                item {
                    Text(
                        state.licenseTexts[name].orEmpty(),
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textDim,
                        modifier = Modifier.padding(vertical = 12.dp),
                    )
                }
            }
            return@Column
        }

        Text(
            stringResource(R.string.licenses_intro, state.libraries.size),
            style = MaterialTheme.typography.bodyMedium,
            color = colors.textDim,
            modifier = Modifier.padding(bottom = 10.dp),
        )
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.testTag("licenses.list"),
        ) {
            items(state.libraries, key = { it.name + it.version }) { lib ->
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clickable(enabled = lib.licenses.isNotEmpty()) {
                            openLicense = lib.licenses.first()
                        }.padding(vertical = 6.dp),
                ) {
                    Text(
                        lib.name,
                        style = MaterialTheme.typography.bodyLarge,
                        color = colors.text,
                    )
                    Text(
                        listOf(lib.version, lib.licenses.joinToString())
                            .filter { it.isNotBlank() }
                            .joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textDim,
                    )
                }
            }
        }
    }
}
