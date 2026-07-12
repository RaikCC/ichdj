package de.ichdj.jukebox.engine

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import de.ichdj.jukebox.auth.SpotifyAuthManager
import de.ichdj.jukebox.model.Track
import de.ichdj.jukebox.net.SpotifyApi
import de.ichdj.jukebox.net.SpotifyApiException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.LocalDate

private val Context.wishLogDataStore by preferencesDataStore(name = "ichdj_wishlog")

/**
 * Protokolliert angenommene Wünsche in eine private Tages-Playlist
 * "Ich DJ Wünsche - YYYY-MM-DD" im Spotify-Konto: bei Bedarf anlegen,
 * dann den Song anhängen – bewusst komplett still, Logging darf den
 * Jukebox-Betrieb niemals stören.
 */
class WishLogger(
    private val api: SpotifyApi,
    private val auth: SpotifyAuthManager,
    private val context: Context,
) {
    private val mutex = Mutex()
    private var cachedUserId: String? = null

    suspend fun log(track: Track) {
        try {
            val token = auth.ensureValidToken() ?: return
            mutex.withLock {
                val name = "Ich DJ Wünsche - ${LocalDate.now()}"
                val playlistId = resolvePlaylist(token, name) ?: return
                try {
                    api.addToPlaylist(token, playlistId, track.uri)
                } catch (e: SpotifyApiException) {
                    if (e.code == 404) {
                        // Playlist zwischenzeitlich gelöscht → Cache leeren, neu anlegen
                        clearCached(name)
                        resolvePlaylist(token, name)?.let {
                            api.addToPlaylist(token, it, track.uri)
                        }
                    }
                }
            }
        } catch (_: Exception) {
            // still bleiben (z.B. fehlende Playlist-Scopes bei alter Verbindung)
        }
    }

    private suspend fun resolvePlaylist(token: String, name: String): String? {
        val key = stringPreferencesKey("playlist:$name")
        context.wishLogDataStore.data.first()[key]?.let { return it }

        val id = api.findPlaylistByName(token, name) ?: run {
            val userId = cachedUserId
                ?: api.getProfile(token)?.id?.also { cachedUserId = it }
                ?: return null
            api.createPlaylist(
                token, userId, name,
                "Automatisch von IchDJ protokollierte Besucherwünsche",
            )
        } ?: return null

        context.wishLogDataStore.edit { it[key] = id }
        return id
    }

    private suspend fun clearCached(name: String) {
        context.wishLogDataStore.edit { it.remove(stringPreferencesKey("playlist:$name")) }
    }
}
