package de.ichdj.jukebox.auth

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Base64
import androidx.browser.customtabs.CustomTabsIntent
import de.ichdj.jukebox.core.SettingsRepository
import de.ichdj.jukebox.net.SpotifyApi
import de.ichdj.jukebox.net.TokenResponse
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.security.MessageDigest
import java.security.SecureRandom

/**
 * OAuth 2.0 Authorization-Code-Flow mit PKCE. Es wird kein Client-Secret
 * benötigt; die Client-ID kommt aus den Einstellungen (Veranstaltermenü).
 */
class SpotifyAuthManager(
    private val tokenStore: TokenStore,
    private val settings: SettingsRepository,
    private val api: SpotifyApi,
    private val client: OkHttpClient,
) {
    companion object {
        // Port 8888: teilt sich die Registrierung mit dem bestehenden
        // spotd-Client des Veranstalters – Loopback ist pro Gerät, daher
        // kollisionsfrei, und im Dashboard muss nichts geändert werden.
        const val REDIRECT_PORT = 8888
        const val REDIRECT_PATH = "/callback"

        /** Muss exakt so im Spotify Developer Dashboard eingetragen sein. */
        const val REDIRECT_URI = "http://127.0.0.1:$REDIRECT_PORT$REDIRECT_PATH"

        private const val SCOPES =
            "user-read-playback-state user-modify-playback-state user-read-currently-playing"
        private const val AUTH_TIMEOUT_MS = 300_000L
    }

    sealed class AuthResult {
        data object Success : AuthResult()
        data class Failure(val message: String) : AuthResult()
    }

    private val json = Json { ignoreUnknownKeys = true }
    private val refreshMutex = Mutex()

    val connectedFlow get() = tokenStore.connectedFlow
    val displayNameFlow get() = tokenStore.displayNameFlow

    /**
     * Startet den Login: Loopback-Server öffnen, Browser (Custom Tab) auf die
     * Spotify-Anmeldeseite schicken, Code entgegennehmen und gegen Tokens
     * tauschen. Muss außerhalb des Kiosk-Pinnings aufgerufen werden, da ein
     * Browser geöffnet wird.
     */
    suspend fun authorize(activityContext: Context): AuthResult {
        val clientId = settings.current().clientId.trim()
        if (clientId.isEmpty()) return AuthResult.Failure("Client-ID fehlt")

        val verifier = randomUrlSafe(64)
        val challenge = sha256UrlSafe(verifier)
        val state = randomUrlSafe(16)

        val authUri = Uri.parse("https://accounts.spotify.com/authorize").buildUpon()
            .appendQueryParameter("response_type", "code")
            .appendQueryParameter("client_id", clientId)
            .appendQueryParameter("scope", SCOPES)
            .appendQueryParameter("redirect_uri", REDIRECT_URI)
            .appendQueryParameter("state", state)
            .appendQueryParameter("code_challenge_method", "S256")
            .appendQueryParameter("code_challenge", challenge)
            .build()

        return coroutineScope {
            val server = async(Dispatchers.IO) {
                runInterruptible {
                    LoopbackServer.awaitCode(REDIRECT_PORT, REDIRECT_PATH, state, AUTH_TIMEOUT_MS)
                }
            }
            delay(200) // Server binden lassen, bevor der Browser lossschickt

            if (!openBrowser(activityContext, authUri)) {
                server.cancel()
                return@coroutineScope AuthResult.Failure("Kein Browser installiert")
            }

            when (val result = server.await()) {
                is LoopbackServer.Result.Error -> AuthResult.Failure(result.message)
                is LoopbackServer.Result.Code ->
                    exchangeCode(clientId, result.code, verifier)
            }
        }
    }

    private fun openBrowser(context: Context, uri: Uri): Boolean = try {
        CustomTabsIntent.Builder().build().launchUrl(context, uri)
        true
    } catch (_: ActivityNotFoundException) {
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, uri))
            true
        } catch (_: ActivityNotFoundException) {
            false
        }
    }

    private suspend fun exchangeCode(
        clientId: String,
        code: String,
        verifier: String,
    ): AuthResult {
        val form = FormBody.Builder()
            .add("grant_type", "authorization_code")
            .add("code", code)
            .add("redirect_uri", REDIRECT_URI)
            .add("client_id", clientId)
            .add("code_verifier", verifier)
            .build()
        return try {
            val response = postToken(form)
            val refresh = response.refreshToken
                ?: return AuthResult.Failure("Kein Refresh-Token erhalten")
            tokenStore.save(
                response.accessToken,
                refresh,
                System.currentTimeMillis() + response.expiresIn * 1000,
            )
            val name = runCatching { api.getProfileName(response.accessToken) }.getOrNull()
            tokenStore.setDisplayName(name ?: "Spotify-Konto")
            AuthResult.Success
        } catch (e: IOException) {
            AuthResult.Failure(e.message ?: "Netzwerkfehler")
        }
    }

    /**
     * Liefert ein gültiges Access-Token (erneuert es bei Bedarf) oder null,
     * wenn keine Verbindung besteht.
     */
    suspend fun ensureValidToken(): String? {
        val tokens = tokenStore.get() ?: return null
        if (tokens.expiresAtMillis - 60_000 > System.currentTimeMillis()) {
            return tokens.accessToken
        }
        return refreshMutex.withLock {
            val current = tokenStore.get() ?: return@withLock null
            if (current.expiresAtMillis - 60_000 > System.currentTimeMillis()) {
                return@withLock current.accessToken
            }
            val clientId = settings.current().clientId.trim()
            val form = FormBody.Builder()
                .add("grant_type", "refresh_token")
                .add("refresh_token", current.refreshToken)
                .add("client_id", clientId)
                .build()
            try {
                val response = postToken(form)
                tokenStore.save(
                    response.accessToken,
                    response.refreshToken ?: current.refreshToken,
                    System.currentTimeMillis() + response.expiresIn * 1000,
                )
                response.accessToken
            } catch (e: TokenEndpointException) {
                // Refresh-Token endgültig ungültig (z.B. Zugriff entzogen) → trennen
                if (e.code == 400 && e.body.contains("invalid_grant")) tokenStore.clear()
                null
            } catch (_: IOException) {
                null
            }
        }
    }

    suspend fun disconnect() = tokenStore.clear()

    private class TokenEndpointException(val code: Int, val body: String) :
        IOException("Token endpoint HTTP $code")

    private suspend fun postToken(form: FormBody): TokenResponse =
        withContext(Dispatchers.IO) {
            val request = Request.Builder()
                .url("https://accounts.spotify.com/api/token")
                .post(form)
                .build()
            client.newCall(request).execute().use { response ->
                val body = response.body?.string() ?: ""
                if (!response.isSuccessful) throw TokenEndpointException(response.code, body)
                json.decodeFromString<TokenResponse>(body)
            }
        }

    private fun randomUrlSafe(byteCount: Int): String {
        val bytes = ByteArray(byteCount)
        SecureRandom().nextBytes(bytes)
        return Base64.encodeToString(
            bytes,
            Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING,
        )
    }

    private fun sha256UrlSafe(input: String): String =
        Base64.encodeToString(
            MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.US_ASCII)),
            Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING,
        )
}
