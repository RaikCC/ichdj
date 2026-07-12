package de.ichdj.jukebox.engine

import de.ichdj.jukebox.auth.SpotifyAuthManager
import de.ichdj.jukebox.core.AppSettings
import de.ichdj.jukebox.core.PlayHistoryRepository
import de.ichdj.jukebox.core.SettingsRepository
import de.ichdj.jukebox.core.WishStore
import de.ichdj.jukebox.model.Track
import de.ichdj.jukebox.model.Wish
import de.ichdj.jukebox.net.SpotifyApi
import de.ichdj.jukebox.net.SpotifyApiException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import java.io.IOException

/** Der im Master gerade laufende Track samt Fortschritt zum Abrufzeitpunkt. */
data class CurrentPlayback(
    val track: Track,
    val progressMs: Long,
    val isPlaying: Boolean,
    val fetchedAtMillis: Long,
)

data class EngineState(
    val connected: Boolean = false,
    val current: CurrentPlayback? = null,
    val queue: List<Track> = emptyList(),
    val wishes: List<Wish> = emptyList(),
    val syncTrouble: Boolean = false,
)

sealed class WishResult {
    data object Accepted : WishResult()
    data class TooLong(val maxMinutes: Int) : WishResult()
    data class Locked(val allowedAgainAtMillis: Long) : WishResult()
    data object AlreadyQueued : WishResult()
    data object NoFreeSlot : WishResult()
    data object NotConnected : WishResult()
    data object NoActiveDevice : WishResult()
    data class Error(val message: String?) : WishResult()
}

/**
 * Kernlogik: pollt Wiedergabestatus und Queue des Master-Accounts, pflegt den
 * Lebenszyklus der Wünsche und nimmt neue Wünsche entgegen.
 */
