package de.ichdj.jukebox.core

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/** Alle konfigurierbaren Einstellungen der App mit ihren Standardwerten. */
data class AppSettings(
    val clientId: String = "",
    val wishBoxCount: Int = 4,
    val lockMinutes: Int = 90,
    val maxTrackMinutes: Int = 10,
    val wishesEnabled: Boolean = true,
    val keepScreenOn: Boolean = true,
    val wishLogEnabled: Boolean = true,
    val operatorPin: String = "", // leer = Menü ungeschützt
    val pollIntervalSeconds: Int = 4,
    val minPlaySecondsForLock: Int = 5,
)

private val Context.settingsDataStore by preferencesDataStore(name = "ichdj_settings")

class SettingsRepository(private val context: Context) {

    private object Keys {
        val CLIENT_ID = stringPreferencesKey("client_id")
        val WISH_BOX_COUNT = intPreferencesKey("wish_box_count")
        val LOCK_MINUTES = intPreferencesKey("lock_minutes")
        val MAX_TRACK_MINUTES = intPreferencesKey("max_track_minutes")
        val WISHES_ENABLED = booleanPreferencesKey("wishes_enabled")
        val KEEP_SCREEN_ON = booleanPreferencesKey("keep_screen_on")
        val WISH_LOG_ENABLED = booleanPreferencesKey("wish_log_enabled")
        val OPERATOR_PIN = stringPreferencesKey("operator_pin")
        val POLL_INTERVAL_SECONDS = intPreferencesKey("poll_interval_seconds")
        val MIN_PLAY_SECONDS = intPreferencesKey("min_play_seconds_for_lock")
    }

    private val defaults = AppSettings()

    val settings: Flow<AppSettings> = context.settingsDataStore.data.map { p ->
        AppSettings(
            clientId = p[Keys.CLIENT_ID] ?: defaults.clientId,
            wishBoxCount = p[Keys.WISH_BOX_COUNT] ?: defaults.wishBoxCount,
            lockMinutes = p[Keys.LOCK_MINUTES] ?: defaults.lockMinutes,
            maxTrackMinutes = p[Keys.MAX_TRACK_MINUTES] ?: defaults.maxTrackMinutes,
            wishesEnabled = p[Keys.WISHES_ENABLED] ?: defaults.wishesEnabled,
            keepScreenOn = p[Keys.KEEP_SCREEN_ON] ?: defaults.keepScreenOn,
            wishLogEnabled = p[Keys.WISH_LOG_ENABLED] ?: defaults.wishLogEnabled,
            operatorPin = p[Keys.OPERATOR_PIN] ?: defaults.operatorPin,
            pollIntervalSeconds = p[Keys.POLL_INTERVAL_SECONDS] ?: defaults.pollIntervalSeconds,
            minPlaySecondsForLock = p[Keys.MIN_PLAY_SECONDS] ?: defaults.minPlaySecondsForLock,
        )
    }

    suspend fun current(): AppSettings = settings.first()

    suspend fun setClientId(value: String) =
        context.settingsDataStore.edit { it[Keys.CLIENT_ID] = value }

    suspend fun setWishBoxCount(value: Int) =
        context.settingsDataStore.edit { it[Keys.WISH_BOX_COUNT] = value.coerceIn(1, 8) }

    suspend fun setLockMinutes(value: Int) =
        context.settingsDataStore.edit { it[Keys.LOCK_MINUTES] = value.coerceIn(0, 24 * 60) }

    suspend fun setMaxTrackMinutes(value: Int) =
        context.settingsDataStore.edit { it[Keys.MAX_TRACK_MINUTES] = value.coerceIn(1, 60) }

    suspend fun setWishesEnabled(value: Boolean) =
        context.settingsDataStore.edit { it[Keys.WISHES_ENABLED] = value }

    suspend fun setKeepScreenOn(value: Boolean) =
        context.settingsDataStore.edit { it[Keys.KEEP_SCREEN_ON] = value }

    suspend fun setWishLogEnabled(value: Boolean) =
        context.settingsDataStore.edit { it[Keys.WISH_LOG_ENABLED] = value }

    suspend fun setOperatorPin(value: String) =
        context.settingsDataStore.edit { it[Keys.OPERATOR_PIN] = value }
}
