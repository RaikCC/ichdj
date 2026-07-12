package de.ichdj.jukebox.net

import de.ichdj.jukebox.model.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.net.URLEncoder

/** HTTP-Fehler der Spotify Web API mit Status-Code. */
class SpotifyApiException(val code: Int, message: String) : IOException(message)

class SpotifyApi(private val client: OkHttpClient) {

    private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }

    private suspend fun request(
        token: String,
        method: String,
        url: String,
        body: RequestBody? = null,
    ): String? = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer $token")
            .method(method, body)
            .build()
        client.newCall(request).execute().use { response ->
            when {
                response.code == 204 -> null
                response.isSuccessful -> response.body?.string()
                else -> throw SpotifyApiException(
                    response.code,
                    response.body?.string()?.take(500) ?: response.message,
                )
            }
        }
    }

    /** Aktueller Wiedergabestatus; null wenn nichts aktiv ist (HTTP 204). */
    suspend fun getPlayback(token: String): PlaybackDto? =
        request(token, "GET", "https://api.spotify.com/v1/me/player")
            ?.let { json.decodeFromString<PlaybackDto>(it) }

    suspend fun getQueue(token: String): QueueDto =
        request(token, "GET", "https://api.spotify.com/v1/me/player/queue")
            ?.let { json.decodeFromString<QueueDto>(it) } ?: QueueDto()

    /**
     * Hängt einen Track an die manuelle Queue an. Spotify spielt manuell
     * eingereihte Tracks vor der Playlist-Fortsetzung – Wünsche landen also
     * hinter bereits abgegebenen Wünschen, aber vor den normalen Songs.
     */
    suspend fun addToQueue(token: String, uri: String) {
        val encoded = URLEncoder.encode(uri, "UTF-8")
        request(
            token, "POST",
            "https://api.spotify.com/v1/me/player/queue?uri=$encoded",
            ByteArray(0).toRequestBody(null),
        )
    }

    suspend fun search(token: String, query: String, limit: Int = 20): List<Track> {
        if (query.isBlank()) return emptyList()
        val q = URLEncoder.encode(query, "UTF-8")
        val raw = request(
            token, "GET",
            "https://api.spotify.com/v1/search?type=track&limit=$limit&q=$q",
        ) ?: return emptyList()
        return json.decodeFromString<SearchDto>(raw)
            .tracks?.items?.mapNotNull { it.toTrack() } ?: emptyList()
    }

    suspend fun getProfile(token: String): ProfileDto? =
        request(token, "GET", "https://api.spotify.com/v1/me")
            ?.let { json.decodeFromString<ProfileDto>(it) }

    suspend fun getProfileName(token: String): String? = getProfile(token)?.displayName

    /** Sucht eine eigene Playlist mit exakt diesem Namen (max. ~200 Playlists). */
    suspend fun findPlaylistByName(token: String, name: String): String? {
        var url: String? = "https://api.spotify.com/v1/me/playlists?limit=50"
        repeat(4) {
            val raw = request(token, "GET", url ?: return null) ?: return null
            val page = json.decodeFromString<PlaylistPageDto>(raw)
            page.items.firstOrNull { it.name == name }?.id?.let { return it }
            url = page.next ?: return null
        }
        return null
    }

    suspend fun createPlaylist(
        token: String,
        userId: String,
        name: String,
        description: String,
    ): String? {
        val body = json.encodeToString(CreatePlaylistRequest(name = name, description = description))
            .toRequestBody("application/json".toMediaType())
        val raw = request(
            token, "POST",
            "https://api.spotify.com/v1/users/${URLEncoder.encode(userId, "UTF-8")}/playlists",
            body,
        ) ?: return null
        return json.decodeFromString<PlaylistDto>(raw).id
    }

    suspend fun addToPlaylist(token: String, playlistId: String, uri: String) {
        val encoded = URLEncoder.encode(uri, "UTF-8")
        request(
            token, "POST",
            "https://api.spotify.com/v1/playlists/$playlistId/tracks?uris=$encoded",
            ByteArray(0).toRequestBody(null),
        )
    }
}
