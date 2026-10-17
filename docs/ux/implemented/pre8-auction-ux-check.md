# Auktionshaus- und Tooltip-UX: pre.8

Die native Auktionsansicht wurde in einem isolierten Minecraft-26.2-Entwicklungsclient mit Xvfb auf Display `:113` geprüft. Der Client lief auf einem temporären Spielverzeichnis und mit GUI-Skalierung 2. Die Testdaten waren ein lokaler Cache-Snapshot mit mehreren Auktionen; es wurde kein Server für die UI-Prüfung benötigt und keine schreibende Auktionsaktion ausgeführt.

| Prüffall | Ergebnis | Aufnahme |
| --- | --- | --- |
| Großes Fenster, 1600×900 | Rechte Preisspalte mit Innenabstand; langer Name und Verkäufer werden in der linken Spalte gekürzt; Gebot und Sofortkauf bleiben getrennte, rechtsbündige Zeilen. | [Auktionsliste](26.2-auctions-pre8-wide.png) |
| Tooltip mit langer Beschreibung und mehreren Verzauberungen | Abschnitte für Preise, Gebote, Beschreibung, Verzauberungen und Auktionsdaten; Lore-Absätze bleiben getrennt. | [Tooltip](26.2-auctions-pre8-tooltip.png) |
| Kleineres Fenster, 740×430 | Zeilen und Tooltip bleiben innerhalb des sichtbaren Fensters. | im Client geprüft |
| Schmale Fenstergröße, 420×430 | Preisangaben wechseln unter Name und Verkäufer; Text wird kontrolliert gekürzt und bleibt innerhalb der Kartenbreite. | [Schmale Auktionsliste](26.2-auctions-pre8-narrow.png), [schmaler Tooltip](26.2-auctions-pre8-narrow-tooltip.png) |
| Lange Namen und Verkäufer sowie Gebot plus Sofortkauf | Alle Fälle waren im Cache-Snapshot enthalten und bei großer sowie schmaler Breite sichtbar. | in den Listenaufnahmen |
| Aktive Clientsprache `en_us` | Verzauberungen, Kategorie, Status, Zeitangaben und Vanilla-Itemname in der Auktionsdarstellung blieben deutsch; UI-Texte außerhalb dieser Auktionsanzeige folgten weiter der aktiven Minecraft-Sprache. | [Tooltip mit `en_us`](26.2-auctions-pre8-tooltip.png) |

Die Aufnahmen stammen aus dem laufenden Client und zeigen die native Minecraft-Schrift und das Visotaris-Farbschema. Die Auktionsdaten sind Test-Fixtures im lokalen Cache, keine Live-Auktionen. Die Prüfung deckt Layout und Darstellung ab; sie bewertet weder Serverdaten noch Providerverhalten.
