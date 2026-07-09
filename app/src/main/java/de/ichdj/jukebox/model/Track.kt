package de.ichdj.jukebox.model

import kotlinx.serialization.Serializable

/** Ein Spotify-Track, reduziert auf das, was die App braucht. */
@Serializable
data class Track(
    val uri: String,
    val id: String,
    val name: String,
    val artists: String,
    val durationMs: Long,
    val coverUrl: String? = null,
)
