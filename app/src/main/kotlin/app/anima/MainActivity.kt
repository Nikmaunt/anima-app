package app.anima

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    /**
     * QS tile / shortcut actions (ADR-015). A Channel, not a SharedFlow:
     * buffered until the NavHost collector exists (cold start from the
     * tile) and delivered exactly once (no replay re-navigation on
     * configuration change).
     */
    private val actions = Channel<String>(Channel.BUFFERED)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        intent?.action?.let(actions::trySend)
        setContent {
            AnimaRoot(actions = actions.receiveAsFlow())
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        intent.action?.let(actions::trySend)
    }
}
