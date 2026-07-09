# Spotify "I Can Haz DJ" - App (IchDJ)

Die ist eine Beschreibung der "I can Haz DJ"-App (kurz "IchDJ"), die entwickelt werden soll. Sie füllt eine Lücke, die Spotify nicht originär anbietet. Gemeinsames Hörerlebnis kann bisher dynamisch über Spotify Jam erreicht werden - allerdings brauchen alle Beteiligten einen Sporify Account. Das ist nicht besodners gut geeignet für eine größere Musikveranstaltung, in der vorberietete Playlists laufen und die Gäste sich an einem feststehenden Panel unbabhängig vom eigenen Mobilgerät, Musik wünschen können. Die IchDJ-App soll das Problem lösen.

## Grundlegende Use-Cases
Es soll folgendes Setup gelten: Spotify läuft als originäre Anwendung mit einem eingeloggten Premium Account bspw. auf einem Laptop und spielt den Master ab. Dieser ist nur für den Veranstalter zugänglich. Es gibt ein oder mehrere andere Android Geräte (beispielsweise fest eingebaute Tablets) mit eigener Internetverbindung die mit dem selben Account eingeloggt sind. Hier ist die orgiginale Spotify App **nicht** installiert. Vielmehr dient die IchDJ App auf den Tablets als eine Fernsteuerung mit sehr eingeschränkten Möglichkeiten (dazu später mehr) Die zu entwickelnde App hat zwei Hauptzwecke für Besucher des Musikevents:

1. Anzuzeigen, was in der Queue ist und als nächstes gespielt wird
2. Musikwünsche entgegenzunehmen

Für den Veranstalter ergeben sich weitere Use-Cases:

1. Das Minimieren der App unterbinden, damit das Tablet ausschließlich als Eingabe und Infopanel genutzt werden kann von den Besuchern (Kiosk-Mode)
2. Einstellungen vornehmen wie Anzahl der Wünsche, Dauer der Sperre bevor ein Track nochmal gespielt wird, Wünsche aktivieren deaktivieren und natürlich die App beenden.
3. Die Einstellungen effizient vor den Besuchern zu schützen

## Aufbau der App
Horizontale Austrichtung erzwingen. Die App startet im Besucher-Modus direkt im Kiosk-Mode. 

In der oberen Zeile ist der Logo-Schriftzug "I can haz DJ?" mit dem ikonischen Katzenkopf zu sehen (erstmal dummy svg).

Der Bildschirm ist in der horizontalen Mitte geteilt. Links befinden sich leere, rechteckige Boxen übereinander für die Wünsche - default Anzahl: 4 

Rechts sieht man die aktuelle Queue.

**Kiosk-Mode**:
Es sind sofort alle normalen Android Funktionen gesperrt: Die Navigationsleiste ist ausgeblendet und kann nicht eingeblendet werden und auch das Streichen von den Bildschirmrändern castet kein normales Android Menü. es ist aus der Besucheransicht nicht möglich, die App zu verlassen, außer durch Ausschalten (sekundenlanges Drücken des Power Buttons).

Wenn man in kurzem Abstand 5 Mal hintereinander auf das Logo tippt, erscheint das Veranstalter-Setting Menü. Dieses benutzt als Sperre die GErätesperre (Muster/ PIN/ Fingerabdruck, oder nichts, wie auch immer das Gerät geschützt ist). Scheitert das entsperren kommt man nach einer MEldung zurück in die Besucheransicht

### Die Wunschboxen
Die Wunschboxen sind initial leere Boxen in denen nur steht "Wünsch dir was..." Wenn man darauf klickt, kann man wie gewohnt bei Spotify Textbasiert songs suchen. NUr songs, keine Alben. Man kann auf einen Song klicken und muss dann noch einmal bestätigen, da Songwünsche nicht reversibel sind.

Ist ein Song gewünscht, erscheint er mit Cover-Art, Titel, Interpret und Abspiellänge in der Box, die jetzt nicht mehr zum Wünschen genutzt werden kann, bis der Song gespielt wurde. Dann wird sie wieder frei.

### Die Queue
Die akteulle Queue wird rechts angezeigt. Alle Tracks ebenfalls mit Cover-Art Song Titel, Artits name und Spiellänge. Der aktuell spielende Song ist oben und wird, wenn er im Master tatsächlich gespielt wir im Cover mit einem play button überlagert. Außerdem muss er hervorgehoben sein, damit man ihn als spielenden Song erkennt. Die Songs darunter sind die, die in der Queue als nächstes kommen. 

