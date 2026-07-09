package de.ichdj.jukebox.core

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.historyDataStore by preferencesDataStore(name = "ichdj_history")

/**
 * Merkt sich, wann ein Track zuletzt tatsächlich lief (Basis für die
 * Wiederhol-Sperre). Ein Track wird erst gestempelt, wenn die ersten Sekunden
 * wirklich gespielt wurden; der Stempel wird während des Abspielens laufend
 * aktualisiert, so dass die Sperre praktisch ab Song-Ende zählt.
 */
class PlayHistoryRepository(private val context: Context) {

    private val json = Json { ignoreUnknownKeys = true }
    private val key = stringPreferencesKey("played_at_json")
    private val mutex = Mutex()
    private var cache: MutableMap<String, Long>? = null

    private val maxAgeMillis = 30L * 24 * 60 * 60 * 1000 // Einträge älter als 30 Tage entfallen

    private suspend fun load(): MutableMap<String, Long> {
        cache?.let { return it }
        return mutex.withLock {
            cache ?: run {
                val raw = context.historyDataStore.data.first()[key]
                val parsed = raw?.let {
                    runCatching { json.decodeFromString<Map<String, Long>>(it) }.getOrNull()
                } ?: emptyMap()
                parsed.toMutableMap().also { cache = it }
            }
        }
    }

    suspend fun recordPlayed(uri: String, atMillis: Long) {
        val map = load()
        mutex.withLock {
            map[uri] = atMillis
            val cutoff = atMillis - maxAgeMillis
            map.entries.removeAll { it.value < cutoff }
            context.historyDataStore.edit { it[key] = json.encodeToString(map.toMap()) }
        }
    }

    suspend fun lastPlayedAt(uri: String): Long? = load()[uri]
}
