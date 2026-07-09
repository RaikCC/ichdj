package de.ichdj.jukebox.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import de.ichdj.jukebox.AppContainer
import de.ichdj.jukebox.auth.SpotifyAuthManager
import de.ichdj.jukebox.core.AppSettings
import de.ichdj.jukebox.core.TimeFormat
import de.ichdj.jukebox.engine.EngineState
import de.ichdj.jukebox.engine.WishResult
import de.ichdj.jukebox.model.Track
import de.ichdj.jukebox.model.Wish
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class UiMode { VISITOR, OPERATOR }

data class WishBoxUi(
    val wish: Wish? = null,
    val number: Int? = null,
    val isPlaying: Boolean = false,
    val startLabel: String? = null, // "HH:mm"; null bedeutet "jetzt" bzw. leer
    val remainingLabel: String? = null, // Restlaufzeit, nur wenn spielend
    val durationLabel: String? = null,
)

data class QueueEntryUi(
    val track: Track,
    val key: String,
    val isCurrent: Boolean,
    val isPlayingNow: Boolean,
    val isWish: Boolean,
    val wishNumber: Int?,
    val durationLabel: String,
    val startLabel: String?, // null beim aktuellen Song → "jetzt"
    val remainingLabel: String?,
)

data class SearchUi(
    val visible: Boolean = false,
    val query: String = "",
    val loading: Boolean = false,
    val results: List<Track> = emptyList(),
    val confirmTrack: Track? = null,
    val rejection: WishResult? = null,
    val submitting: Boolean = false,
)

data class JukeboxUiState(
    val mode: UiMode = UiMode.VISITOR,
    val connected: Boolean = false,
    val syncTrouble: Boolean = false,
    val wishesEnabled: Boolean = true,
    val boxes: List<WishBoxUi> = emptyList(),
    val queue: List<QueueEntryUi> = emptyList(),
    val allBoxesFull: Boolean = false,
    val search: SearchUi = SearchUi(),
    val settings: AppSettings = AppSettings(),
    val accountName: String? = null,
    val authBusy: Boolean = false,
    val authError: String? = null,
)

class MainViewModel(private val container: AppContainer) : ViewModel() {

    private val engine = container.engine
    private val modeFlow = MutableStateFlow(UiMode.VISITOR)
    private val searchFlow = MutableStateFlow(SearchUi())
    private val authBusyFlow = MutableStateFlow(false)
    private val authErrorFlow = MutableStateFlow<String?>(null)
    private var searchJob: Job? = null

    /** Sekundentakt, damit Restlaufzeit und Startzeiten laufend stimmen. */
    private val ticker: Flow<Long> = flow {
        while (true) {
            emit(System.currentTimeMillis())
            delay(1000)
        }
    }

    private val base: Flow<JukeboxUiState> = combine(
        engine.state, container.settings.settings, modeFlow, searchFlow, ticker,
    ) { engineState, settings, mode, search, now ->
        buildUiState(engineState, settings, mode, search, now)
    }

    private val account: Flow<Triple<String?, Boolean, String?>> = combine(
        container.auth.displayNameFlow, authBusyFlow, authErrorFlow,
    ) { name, busy, error -> Triple(name, busy, error) }

