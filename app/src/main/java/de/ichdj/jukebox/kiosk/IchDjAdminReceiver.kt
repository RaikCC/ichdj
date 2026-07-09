package de.ichdj.jukebox.kiosk

import android.app.admin.DeviceAdminReceiver

/**
 * Device-Admin-Hook. Wird die App per adb als Device Owner gesetzt
 * (siehe README), kann sie sich selbst für den Lock-Task-Modus freischalten –
 * dann ist der Kiosk-Modus ohne System-Dialog und ohne Ausbruchsmöglichkeit
 * aktiv.
 */
class IchDjAdminReceiver : DeviceAdminReceiver()
