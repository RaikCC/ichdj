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
- als **Release "latest"** mit stabiler Download-URL:
  `https://github.com/<USER>/<REPO>/releases/download/latest/ichdj.apk`
  – diese URL kann man direkt am Tablet im Browser öffnen.

### Einmalige Einrichtung auf GitHub

```
# Auf github.com ein neues (privates oder öffentliches) Repo anlegen, dann:
git remote add origin https://github.com/<USER>/<REPO>.git
git push -u origin main
```

Mehr ist nicht nötig – Actions ist standardmäßig aktiv. Der erste Lauf dauert
ca. 5–8 Minuten. GitHub-Free reicht aus (öffentliche Repos: unbegrenzte
Build-Minuten, private: 2000 min/Monat).

### Signatur / Keystore

`signing/ichdj-release.jks` ist ein reiner **Sideload-Keystore** (Passwort
`ichdj-sideload`, bewusst im Repo): Er sorgt nur dafür, dass alle Builds
dieselbe Signatur tragen und Updates ohne Deinstallation installierbar sind.
Er schützt keinerlei Geheimnisse und ist NICHT für einen Play-Store-Release
gedacht. Fehlt er, erzeugt der CI-Lauf ihn automatisch neu und committet ihn.

## Spotify einrichten (einmalig)

1. Auf https://developer.spotify.com/dashboard eine App anlegen
   (der Premium-Account des Veranstalters genügt).
2. Bei **Redirect URIs** exakt eintragen: `http://127.0.0.1:8899/callback`
   (Loopback-Adresse; von Spotify seit den Sicherheitsänderungen 2025
   ausdrücklich erlaubt – eine eigene Domain ist nicht nötig).
3. Als API **Web API** auswählen und die **Client ID** kopieren.
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

Rückgängig: `adb shell dpm remove-active-admin de.ichdj.jukebox/.kiosk.IchDjAdminReceiver`
oder Werksreset.

Das Veranstaltermenü (5× aufs Logo tippen) wird über die Gerätesperre des
Tablets geschützt (PIN/Muster/Fingerabdruck – ohne Sperre ist es frei
zugänglich).

## Lokal bauen (optional)

Es wird bewusst **keine** systemweite Build-Umgebung benötigt. Für lokale
Builds liegt eine komplette Toolchain (JDK 17, Android SDK, Gradle-Caches)
ausschließlich unter `%LOCALAPPDATA%\ichdj-build` (~3 GB). **Aufräumen =
diesen einen Ordner löschen.**

```powershell
$env:JAVA_HOME = "$env:LOCALAPPDATA\ichdj-build\jdk"
$env:GRADLE_USER_HOME = "$env:LOCALAPPDATA\ichdj-build\gradle-home"
.\gradlew.bat assembleRelease
# Ergebnis: app\build\outputs\apk\release\app-release.apk
```

## Architektur (Kurzüberblick)

| Paket | Zweck |
|---|---|
| `core` | Einstellungen (DataStore), Abspiel-Historie (Wiederholsperre), Wunsch-Persistenz, Zeitformatierung |
| `net` | Spotify Web API (OkHttp + kotlinx.serialization) |
| `auth` | OAuth 2.0 PKCE mit Loopback-Redirect (`http://127.0.0.1:8899/callback`), Token-Verwaltung |
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