    val uiState: StateFlow<JukeboxUiState> =
        combine(base, account) { state, (name, busy, error) ->
            state.copy(accountName = name, authBusy = busy, authError = error)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), JukeboxUiState())

    private fun buildUiState(
        es: EngineState,
        s: AppSettings,
        mode: UiMode,
        search: SearchUi,
        now: Long,
    ): JukeboxUiState {
        val current = es.current
        val elapsed = if (current?.isPlaying == true) now - current.fetchedAtMillis else 0L
        val progress = ((current?.progressMs ?: 0L) + elapsed)
            .coerceAtMost(current?.track?.durationMs ?: 0L)
        val remainingMs = current?.let { (it.track.durationMs - progress).coerceAtLeast(0L) } ?: 0L

        // Spielender Wunsch hat intern Nummer 0 (wird nicht angezeigt),
        // wartende Wünsche bekommen 1..n in der Reihenfolge der Master-Queue.
        val playingWish = current?.let { c -> es.wishes.firstOrNull { it.track.uri == c.track.uri } }
        val waitingWishes = es.wishes
            .filter { it.track.uri != current?.track?.uri }
            .sortedBy { w ->
                val idx = es.queue.indexOfFirst { it.uri == w.track.uri }
                if (idx < 0) Int.MAX_VALUE else idx
            }
        val numberByUri = buildMap {
            waitingWishes.forEachIndexed { i, w -> put(w.track.uri, i + 1) }
        }

        val entries = mutableListOf<QueueEntryUi>()
        if (current != null) {
            entries += QueueEntryUi(
                track = current.track,
                key = "current",
                isCurrent = true,
                isPlayingNow = current.isPlaying,
                isWish = playingWish != null,
                wishNumber = null,
                durationLabel = TimeFormat.duration(current.track.durationMs),
                startLabel = null,
                remainingLabel = TimeFormat.duration(remainingMs),
            )
        }
        var cursor = now + remainingMs
        val startByUri = mutableMapOf<String, String>()
        val flaggedWishUris = mutableSetOf<String>()
        es.queue.forEachIndexed { idx, track ->
            val isWish = track.uri in numberByUri && flaggedWishUris.add(track.uri)
            val startLabel = TimeFormat.clockFloor(cursor)
            startByUri.putIfAbsent(track.uri, startLabel)
            entries += QueueEntryUi(
                track = track,
                key = "q$idx:${track.uri}",
                isCurrent = false,
                isPlayingNow = false,
                isWish = isWish,
                wishNumber = if (isWish) numberByUri[track.uri] else null,
                durationLabel = TimeFormat.duration(track.durationMs),
                startLabel = startLabel,
                remainingLabel = null,
            )
            cursor += track.durationMs
        }

        val boxes = buildList {
            if (playingWish != null) {
                add(
                    WishBoxUi(
                        wish = playingWish,
                        number = null,
                        isPlaying = true,
                        startLabel = null,
                        remainingLabel = TimeFormat.duration(remainingMs),
                        durationLabel = TimeFormat.duration(playingWish.track.durationMs),
                    ),
                )
            }
            waitingWishes.forEach { w ->
                add(
                    WishBoxUi(
                        wish = w,
                        number = numberByUri[w.track.uri],
                        isPlaying = false,
                        startLabel = startByUri[w.track.uri],
                        remainingLabel = null,
                        durationLabel = TimeFormat.duration(w.track.durationMs),
                    ),
                )
            }
            while (size < s.wishBoxCount) add(WishBoxUi())
        }

        return JukeboxUiState(
            mode = mode,
            connected = es.connected,
            syncTrouble = es.syncTrouble,
            wishesEnabled = s.wishesEnabled,
            boxes = boxes,
            queue = entries,
            allBoxesFull = es.wishes.size >= s.wishBoxCount,
            search = search,
            settings = s,
        )
    }

    // ---- Modus ----

    fun currentMode(): UiMode = modeFlow.value

    fun enterOperator() {
        authErrorFlow.value = null
        modeFlow.value = UiMode.OPERATOR
    }

    fun exitOperator() {
        modeFlow.value = UiMode.VISITOR
    }

    // ---- Suche & Wünsche ----

    fun openSearch() {
        searchFlow.value = SearchUi(visible = true)
    }

    fun closeSearch() {
        searchJob?.cancel()
        searchFlow.value = SearchUi()
    }

    fun setQuery(query: String) {
        searchFlow.update { it.copy(query = query) }
        searchJob?.cancel()
        if (query.trim().length < 2) {
            searchFlow.update { it.copy(results = emptyList(), loading = false) }
            return
        }
        searchJob = viewModelScope.launch {
            delay(350) // Tipp-Pause abwarten
            searchFlow.update { it.copy(loading = true) }
            val results = engine.search(query.trim())
            searchFlow.update {
                if (it.query == query) it.copy(results = results, loading = false) else it
            }
        }
    }

    fun selectTrack(track: Track) {
        searchFlow.update { it.copy(confirmTrack = track) }
    }

    fun dismissConfirm() {
        searchFlow.update { it.copy(confirmTrack = null) }
    }

    fun confirmWish() {
        val track = searchFlow.value.confirmTrack ?: return
        if (searchFlow.value.submitting) return
        viewModelScope.launch {
            searchFlow.update { it.copy(submitting = true) }
            when (val result = engine.submitWish(track)) {
                is WishResult.Accepted -> closeSearch()
                else -> searchFlow.update {
                    it.copy(confirmTrack = null, submitting = false, rejection = result)
                }
            }
        }
    }

    fun dismissRejection() {
        searchFlow.update { it.copy(rejection = null) }
    }

    // ---- Veranstalter ----

    private var authJob: Job? = null

    fun connectSpotify(activityContext: Context) {
        // Erneutes Tippen bricht einen hängenden Versuch ab und startet neu
        // (gibt auch den Loopback-Port wieder frei).
        val previous = authJob
        authJob = viewModelScope.launch {
            previous?.cancelAndJoin()
            authBusyFlow.value = true
            authErrorFlow.value = null
            try {
                val result = container.auth.authorize(activityContext)
                if (result is SpotifyAuthManager.AuthResult.Failure) {
                    authErrorFlow.value = result.message
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                authErrorFlow.value = e.message ?: "Unerwarteter Fehler"
            } finally {
                authBusyFlow.value = false
            }
        }
    }

    fun disconnectSpotify() {
        viewModelScope.launch { container.auth.disconnect() }
    }

    fun setClientId(value: String) {
        viewModelScope.launch { container.settings.setClientId(value) }
    }

    fun setWishBoxCount(value: Int) {
        viewModelScope.launch { container.settings.setWishBoxCount(value) }
    }

    fun setLockMinutes(value: Int) {
        viewModelScope.launch { container.settings.setLockMinutes(value) }
    }

    fun setMaxTrackMinutes(value: Int) {
        viewModelScope.launch { container.settings.setMaxTrackMinutes(value) }
    }

    fun setWishesEnabled(value: Boolean) {
        viewModelScope.launch { container.settings.setWishesEnabled(value) }
    }

    companion object {
        fun factory(container: AppContainer): ViewModelProvider.Factory = viewModelFactory {
            initializer { MainViewModel(container) }
        }
    }
}
