# IchDJ – "I can haz DJ?"

Kiosk-Jukebox-App für Android-Tablets: Gäste sehen die aktuelle Spotify-Queue
und dürfen sich Songs wünschen, ohne dass sie einen eigenen Spotify-Account
brauchen. Die fachliche Spezifikation steht in
[project-spotify-ichdj.md](project-spotify-ichdj.md).

## Wie die APK gebaut wird (ohne lokale Build-Umgebung)

Das Repo enthält einen GitHub-Actions-Workflow
([.github/workflows/build-apk.yml](.github/workflows/build-apk.yml)), der bei
jedem Push auf `main` automatisch eine signierte Release-APK baut:

- als **Artifact** `ichdj-apk` am Workflow-Lauf, und
- als **Release "latest"** mit stabiler Download-URL, die man direkt am Tablet
  im Browser öffnen kann:

  **https://github.com/RaikCC/ichdj/releases/download/latest/ichdj.apk**

Das Repo liegt auf https://github.com/RaikCC/ichdj. Jeder Push auf `main`
baut neu (Dauer ca. 4–8 Minuten, GitHub-Free reicht: öffentliche Repos haben
unbegrenzte Build-Minuten).

### Signatur / Keystore

`signing/ichdj-release.jks` ist ein reiner **Sideload-Keystore** (Passwort
`ichdj-sideload`, bewusst im Repo): Er sorgt nur dafür, dass alle Builds
dieselbe Signatur tragen und Updates ohne Deinstallation installierbar sind.
Er schützt keinerlei Geheimnisse und ist NICHT für einen Play-Store-Release
gedacht. Fehlt er, erzeugt der CI-Lauf ihn automatisch neu und committet ihn.

## Spotify einrichten (einmalig)

1. Es wird eine bestehende App-Registrierung mitgenutzt (Spotify vergibt
   keine neuen Dev-Clients mehr beliebig): Die App verwendet den Redirect
   `http://127.0.0.1:8888/callback` – identisch mit der vorhandenen
   spotd-Registrierung, im Dashboard ist nichts zu ändern. (Loopback-Adressen
   gelten pro Gerät; spotd auf dem Server und IchDJ auf dem Tablet kommen
   sich nicht in die Quere, auch die Tokens sind unabhängig.)
2. Auf https://developer.spotify.com/dashboard die **Client ID** der
   bestehenden App kopieren.
3. Hinweis: Rate-Limits gelten pro Client-ID und werden mit spotd geteilt.
   Falls Spotify mit HTTP 429 drosselt, das Poll-Intervall erhöhen
   (`pollIntervalSeconds` in den Settings-Defaults).
4. In der App: 5× schnell auf das Logo tippen → Veranstaltermenü → Client-ID
   eintragen → „Mit Spotify verbinden" (dafür muss ein Browser auf dem Tablet
   installiert sein; nur für diese einmalige Anmeldung).

Damit Wünsche funktionieren, muss auf dem Master-Gerät (Laptop des
Veranstalters) Spotify mit demselben Account **aktiv abspielen**.

## Installation auf dem Tablet (Sideload)

1. Am Tablet die APK-URL öffnen (siehe oben) oder die APK per USB kopieren.
2. „Installation aus unbekannten Quellen" für den Browser/Dateimanager
   erlauben, APK installieren.

### Echter Kiosk-Modus (empfohlen für fest verbaute Tablets)

Ohne weitere Einrichtung nutzt die App das normale Android-App-Pinning
(Ausbruch durch langes Drücken von Zurück+Übersicht möglich). Für einen
Kiosk-Modus ohne Ausbruchsmöglichkeit die App als **Device Owner** setzen:

```
# Voraussetzung: Auf dem Tablet ist KEIN Google-Konto angemeldet
# (ggf. vorher entfernen, danach wieder hinzufügen). USB-Debugging aktivieren.
adb shell dpm set-device-owner de.ichdj.jukebox/.kiosk.IchDjAdminReceiver
```

Rückgängig machen: Im Veranstaltermenü den Button **„Kiosk-Modus (Device
Owner) aufheben"** verwenden. (Per adb geht es bei einem echten Device Owner
NICHT – `dpm remove-active-admin` wird von Android abgelehnt; einziger
weiterer Ausweg wäre ein Werksreset.)

Das Veranstaltermenü (5× aufs Logo tippen) wird über die Gerätesperre des
Tablets geschützt (PIN/Muster/Fingerabdruck – ohne Sperre ist es frei
zugänglich). Bleibt die Abfrage unbeantwortet, bricht sie nach 30 Sekunden
automatisch ab und kehrt zur Besucheransicht zurück.

## Wunsch-Protokoll (Playlist-Logging)

