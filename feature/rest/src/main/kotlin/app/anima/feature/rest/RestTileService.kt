package app.anima.feature.rest

import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import app.anima.core.model.RestPhase
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * ADR-015: the QS tile is an ACTION — one tap opens the rest screen (and the
 * session continues there). Active-mode tile: this process runs only for
 * tile events; no polling, no background refresh. State shown is whatever
 * the in-process manager holds at bind time — honest and cheap.
 */
@AndroidEntryPoint
class RestTileService : TileService() {
    @Inject lateinit var manager: RestSessionManager

    override fun onStartListening() {
        qsTile?.apply {
            state = if (manager.phase.value is RestPhase.Running) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
            label = getString(R.string.rest_tile_label)
            subtitle =
                when (val p = manager.phase.value) {
                    is RestPhase.Running -> "${p.plannedMin} min"
                    is RestPhase.Waiting -> "waiting"
                    else -> ""
                }
            updateTile()
        }
    }

    override fun onClick() {
        val intent =
            Intent(ACTION_OPEN_REST).apply {
                setClassName(packageName, MAIN_ACTIVITY)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startActivityAndCollapse(
                PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE),
            )
        } else {
            @Suppress("DEPRECATION")
            startActivityAndCollapse(intent)
        }
    }

    companion object {
        const val ACTION_OPEN_REST = "app.anima.action.REST"
        private const val MAIN_ACTIVITY = "app.anima.MainActivity"
    }
}