class JukeboxEngine(
    private val api: SpotifyApi,
    private val auth: SpotifyAuthManager,
    private val settings: SettingsRepository,
    private val history: PlayHistoryRepository,
    private val wishStore: WishStore,
    private val wishLogger: WishLogger,
    private val scope: CoroutineScope,
) {
    private val _state = MutableStateFlow(EngineState())
    val state: StateFlow<EngineState> = _state.asStateFlow()

    private val wishMutex = Mutex()
    private var failureStreak = 0
    private var started = false

    /** Weckt die Poll-Schleife vorzeitig, z.B. direkt nach einem neuen Wunsch. */
    private val resyncRequests = Channel<Unit>(Channel.CONFLATED)

    /**
     * Spotifys Queue-Endpoint hinkt dem Einreihen einige Sekunden hinterher.
     * So lange gilt ein frischer Wunsch nicht als "vom Master entfernt",
     * auch wenn er im Poll noch fehlt.
     */
    private val freshWishGraceMillis = 20_000L

    fun start() {
        if (started) return
        started = true
        scope.launch {
            _state.update { it.copy(wishes = wishStore.load()) }
            while (isActive) {
                val s = settings.current()
                pollOnce(s)
                val intervalMs = s.pollIntervalSeconds.coerceAtLeast(2) * 1000L
                val poked = withTimeoutOrNull(intervalMs) { resyncRequests.receive() } != null
                if (poked) delay(1500) // Spotify kurz Zeit geben, das Einreihen zu verarbeiten
            }
        }
    }

    private suspend fun pollOnce(s: AppSettings) {
        val token = auth.ensureValidToken()
        if (token == null) {
            _state.update { it.copy(connected = false, current = null, queue = emptyList()) }
            return
        }
        try {
            val playback = api.getPlayback(token)
            val queueDto = api.getQueue(token)
            failureStreak = 0

            val now = System.currentTimeMillis()
            val current = playback?.item?.toTrack()?.let { track ->
                CurrentPlayback(track, playback.progressMs ?: 0, playback.isPlaying, now)
            }
            val queue = queueDto.queue.mapNotNull { it.toTrack() }

            // Sperr-Historie: zählt erst, wenn die ersten Sekunden wirklich liefen
            if (current != null && current.isPlaying &&
                current.progressMs >= s.minPlaySecondsForLock * 1000L
            ) {
                history.recordPlayed(current.track.uri, now)
            }

            updateWishes(current, queue)
            _state.update {
                it.copy(connected = true, current = current, queue = queue, syncTrouble = false)
            }
        } catch (e: SpotifyApiException) {
            registerFailure()
        } catch (e: IOException) {
            registerFailure()
        }
    }

    private fun registerFailure() {
        failureStreak++
        if (failureStreak >= 3) _state.update { it.copy(syncTrouble = true) }
    }

    private suspend fun updateWishes(current: CurrentPlayback?, queue: List<Track>) {
        wishMutex.withLock {
            val now = System.currentTimeMillis()
            val queueUris = queue.map { it.uri }.toSet()
            val old = _state.value.wishes
            val updated = old.mapNotNull { wish ->
                when {
                    current?.track?.uri == wish.track.uri ->
                        wish.copy(startedPlaying = true) // spielt gerade (interne Nummer 0)
                    wish.startedPlaying -> null // fertig gespielt → Box wird frei
                    wish.track.uri in queueUris -> wish // wartet noch
                    // Frisch eingereiht: Queue-Endpoint hinkt hinterher → behalten,
                    // sonst würde der Wunsch fälschlich als "entfernt" gelten
                    now - wish.wishedAtMillis < freshWishGraceMillis -> wish
                    else -> null // vom Master aus der Queue entfernt
                }
            }
            if (updated != old) {
                wishStore.save(updated)
                _state.update { it.copy(wishes = updated) }
            }
        }
    }

    /** Prüft alle Regeln und reiht den Wunsch bei Erfolg in die Spotify-Queue ein. */
    suspend fun submitWish(track: Track): WishResult {
        val s = settings.current()
        val token = auth.ensureValidToken() ?: return WishResult.NotConnected
        return wishMutex.withLock {
            val st = _state.value
            if (!s.wishesEnabled) return@withLock WishResult.NoFreeSlot
            if (st.wishes.size >= s.wishBoxCount) return@withLock WishResult.NoFreeSlot
            if (track.durationMs > s.maxTrackMinutes * 60_000L) {
                return@withLock WishResult.TooLong(s.maxTrackMinutes)
            }
            history.lastPlayedAt(track.uri)?.let { playedAt ->
                val allowedAgain = playedAt + s.lockMinutes * 60_000L
                if (System.currentTimeMillis() < allowedAgain) {
                    return@withLock WishResult.Locked(allowedAgain)
                }
            }
            val alreadyThere = st.current?.track?.uri == track.uri ||
                st.queue.any { it.uri == track.uri } ||
                st.wishes.any { it.track.uri == track.uri }
            if (alreadyThere) return@withLock WishResult.AlreadyQueued

            try {
                api.addToQueue(token, track.uri)
            } catch (e: SpotifyApiException) {
                return@withLock if (e.code == 404) WishResult.NoActiveDevice
                else WishResult.Error("HTTP ${e.code}")
            } catch (e: IOException) {
                return@withLock WishResult.Error(e.message)
            }

            val wishes = _state.value.wishes + Wish(track, System.currentTimeMillis())
            wishStore.save(wishes)
            // Queue optimistisch ergänzen; der nächste Poll liefert die echte Reihenfolge
            _state.update { it.copy(wishes = wishes, queue = it.queue + track) }
            resyncRequests.trySend(Unit) // zeitnah neu abgleichen
            if (s.wishLogEnabled) {
                scope.launch { wishLogger.log(track) } // still, stört den Betrieb nie
            }
            WishResult.Accepted
        }
    }

    suspend fun search(query: String): List<Track> {
        val token = auth.ensureValidToken() ?: return emptyList()
        return try {
            api.search(token, query)
        } catch (_: IOException) {
            emptyList()
        }
    }
}
