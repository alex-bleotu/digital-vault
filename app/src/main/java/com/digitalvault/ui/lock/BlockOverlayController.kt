package com.digitalvault.ui.lock

import android.content.Context
import android.graphics.PixelFormat
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.content.getSystemService
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.digitalvault.ui.theme.DigitalVaultTheme

private const val OVERLAY_TAG = "BlockOverlay"

class BlockOverlayController(private val context: Context) {

    private val windowManager = context.getSystemService<WindowManager>()

    private var overlayView: View? = null
    private var lifecycleOwner: OverlayLifecycleOwner? = null

    var kind: OverlayKind? = null
        private set

    val isShowing: Boolean
        get() = overlayView != null

    enum class OverlayKind { TIMEOUT_WALL, VAULT_PROMPT }

    fun showTimeoutWall(
        appLabel: String,
        appIcon: ImageBitmap?,
        dismissMinutes: Int,
        holdMillis: Int,
        allowBreak: Boolean,
        breakUsedToday: Boolean,
        onDismissForBreak: () -> Unit,
        onExit: () -> Unit,
    ) {
        val request = BlockOverlayRequest(
            appLabel = appLabel,
            appIcon = appIcon,
            dismissMinutes = dismissMinutes,
            holdMillis = holdMillis,
            reduceMotion = isReduceMotionEnabled(),
            allowBreak = allowBreak,
            breakUsedToday = breakUsedToday,
        )
        attach(OverlayKind.TIMEOUT_WALL, focusable = false) {
            BlockOverlayContent(request = request, onDismissForBreak = onDismissForBreak, onExit = onExit)
        }
    }

    fun showVaultPrompt(
        message: String,
        onVerify: suspend (String) -> Boolean,
        onSuccess: () -> Unit,
        onLeave: () -> Unit,
    ) {
        attach(OverlayKind.VAULT_PROMPT, focusable = true) {
            VaultPromptContent(
                message = message,
                onVerify = onVerify,
                onSuccess = onSuccess,
                onLeave = onLeave,
            )
        }
    }

    fun hide() {
        val manager = windowManager ?: return
        overlayView?.let { view ->
            runCatching { manager.removeView(view) }
                .onFailure { Log.w(OVERLAY_TAG, "Failed to remove overlay view", it) }
        }
        lifecycleOwner?.onDestroy()
        overlayView = null
        lifecycleOwner = null
        kind = null
    }

    private fun attach(
        overlayKind: OverlayKind,
        focusable: Boolean,
        content: @Composable () -> Unit,
    ) {
        if (isShowing) {
            return
        }
        if (!Settings.canDrawOverlays(context)) {
            Log.w(OVERLAY_TAG, "Overlay permission not granted; cannot show ${overlayKind.name}")

            return
        }
        val manager = windowManager ?: return

        val owner = OverlayLifecycleOwner().apply { onCreate() }
        val composeView = ComposeView(context).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setViewTreeLifecycleOwner(owner)
            setViewTreeViewModelStoreOwner(owner)
            setViewTreeSavedStateRegistryOwner(owner)
            setContent {
                DigitalVaultTheme {
                    content()
                }
            }
        }

        try {
            manager.addView(composeView, layoutParams(focusable))
            overlayView = composeView
            lifecycleOwner = owner
            kind = overlayKind
        } catch (error: WindowManager.BadTokenException) {
            Log.w(OVERLAY_TAG, "Failed to add overlay view", error)
            owner.onDestroy()
        }
    }

    private fun isReduceMotionEnabled(): Boolean {
        val scale = Settings.Global.getFloat(
            context.contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1f,
        )

        return scale == 0f
    }

    private fun layoutParams(focusable: Boolean): WindowManager.LayoutParams {
        var flags = WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        if (!focusable) {
            flags = flags or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
        }

        return WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            screenHeightPx() - gestureNavHeightPx(),
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            flags,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING
        }
    }

    private fun screenHeightPx(): Int = context.resources.displayMetrics.heightPixels

    private fun gestureNavHeightPx(): Int {
        val resourceId = context.resources.getIdentifier("navigation_bar_height", "dimen", "android")

        return if (resourceId > 0) context.resources.getDimensionPixelSize(resourceId) else 0
    }
}
