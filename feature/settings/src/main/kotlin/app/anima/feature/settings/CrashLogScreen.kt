package app.anima.feature.settings

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import app.anima.core.ui.components.GhostButton
import app.anima.core.ui.components.SectionCard
import app.anima.core.ui.components.SectionLabel
import app.anima.core.ui.theme.LocalAnimaColors
import java.io.File

/** Where AnimaApp's handler writes the last crash. No telemetry — a file. */
object CrashLog {
    const val DIR = "crash"
    const val FILE = "last-crash.txt"

    fun file(context: Context): File = File(File(context.filesDir, DIR), FILE)
}

/**
 * Self-diagnosis without telemetry (Phase 3 security list): the last crash,
 * on screen, with a copy button — the user decides where it goes. Nothing
 * is ever sent anywhere by the app.
 */
@Composable
fun CrashLogScreen(onBack: () -> Unit) {
    val colors = LocalAnimaColors.current
    val context = LocalContext.current
    val text = remember { mutableStateOf(readCrash(context)) }

    Column(
        Modifier
            .fillMaxSize()
            .background(colors.background)
            .statusBarsPadding()
            .padding(horizontal = 20.dp)
            .verticalScroll(rememberScrollState()),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 8.dp)) {
            GhostButton("Back", onClick = onBack)
            Text(
                "Last crash",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(start = 8.dp),
            )
        }
        SectionCard {
            SectionLabel("For your eyes only")
            Text(
                "If Anima ever falls over, the details land here — on this " +
                    "phone, nowhere else. Copy them if you want to report a bug.",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        Spacer(Modifier.height(14.dp))
        SectionCard {
            val crash = text.value
            if (crash == null) {
                Text("No crashes recorded. Long may it last.", style = MaterialTheme.typography.bodyLarge)
            } else {
                Row {
                    GhostButton("Copy", onClick = { copy(context, crash) })
                    Spacer(Modifier.height(0.dp))
                    GhostButton("Clear", onClick = {
                        CrashLog.file(context).delete()
                        text.value = null
                    })
                }
                Text(crash, style = MaterialTheme.typography.bodySmall)
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

private fun readCrash(context: Context): String? =
    CrashLog
        .file(context)
        .takeIf { it.exists() }
        ?.readText()
        ?.take(MAX_SHOWN_CHARS)

private fun copy(
    context: Context,
    text: String,
) {
    val clipboard = context.getSystemService(ClipboardManager::class.java) ?: return
    clipboard.setPrimaryClip(ClipData.newPlainText("anima-crash", text))
}

private const val MAX_SHOWN_CHARS = 20_000
