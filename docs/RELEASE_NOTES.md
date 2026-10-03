# Release Notes

## Visotaris OPMod 1.2.0-pre.4 — Vorabversion (aktuelle Entwicklungsfassung)

Diese Fassung baut auf dem gesicherten pre.3-Stand auf und bleibt ausdrücklich eine
Vorabversion. Zielplattform ist Minecraft 26.x (aktuell 26.2); ein stabiler 1.2.0-
Release oder Release-Tag wird hierdurch nicht erstellt.

### Änderungen in pre.4

- Auktions-Liveupdates sind bei Neuinstallationen AUS. Ein bewusster Refresh lädt
  Kategorien und `/auctions/active`; erst die gespeicherte gemeinsame Einstellung
  oder `VISOTARIS_AUCTIONS_STREAM_DEV=true` startet danach SSE.
- Die SSE-Verbindung fordert `text/event-stream` an, hat kein kurzes JSON-Read-Timeout,
  ignoriert Keepalive-Kommentare und verarbeitet Events über die fachliche Auction-UID.
- Gebote und Updates führen vollständige bzw. zusammengeführte Snapshots derselben UID;
  `endTime` wird aktualisiert. Abschlussereignisse verlassen den aktiven Bestand, ihre
  Details bleiben begrenzt für spätere lokale Historie verfügbar.
- Ingame- und geschützte Web-Einstellungen schreiben denselben ConfigManager-Wert;
  Ingame-Änderungen bleiben bis zum ausdrücklichen Speichern Entwurf.

### Vor dem stabilen 1.2.0-Release prüfen

- Alle neuen Eventtypen und Header-/Config-Fälle automatisiert testen.
- Manuelle Web-/Client-Aktualisierung, Live-Stream und Cache-Synchronität praktisch
  prüfen; tatsächlich beobachtete und nur simulierte Events getrennt dokumentieren.
- Cachepersistenz, Spielerprofilnamen und API-Icons im Minecraft- und Web-Client
  praktisch verifizieren, sobald diese Pre.4-Teile fertiggestellt sind.

---

## Visotaris OPMod 1.2.0-pre.3 — gesicherter Vorabstand

Diese Entwicklungsfassung bereitet Visotaris OPMod 1.2.0 vor. Sie ist **kein stabiler Release**. Visotaris 1.2.0+ unterstützt Minecraft 26.x (derzeit 26.2). Ältere veröffentlichte 1.1.x-Versionen behalten ihre damalige Kompatibilität.

### Im pre.3-Stand enthalten

### Korrekturen für pre.3

- Aktiver 1.21.11-Support entfernt: ein 26.x-Build, Quellen unter `src/main/`, erhaltene Service-/API-Tests unter `src/test/`; keine doppelten Render-/Source-Adapter oder alten MaLiLib-Buildpfade.
- Itemicons verwenden die Runtime-Item-Registry der geladenen Welt. Im Hauptmenü werden neutrale Platzhalter ohne ItemStack-Erzeugung verwendet; Markt-, Auktions- und Händlerdaten sowie Einstellungen bleiben erreichbar.
- Getrennter AuctionCache mit Initial-Snapshot, SSE, Last-Event-ID, Reconnect und `stream.reset`; lokale Auktionssuche/Filter/Sortierung und lesende Web-Endpunkte. Siehe [AUCTIONS.md](AUCTIONS.md).

- Ingame-Einstellungen und Alarmregeln werden in einem isolierten Entwurf bearbeitet. „Speichern“ schreibt diesen über den ConfigManager atomar; „Zurück“/Escape fragen bei Änderungen nach Speichern, Verwerfen oder Weiterbearbeiten. Fehler beim Schreiben bleiben sichtbar und veröffentlichen den Entwurf nicht als Live-Konfiguration.
- Zurück-/Speichern-Aktionen bleiben außerhalb scrollender Inhalte erreichbar. Netzwerk- und Discord-Unterseiten teilen denselben Entwurf; ihre eigenen Speichern-/Zurück-Aktionen führen ebenfalls keine stillen Sofortspeicherungen aus.
- Das Speichern von Konfigurationswerten startet keine sofortigen Markt-/Händlerabrufe. Netzwerkänderungen übernehmen den neuen Client und warten bis zum nächsten regulären Intervall; „Jetzt aktualisieren“ bleibt die ausdrücklich manuelle Refresh-Aktion.
- Steinplatten verwendet nun das manuell vorbereitete Quellbild `docs/ux/new-steinplatten.png`, als transparente 32×32-Laufzeitressource und eindeutig an den OPShards-Cache-Key `paper#635` gebunden.
- Das native Menü öffnet sich nun über MaLiLib (Standard Alt+V) statt eines Fabric-Menü-Keybinds. Eine vorhandene alte Einzeltasten-Belegung wird beim ersten Start übernommen, sofern noch keine MaLiLib-Belegung gespeichert ist.

- Native A+C-Ingame-UX mit linker Navigation und den Bereichen Übersicht, Markt, Shard & Händler, Tooltips, Preisalarme, Schutz & Komfort und System sowie Auktionshaus. Sie nutzt den vorhandenen konfigurierbaren Öffnen-Keybind und läuft ohne Webview.
- Markt- und Kursansichten lesen bestehende lokale Cache-Snapshots. Suche, Kategorien, Preise und **aktive Auftragszahlen** werden angezeigt; diese Zahlen sind keine abgeschlossenen Handelsvolumina. Fehlende und veraltete Daten werden gekennzeichnet.
- Häufige Anzeige- und Schutzoptionen verwenden denselben ConfigManager wie die übrigen Oberflächen. Ein ausdrücklich betätigter Refresh nutzt die vorhandenen Sync-Dienste; Menüöffnung und Navigation lösen keine zusätzlichen Anfragen aus.
- Preisalarme lassen sich nun auch ingame anlegen, bearbeiten, pausieren und bestätigt löschen. Beide Oberflächen nutzen dieselben Regeln, dieselbe Validierung und HUD/Web-Benachrichtigungen. Charts bleiben in der lokalen Web-UX.
- Markt und Händlerkurse bieten lokale Suche, Filter und Sortierung. OPSUCHT-spezifische Shardmaterialien zeigen ihren API-Anzeigenamen statt des Vanilla-Trägermaterials Papier. Das Fenster nutzt stärkere Marine-/Eisblau-Flächen und einen gemessenen Footer ohne konkurrierende Texte.
- Ein Minecraft-26.x-Renderer extrahiert die Renderzustände. Historische 1.21.11-Aufnahmen und aktuelle Prüfnachweise: [Ingame-UX](ux/implemented/README.md).

## Vor dem stabilen 1.2.0-Release prüfen

- Bedienung und Skalierung bei verschiedenen GUI-Größen, Auflösungen und Eingaben praktisch testen.
- Cache-Anzeigen, Datenalter, Leerzustände und gemeinsame Einstellungen im laufenden Client prüfen.
- Hauptmenü, lokale Welt, Rückkehr, Seitenwechsel und Auktionssuche wurden unter Minecraft 26.2 praktisch geprüft. Nachweise und offene Transportpunkte: [26.2-Prüfung](ux/implemented/26.2-mainmenu-registry-check.md).
- Windows-Laufzeitprüfung nachholen.

Die Projektversion wird zentral in `gradle.properties` als `mod_version` gesetzt. Gradle ergänzt im Artefakt und in den Fabric-Metadaten `+mc26.2`; Versionsanzeigen lesen die Metadaten. Ein Wechsel auf `1.2.0` ohne Pre-Suffix erfolgt erst nach den praktischen Prüfungen.
