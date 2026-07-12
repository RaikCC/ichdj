## Settings Menü
* Die Sperrung funktioniert grundsätzlich. Wenn ein bspw. ein Muster abgefragt werden soll geht das Tablet in Display Lock, dann muss das Muster zweimal eingegeben werden (einmal für die App, dann anscheinend für den Display Lock). Das ist schlecht weil die App gesperrt bleibt, wenn der Nutzer das Muster nicht kennt. Es soll dann aber nicht gesperrt sein sondern einfach wieder in die Besucheransicht wechseln

* Die Optionen die als Zahlen gesetzt werden sollen eine Numpad eingabe erlauben, so dass auch andere Zahlen als die in den definierten Sprüngen erreichbare eingegeben werden können.

## Besucheransicht

* Wenn der Schalter im Settingsmenü keine Wünsche zulässt, sollen auch keine Wunschboxen zu sehen sein. Der Text "Wünsche sind gerade deaktiviert." muss dann als einziges prominent, in größerer Schriftart in der Mitte stehen, dort wo die Wunschboxen sonst stehen.

* Die Spiellänge der Tracks soll nur noch bei dem Spielenden Track angezeigt werden (in der Queue und in der Wunschbox), bei den anderen wird sie nicht mehr angezeigt

* Die Uhrzeiten in den Wunschboxen und in der Queue sollen alle das Wort "Uhr" hinter der Uhrzeit bekommen, also "23:15 Uhr" statt "23:15" usw.
Das Wort "jetzt" bleibt natürlich ohne "Uhr"

* Die weißen Hintergründe sollen verschwinden. Alle Boxen un Queue Items haben den dunklen Hintergrund wie die bisher leeren Boxen /Queue Items. Die grafische Zuorodnung Playing/ Wunschitem und Queue-Item findet statt mit dem Hintergrund mit den Umrandungsfarben der Items statt (siehe unten: Design)

## Design

* Überarbeitung des Farbschemas der App, folgende sind die Designfarben in Hex:

{"Neon Ice":"2de2e6","Electric Rose":"f6019d","Indigo":"650d89","Dark Amethyst":"261447","Midnight Violet":"0d0221"}

https://coolors.co/2de2e6-f6019d-650d89-261447-0d0221

Das spielende Lied in der Queue wird mit neon ice umrandet, die schriftfarbe von "jetzt" ebenfalls. Genau so wie alles das bisher grün war (mit spotify verbundnen" in den settings) 

Gefüllte Wünsche in der Wunschbox und in der Queue sollen Electric Rose Ränder bekommen.

Steuerelemente, die bisher Orange waren, sollen Indigo werden.

Dark Amethyst und Midnight Violet erstmal keine Verwendung, Hintergrunde bleiben wie sie sind, ausser der helle, der ersatzlos verschwindet (siehe auch Abschnitt "Besucheransicht")

* Logo und Schriftzug: Sollen in App eine SVG sein (wenn nocht nicht so eingerichtet) Soleln in der Besucheransicht mittig zentriert sein. Bitte vor dem nächsten Build einmal sagen wo diese Assets abgelegt sind, damit ich diese in der richtigen Variante hinterlegen kann. Ebenfalls das App-Logo.

* ebenfalls Logo und Schriftzug: Jetzt wird es manchmal oben abgeschitten in der Besucheransich, manchmal nicht. (Die KAtzenohren sind manchmal auf der Höhe des Schrftzuges "abrasiert") Es muss sichergestellt sein, dass es immer vollständig zu sehen ist