### Das Veranstaltermenü

Das Veranstaltermenü hat die Möglichkeiten sich mit einem Spotify-Account zu verbinden. Ist das Veranstaltermenü entsperrt, dann kann die App auch wie gewohnt verlassen und geschlossen werden, was nötig ist, um sich mit dem Spotify Account OAuth technisch zu verifizieren. Weiter können dort einstellungen vorgenomen werden: Wieviele Wunschboxen es gibt, wie lange Tracks gesperrt sind, nachdem sie gespielt wurden. Die maximale Länge der wünschbaren Tracks und der Spotify Account kann antürlich auch wieder getrennt werden. Wir das Veranstalermenü geschlossen, ist automatisch wieder der Kiosk Mode für Besucher an.

## Verhalten der Elemente

Gewünschte Songs bekommen eine Nummer. Sind noch alle Wünsche leer, und man setzt den ersten Wunsch ab, dann ist das Wunsch Nummer 1. Diese Zahl muss promienent in der Wunschbox zu sehen sein. Die Wunschbox mit den kleinsten Nummern rutschen automatisch nach oben, die frieen oder die mit höheren NUmmern nach unten.

Sobald ein Wunsch abgegeben wurde, wird er hinter dem aktuell spielenden Song eingereiht und hinter den anderen gegebnenenfalls vorher bereits abgegebenen Wünschen, aber immer vor den Songs die in der Queue stehen, die keine Wünsche sind.

Wünsche werden in der Queue farbig hell hinterlegt und entsprechen damit der Optik der Boxen. Die anderen Songs, die keine Wünsche sind, sind dunkel hinterlegt.

Die Wünsche haben ihre Wunschnummer auch in der Queue. Sowohl für Wünsche als auch für alle Songs in der QUeue gilt: Es wird die Abspiellänge UND die Uhrzeit angezeigt, wann der Song anfängt. Angebrochene Minuten werden dabei immer abgerundet, so dass man zu einem Song da sein und sicher den Anfang hören kann. Der spielende Song zeigt an der Stelle die Restlaufzeit und da wo die Startzeit währe steht "jetzt"

Spielzeit und Startzeit werden auch in den Wunschboxen angezeigt. Spielt ein wunsch steht dort ebenfalls Restlaufzeit und "jetzt" statt der Startzeit. Ein Wunsch der spielt hat keine Nummer mehr stattdessen ein sanft blinkendes Playzeichen. Während ein Wunsch spielt, hat er intern die Nummer 0, diese wird aber nicht angezeigt. DEr nächste Wunsch hat dann 1 usw. 

Leere Wunschboxen haben niemals Nummern.

Songs in der Queue die nicht mehr gespielt werden verschwinden. Auch die gespielten Wünsche verschwinden und machen Platz für leere Wunschboxen und lassen die anderen nach oben rücken und kleinere Nummern bekommen.

Belegte WUnschboxen lassen sich nicht mehr anklicken. Sind alle Wunschboxen belegt steht über ihnen dauerhaft ein kleiner Hinweis: "Gerade keine Wunschplätze mehr frei, warte bis der nächste Wunsch gespielt wurde."

Wünscht man sich einen Song der nicht gespielt werden darf, weil er entweder vor kurzem gespielt wurde oder weil er zu lang ist sollte ein entsprechender Hinweis erscheinen. Wenn der Song beriets gespielt wurde soll die Restwartezeit und die Uhrzeit in dem Hinweis erscheinen, ab wann der Song wieder gespielt werden darf.

## Inferenz mit der Master App
Die Master App ist die originäre Spotify app sie wird daher die wiedergabliste als Standard Queue sehen und kann sie Manipulieren. Verschiebt sie die Anordnung der Wünsche, so muss sich das in den Zahlen und der Rangfolge der Wünsche der Besucher App niederschlagen. Aber die Besucher-App stellt die Reihenfolge in diesem Fall nicht wieder her.

Entfernt die MAster App einen Track, verschwindet auch der Wunsch.

Ein Track gilt für die Besucher App erst dann als für eine Zeit gesperrt, wenn tatsächlich die ersten Sekunden liefen.

## Coding Richtlinien
Der letzendliche Funktionsumfang und der Bedarf der Einstellungen steht noch nicht fest. Alles soll möglichst wart- und erweiterbar, modular und objektorientiert aufgebaut werden. Möglichst nichts hard-wiren sondern durch settings und Einstellungsdateien konfigurierbar lassen.