package de.ichdj.jukebox.auth

import java.io.IOException
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketTimeoutException
import java.net.URLDecoder

/**
 * Minimaler HTTP-Listener auf 127.0.0.1, der genau einen OAuth-Redirect
 * entgegennimmt. Spotify erlaubt (seit den Sicherheitsänderungen 2025) nur
 * noch HTTPS- oder explizite Loopback-Redirect-URIs – dieser Server bedient
 * letzteres, ohne dass eine eigene Domain nötig ist.
 *
 * Wichtig: Browser öffnen neben der eigentlichen Navigation oft zusätzliche
 * Verbindungen (Preconnect/Spekulation, /favicon.ico). Solche Verbindungen
 * dürfen den Listener nicht beenden – er lauscht weiter, bis der echte
 * Redirect mit passendem state eintrifft oder das Timeout abläuft.
 */
object LoopbackServer {

    sealed class Result {
        data class Code(val code: String) : Result()
        data class Error(val message: String) : Result()
    }

    /** Blockiert, bis der Redirect eintrifft oder das Timeout abläuft. */
    fun awaitCode(port: Int, path: String, expectedState: String, timeoutMs: Long): Result {
        val deadline = System.currentTimeMillis() + timeoutMs
        ServerSocket().use { server ->
            server.reuseAddress = true
            server.bind(InetSocketAddress(InetAddress.getLoopbackAddress(), port), 16)
            server.soTimeout = 1000
            while (System.currentTimeMillis() < deadline && !Thread.currentThread().isInterrupted) {
                val socket = try {
                    server.accept()
                } catch (_: SocketTimeoutException) {
                    continue
                }
                val result = try {
                    socket.use { handleConnection(it, path, expectedState) }
                } catch (_: IOException) {
                    null // kaputte oder spekulative Verbindung → weiter lauschen
                }
                if (result != null) return result
            }
        }
        return Result.Error("Zeitüberschreitung bei der Anmeldung")
    }

    /** Liefert null, wenn weiter gelauscht werden soll. */
    private fun handleConnection(s: Socket, path: String, expectedState: String): Result? {
        s.soTimeout = 3000
        val requestLine = s.getInputStream().bufferedReader().readLine() ?: return null
        val target = requestLine.split(" ").getOrNull(1) ?: return null
        if (!target.startsWith(path)) {
            respond(s, 404, "Not found") // z.B. /favicon.ico
            return null
        }
        val params = parseQuery(target.substringAfter('?', ""))
        if (params["state"] != expectedState) {
            // Veralteter Aufruf (z.B. alter Browser-Tab) → echten Redirect abwarten
            respond(s, 400, page("Ungültige Anfrage – bitte die Anmeldung in der App neu starten."))
            return null
        }
        params["error"]?.let { err ->
            respond(s, 200, page("Anmeldung abgebrochen: $err"))
            return Result.Error(err)
        }
        val code = params["code"] ?: run {
            respond(s, 400, page("Kein Code erhalten."))
            return Result.Error("Kein Autorisierungscode erhalten")
        }
        respond(
            s, 200,
            page("Anmeldung erfolgreich! Du kannst dieses Fenster schließen und zur IchDJ-App zurückkehren."),
        )
        return Result.Code(code)
    }

    private fun parseQuery(query: String): Map<String, String> =
        query.split('&').mapNotNull { part ->
            val i = part.indexOf('=')
            if (i < 0) null
            else URLDecoder.decode(part.take(i), "UTF-8") to
                URLDecoder.decode(part.substring(i + 1), "UTF-8")
        }.toMap()

    private fun page(message: String): String =
        "<!doctype html><html lang=\"de\"><head><meta charset=\"utf-8\">" +
            "<title>IchDJ</title></head><body style=\"font-family:sans-serif;" +
            "background:#121212;color:#eee;display:flex;align-items:center;" +
            "justify-content:center;height:100vh;margin:0\">" +
            "<div style=\"max-width:28em;text-align:center\"><h2>IchDJ</h2>" +
            "<p>$message</p></div></body></html>"

    private fun respond(socket: Socket, status: Int, body: String) {
        val statusText = if (status == 200) "OK" else "Error"
        val bytes = body.toByteArray(Charsets.UTF_8)
        val header = "HTTP/1.1 $status $statusText\r\n" +
            "Content-Type: text/html; charset=utf-8\r\n" +
            "Content-Length: ${bytes.size}\r\n" +
            "Connection: close\r\n\r\n"
        socket.getOutputStream().apply {
            write(header.toByteArray(Charsets.US_ASCII))
            write(bytes)
            flush()
        }
    }
}
