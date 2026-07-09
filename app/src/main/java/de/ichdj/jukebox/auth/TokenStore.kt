package de.ichdj.jukebox.auth

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.tokenDataStore by preferencesDataStore(name = "ichdj_tokens")

data class Tokens(
    val accessToken: String,
    val refreshToken: String,
    val expiresAtMillis: Long,
)

class TokenStore(private val context: Context) {

    private object Keys {
        val ACCESS = stringPreferencesKey("access_token")
        val REFRESH = stringPreferencesKey("refresh_token")
        val EXPIRES_AT = longPreferencesKey("expires_at")
        val DISPLAY_NAME = stringPreferencesKey("display_name")
    }

    val connectedFlow: Flow<Boolean> =
        context.tokenDataStore.data.map { it[Keys.REFRESH] != null }

    val displayNameFlow: Flow<String?> =
        context.tokenDataStore.data.map { p ->
            if (p[Keys.REFRESH] == null) null else p[Keys.DISPLAY_NAME]
        }

    suspend fun get(): Tokens? {
        val p = context.tokenDataStore.data.first()
        return Tokens(
            accessToken = p[Keys.ACCESS] ?: return null,
            refreshToken = p[Keys.REFRESH] ?: return null,
            expiresAtMillis = p[Keys.EXPIRES_AT] ?: 0L,
        )
    }

    suspend fun save(accessToken: String, refreshToken: String, expiresAtMillis: Long) {
        context.tokenDataStore.edit {
            it[Keys.ACCESS] = accessToken
            it[Keys.REFRESH] = refreshToken
            it[Keys.EXPIRES_AT] = expiresAtMillis
        }
    }

    suspend fun setDisplayName(name: String) {
        context.tokenDataStore.edit { it[Keys.DISPLAY_NAME] = name }
    }

    suspend fun clear() {
        context.tokenDataStore.edit { it.clear() }
    }
}