Ist „Wünsche in Spotify-Playlist protokollieren" im Veranstaltermenü aktiv
(Standard: an), landet jeder angenommene Wunsch zusätzlich in einer privaten
Tages-Playlist **„Ich DJ Wünsche - JJJJ-MM-TT"** im Spotify-Konto. Die
Playlist wird beim ersten Wunsch des Tages automatisch angelegt. Das Logging
läuft komplett still und stört den Betrieb nie – schlägt es fehl (z.B.
fehlende Berechtigung), passiert einfach nichts.

Hinweis: Dafür braucht die App die Playlist-Berechtigungen. Wurde die
Spotify-Verbindung mit einer App-Version vor v0.1.9 hergestellt, einmal im
Veranstaltermenü **trennen und neu verbinden**.

## Lokal bauen (optional)

Es wird bewusst **keine** systemweite Build-Umgebung benötigt. Für lokale
Builds liegt eine komplette Toolchain (JDK 17, Android SDK, Gradle-Caches)
ausschließlich unter `%LOCALAPPDATA%\ichdj-build` (~3 GB). **Aufräumen =
diesen einen Ordner löschen.**

```powershell
$env:JAVA_HOME = "$env:LOCALAPPDATA\ichdj-build\jdk"
$env:GRADLE_USER_HOME = "$env:LOCALAPPDATA\ichdj-build\gradle-home"
$env:ICHDJ_BUILD_DIR = "$env:LOCALAPPDATA\ichdj-build\out\app"   # Build-Ausgaben raus aus OneDrive
.\gradlew.bat --project-cache-dir "$env:LOCALAPPDATA\ichdj-build\project-cache" assembleRelease
# Ergebnis: %LOCALAPPDATA%\ichdj-build\out\app\outputs\apk\release\app-release.apk
```

Wichtig: `ICHDJ_BUILD_DIR` setzen, sonst schreibt Gradle nach `app\build` im
OneDrive-Ordner – OneDrive sperrt dort sporadisch Dateien und der Build
schlägt mit `AccessDeniedException` fehl.

## Branding / eigene Grafiken

| Asset | Pfad | Format |
|---|---|---|
| Wordmark (Besucheransicht, Logo integriert) | `app/src/main/assets/branding/wordmark.*` | SVG **oder** Rastergrafik (PNG/WebP, transparent). Angezeigt mit 52 dp Höhe auf #121212. |
| Logo (Referenz + Quelle fürs App-Icon) | `app/src/main/assets/branding/logo.png` | Wird in der App selbst nicht angezeigt. |
| App-Icon (Launcher) | `app/src/main/res/drawable-*dpi/ic_launcher_foreground.png` | Aus `logo.png` generiert (Motiv in der Safe-Zone des adaptiven Icons, dunkler Hintergrund aus `colors.xml`). Bei neuem Logo neu generieren lassen. |

Es zählt der Dateiname vor der Endung (`wordmark.*`) – Datei ersetzen,
pushen; der nächste Build nimmt sie automatisch mit.

## Architektur (Kurzüberblick)

| Paket | Zweck |
|---|---|
| `core` | Einstellungen (DataStore), Abspiel-Historie (Wiederholsperre), Wunsch-Persistenz, Zeitformatierung |
| `net` | Spotify Web API (OkHttp + kotlinx.serialization) |
| `auth` | OAuth 2.0 PKCE mit Loopback-Redirect (`http://127.0.0.1:8888/callback`), Token-Verwaltung |
| `engine` | Kernlogik: Polling von Playback + Queue, Wunsch-Lebenszyklus, Regeln (Sperrzeit, Maximallänge) |
| `kiosk` | Immersive Mode, Lock-Task/Pinning, Device-Owner-Receiver |
| `ui` | Jetpack Compose: Besucheransicht, Suche, Veranstaltermenü |

Alle Grenzwerte (Anzahl Wunschboxen, Sperrminuten, maximale Songlänge,
Poll-Intervall) sind Einstellungen mit Defaults in
[SettingsRepository.kt](app/src/main/java/de/ichdj/jukebox/core/SettingsRepository.kt),
nichts ist hart verdrahtet.

## Bekannte Grenzen (Spotify-Web-API-bedingt)

- **Wünsche landen automatisch richtig**: Spotify spielt manuell eingereihte
  Tracks („Queue") vor der Playlist-Fortsetzung – genau das gewünschte
  Verhalten. Die App kann die Queue aber nicht umsortieren oder Einträge
  entfernen; das kann nur der Master (und die App spiegelt es, wie
  spezifiziert).
- Die Wunschboxen sind **pro Tablet** lokal. Bei mehreren Tablets sieht jedes
  Tablet die Wünsche der anderen zwar in der Queue, aber nicht farbig
  markiert/nummeriert (dafür bräuchte es einen gemeinsamen kleinen Server –
  möglicher Ausbau).
- Wünsche funktionieren nur, solange auf dem Master **aktiv** abgespielt wird
  (API-Vorgabe „kein aktives Gerät" → Hinweis an den Gast).
- Die Queue-Abfrage liefert maximal ~20 kommende Titel.
