package de.ichdj.jukebox

import android.app.Application
import android.content.Context
import de.ichdj.jukebox.auth.SpotifyAuthManager
import de.ichdj.jukebox.auth.TokenStore
import de.ichdj.jukebox.core.PlayHistoryRepository
import de.ichdj.jukebox.core.SettingsRepository
import de.ichdj.jukebox.core.WishStore
import de.ichdj.jukebox.engine.JukeboxEngine
import de.ichdj.jukebox.net.SpotifyApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

/** Einfache, manuelle Dependency-Verdrahtung – bewusst ohne DI-Framework. */
class AppContainer(context: Context) {
    val settings = SettingsRepository(context)
    val tokenStore = TokenStore(context)
    val history = PlayHistoryRepository(context)
    val wishStore = WishStore(context)

    val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    val api = SpotifyApi(httpClient)
    val auth = SpotifyAuthManager(tokenStore, settings, api, httpClient)

    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    val engine = JukeboxEngine(api, auth, settings, history, wishStore, appScope)
}

class IchDjApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
