package de.ichdj.jukebox.net

import de.ichdj.jukebox.model.Track
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.math.abs

@Serializable
data class PlaybackDto(
    val item: TrackDto? = null,
    @SerialName("progress_ms") val progressMs: Long? = null,
    @SerialName("is_playing") val isPlaying: Boolean = false,
    @SerialName("currently_playing_type") val currentlyPlayingType: String? = null,
)

@Serializable
data class QueueDto(
    @SerialName("currently_playing") val currentlyPlaying: TrackDto? = null,
    val queue: List<TrackDto> = emptyList(),
)

@Serializable
data class TrackDto(
    val uri: String? = null,
    val id: String? = null,
    val name: String? = null,
    @SerialName("duration_ms") val durationMs: Long? = null,
    val artists: List<ArtistDto> = emptyList(),
    val album: AlbumDto? = null,
    val type: String? = null,
) {
    fun toTrack(): Track? {
        val u = uri ?: return null
        val n = name ?: return null
        val cover = album?.images
            ?.filter { it.url != null }
            ?.minByOrNull { abs((it.width ?: 300) - 300) }
            ?.url
        return Track(
            uri = u,
            id = id ?: u.substringAfterLast(':'),
            name = n,
            artists = artists.mapNotNull { it.name }.joinToString(", "),
            durationMs = durationMs ?: 0,
            coverUrl = cover,
        )
    }
}

@Serializable
data class ArtistDto(val name: String? = null)

@Serializable
data class AlbumDto(val images: List<ImageDto> = emptyList())

@Serializable
data class ImageDto(val url: String? = null, val width: Int? = null, val height: Int? = null)

@Serializable
data class SearchDto(val tracks: TracksPageDto? = null)

@Serializable
data class TracksPageDto(val items: List<TrackDto> = emptyList())

@Serializable
data class ProfileDto(@SerialName("display_name") val displayName: String? = null)

@Serializable
data class TokenResponse(
    @SerialName("access_token") val accessToken: String,
    @SerialName("expires_in") val expiresIn: Long = 3600,
    @SerialName("refresh_token") val refreshToken: String? = null,
)
