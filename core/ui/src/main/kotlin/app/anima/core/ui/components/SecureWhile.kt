package app.anima.core.ui.components

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalView

/**
 * v0.6 (audit-v05 D1): one FLAG_SECURE implementation for every soul-bearing
 * surface. threat-model.md promises the flag on the memory list AND on time
 * capsules; v0.5 only delivered the former. While [active], the hosting
 * window refuses screenshots/recents thumbnails; the flag is dropped the
 * moment the surface leaves composition or [active] flips.
 */
@Composable
fun SecureWhile(active: Boolean) {
    val view = LocalView.current
    DisposableEffect(active) {
        val window = view.context.findActivity()?.window
        if (active) {
            window?.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
        onDispose {
            if (active) {
                window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
            }
        }
    }
}

private tailrec fun Context.findActivity(): Activity? =
    when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }
