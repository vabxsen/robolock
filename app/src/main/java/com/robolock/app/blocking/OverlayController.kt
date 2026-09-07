package com.robolock.app.blocking

import android.content.Context
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.os.Build
import android.provider.Settings
import android.view.Display
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import androidx.compose.runtime.Composable
import com.robolock.app.core.log.RLog

/**
 * Shows and removes the intervention screen.
 *
 * A window rather than an Activity, deliberately. From Android 10 onward a background app cannot
 * reliably start an Activity, and an app in this position is by definition in the background. A
 * `TYPE_APPLICATION_OVERLAY` window is not an activity launch, so it is not subject to that
 * restriction at all. (SYSTEM_ALERT_WINDOW does currently carry a background-launch exemption,
 * but it has been narrowed repeatedly and OEMs differ, so nothing here depends on it.)
 *
 * Rules this class enforces, because an overlay that misbehaves is worse than no overlay:
 *  - it never covers Settings or the package installer, so it cannot sit on top of a permission
 *    dialog the user is trying to answer;
 *  - it is always dismissible and never traps the user;
 *  - it does not imitate a system dialog and takes no text input;
 *  - it comes down the moment it stops being warranted.
 */
class OverlayController(private val context: Context) {

    private val windowManager =
        context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager

    private var host: OverlayHost? = null
    private var attached: View? = null

    val isShowing: Boolean get() = attached != null

    /**
     * Attempts to show the overlay.
     *
     * Returns false when it could not be shown — the caller then falls back to a notification
     * rather than silently doing nothing.
     */
    fun show(
        overPackage: String,
        onBackPressed: () -> Unit,
        content: @Composable () -> Unit,
    ): Boolean {
        if (isShowing) return true
        val wm = windowManager ?: return false

        if (!Settings.canDrawOverlays(context)) {
            RLog.w("Overlay permission absent; cannot show intervention")
            return false
        }
        if (overPackage in NEVER_COVER) {
            RLog.i("Suppressing overlay over $overPackage")
            return false
        }

        val overlayHost = OverlayHost(windowContext())
        val view = overlayHost.createView(content)

        view.isFocusableInTouchMode = true
        view.setOnKeyListener { _, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_UP) {
                onBackPressed()
                true
            } else {
                false
            }
        }

        return try {
            wm.addView(view, layoutParams())
            view.requestFocus()
            host = overlayHost
            attached = view
            true
        } catch (e: WindowManager.BadTokenException) {
            RLog.e("Overlay rejected by window manager", e)
            overlayHost.destroy()
            false
        } catch (e: SecurityException) {
            RLog.e("Overlay denied", e)
            overlayHost.destroy()
            false
        }
    }

    /** Removes the overlay. Idempotent, and safe if the window is already gone. */
    fun hide() {
        val view = attached ?: return
        attached = null
        try {
            windowManager?.removeViewImmediate(view)
        } catch (e: IllegalArgumentException) {
            // Already detached — the window can be torn down by the system underneath us.
            RLog.d("Overlay already removed: ${e.message}")
        }
        host?.destroy()
        host = null
    }

    /**
     * A window context on the correct display, so insets, density and configuration changes are
     * resolved against the display the overlay actually appears on.
     */
    private fun windowContext(): Context {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return context

        // A Service context has no display of its own — asking it for one throws — so the default
        // display is resolved through DisplayManager instead.
        val displayManager =
            context.getSystemService(Context.DISPLAY_SERVICE) as? DisplayManager ?: return context
        val display = displayManager.getDisplay(Display.DEFAULT_DISPLAY) ?: return context

        return context.createDisplayContext(display)
            .createWindowContext(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY, null)
    }

    private fun layoutParams() = WindowManager.LayoutParams(
        WindowManager.LayoutParams.MATCH_PARENT,
        WindowManager.LayoutParams.MATCH_PARENT,
        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
        // Focusable so the back gesture reaches us. Deliberately NOT touch-modal-exempt and not
        // watching outside touches: the overlay owns its own surface and nothing beyond it.
        WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
            WindowManager.LayoutParams.FLAG_LAYOUT_INSET_DECOR,
        PixelFormat.TRANSLUCENT,
    ).apply {
        gravity = Gravity.TOP or Gravity.START
        // No FLAG_SECURE and no input type: this window never accepts credentials.
    }

    companion object {
        /**
         * Packages the overlay must never appear over.
         *
         * Covering Settings could hide the very dialog the user needs in order to revoke our
         * permissions, which would make the overlay a trap.
         */
        val NEVER_COVER = setOf(
            "com.android.settings",
            "com.android.packageinstaller",
            "com.google.android.packageinstaller",
            "com.android.permissioncontroller",
            "com.google.android.permissioncontroller",
        )
    }
}
