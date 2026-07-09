package de.ichdj.jukebox.core

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import de.ichdj.jukebox.model.Wish
import kotlinx.coroutines.flow.first
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.wishDataStore by preferencesDataStore(name = "ichdj_wishes")

/** Persistiert die aktiven Wünsche, damit ein App-Neustart sie nicht verliert. */
class WishStore(private val context: Context) {

    private val json = Json { ignoreUnknownKeys = true }
    private val key = stringPreferencesKey("wishes_json")

    suspend fun load(): List<Wish> {
        val raw = context.wishDataStore.data.first()[key] ?: return emptyList()
        return runCatching { json.decodeFromString<List<Wish>>(raw) }.getOrElse { emptyList() }
    }

    suspend fun save(wishes: List<Wish>) {
        context.wishDataStore.edit { it[key] = json.encodeToString(wishes) }
    }
}
