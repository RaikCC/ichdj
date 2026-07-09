package de.ichdj.jukebox.model

import kotlinx.serialization.Serializable

/**
 * Ein abgegebener Musikwunsch. [startedPlaying] merkt sich, ob der Track im
 * Master schon einmal der aktuell spielende war – nur dann gilt der Wunsch
 * beim Verschwinden aus der Queue als "gespielt" (sonst wurde er vom
 * Veranstalter entfernt).
 */
@Serializable
data class Wish(
    val track: Track,
    val wishedAtMillis: Long,
    val startedPlaying: Boolean = false,
)
