package de.ichdj.jukebox.kiosk

import android.app.Activity
import android.app.ActivityManager
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

/**
 * Kapselt Immersive-Mode und Lock-Task ("App-Pinning"). Ist die App Device
 * Owner, ist das Pinning ein echter Kiosk-Modus ohne Ausbruchsmöglichkeit;
 * sonst greift das normale Android-Pinning (Back+Übersicht lange drücken
 * beendet es – für harte Anforderungen Device Owner setzen, siehe README).
 */
class KioskManager(private val activity: Activity) {

    private val dpm =
        activity.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
    private val activityManager =
        activity.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    private val adminComponent = ComponentName(activity, IchDjAdminReceiver::class.java)

    fun isDeviceOwner(): Boolean = dpm.isDeviceOwnerApp(activity.packageName)

    private fun isPinned(): Boolean =
        activityManager.lockTaskModeState != ActivityManager.LOCK_TASK_MODE_NONE

    fun enterKiosk() {
        hideSystemBars()
        try {
            if (isDeviceOwner()) {
                dpm.setLockTaskPackages(adminComponent, arrayOf(activity.packageName))
            }
            if (!isPinned()) activity.startLockTask()
        } catch (_: Exception) {
            // Pinning nicht möglich (z.B. Emulator) → wenigstens immersive bleiben
        }
    }

    fun exitKiosk() {
        unpin()
        showSystemBars()
    }

    /**
     * Hebt nur das Pinning auf (z.B. damit der Keyguard-Dialog der
     * Gerätesperre erscheinen kann), lässt die Systemleisten aber versteckt.
     */
    fun unpin() {
        try {
            if (isPinned()) activity.stopLockTask()
        } catch (_: Exception) {
        }
    }

    private fun insetsController(): WindowInsetsControllerCompat =
        WindowInsetsControllerCompat(activity.window, activity.window.decorView)

    private fun hideSystemBars() {
        WindowCompat.setDecorFitsSystemWindows(activity.window, false)
        insetsController().apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    private fun showSystemBars() {
        insetsController().show(WindowInsetsCompat.Type.systemBars())
    }
}
