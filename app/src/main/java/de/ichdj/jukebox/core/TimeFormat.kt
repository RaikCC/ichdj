package de.ichdj.jukebox.core

import java.time.Instant
import java.time.ZoneId
import java.util.Locale
import kotlin.math.ceil

object TimeFormat {

    /** Spieldauer als m:ss, z.B. "3:45". */
    fun duration(ms: Long): String {
        val totalSeconds = (ms / 1000).coerceAtLeast(0)
        return String.format(Locale.ROOT, "%d:%02d", totalSeconds / 60, totalSeconds % 60)
    }

    /**
     * Startzeit als "HH:mm", angebrochene Minuten abgerundet – wer zur
     * angezeigten Zeit da ist, hört sicher den Anfang.
     */
    fun clockFloor(epochMillis: Long): String {
        val t = Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault())
        return String.format(Locale.ROOT, "%02d:%02d", t.hour, t.minute)
    }

    /** Uhrzeit als "HH:mm", auf die nächste volle Minute aufgerundet (für Sperr-Hinweise). */
    fun clockCeil(epochMillis: Long): String {
        var t = Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault())
        if (t.second > 0 || t.nano > 0) t = t.plusMinutes(1)
        return String.format(Locale.ROOT, "%02d:%02d", t.hour, t.minute)
    }

    /** Restwartezeit menschenlesbar, z.B. "1 Std 12 Min" oder "12 Min". */
    fun remainingWait(ms: Long): String {
        val totalMinutes = ceil(ms / 60_000.0).toLong()
        return when {
            totalMinutes < 1 -> "unter 1 Min"
            totalMinutes < 60 -> "$totalMinutes Min"
            else -> "${totalMinutes / 60} Std ${totalMinutes % 60} Min"
        }
    }
}
