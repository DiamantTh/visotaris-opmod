# Visotaris OPMod 1.2.0-pre.3 — Vorabversion

Diese Entwicklungsfassung bereitet Visotaris OPMod 1.2.0 vor. Sie ist **kein stabiler Release**. Die unterstützten Minecraft-Versionen sind weiterhin 1.21.11 und 26.x (derzeit 26.2); sie sind unabhängig von der Mod-Version.

## Neu in der Vorabversion

### Korrekturen für pre.3

- Ingame-Einstellungen und Alarmregeln werden in einem isolierten Entwurf bearbeitet. „Speichern“ schreibt diesen über den ConfigManager atomar; „Zurück“/Escape fragen bei Änderungen nach Speichern, Verwerfen oder Weiterbearbeiten. Fehler beim Schreiben bleiben sichtbar und veröffentlichen den Entwurf nicht als Live-Konfiguration.
- Zurück-/Speichern-Aktionen bleiben außerhalb scrollender Inhalte erreichbar. Netzwerk- und Discord-Unterseiten teilen denselben Entwurf; ihre eigenen Speichern-/Zurück-Aktionen führen ebenfalls keine stillen Sofortspeicherungen aus.
- Das Speichern von Konfigurationswerten startet keine sofortigen Markt-/Händlerabrufe. Netzwerkänderungen übernehmen den neuen Client und warten bis zum nächsten regulären Intervall; „Jetzt aktualisieren“ bleibt die ausdrücklich manuelle Refresh-Aktion.
- Steinplatten verwendet nun das manuell vorbereitete Quellbild `docs/ux/new-steinplatten.png`, als transparente 32×32-Laufzeitressource und eindeutig an den OPShards-Cache-Key `paper#635` gebunden.
- Das native Menü öffnet sich nun über MaLiLib (Standard Alt+V) statt eines Fabric-Menü-Keybinds. Eine vorhandene alte Einzeltasten-Belegung wird beim ersten Start übernommen, sofern noch keine MaLiLib-Belegung gespeichert ist.

- Native A+C-Ingame-UX mit linker Navigation und den Bereichen Übersicht, Markt, Shard & Händler, Tooltips, Preisalarme, Schutz & Komfort und System. Sie nutzt den vorhandenen konfigurierbaren Öffnen-Keybind und läuft ohne Webview.
- Markt- und Kursansichten lesen bestehende lokale Cache-Snapshots. Suche, Kategorien, Preise und **aktive Auftragszahlen** werden angezeigt; diese Zahlen sind keine abgeschlossenen Handelsvolumina. Fehlende und veraltete Daten werden gekennzeichnet.
- Häufige Anzeige- und Schutzoptionen verwenden denselben ConfigManager wie die übrigen Oberflächen. Ein ausdrücklich betätigter Refresh nutzt die vorhandenen Sync-Dienste; Menüöffnung und Navigation lösen keine zusätzlichen Anfragen aus.
- Preisalarme lassen sich nun auch ingame anlegen, bearbeiten, pausieren und bestätigt löschen. Beide Oberflächen nutzen dieselben Regeln, dieselbe Validierung und HUD/Web-Benachrichtigungen. Charts bleiben in der lokalen Web-UX.
- Markt und Händlerkurse bieten lokale Suche, Filter und Sortierung. OPSUCHT-spezifische Shardmaterialien zeigen ihren API-Anzeigenamen statt des Vanilla-Trägermaterials Papier. Das Fenster nutzt stärkere Marine-/Eisblau-Flächen und einen gemessenen Footer ohne konkurrierende Texte.
- Beide Minecraft-Builds verwenden dieselben Inhalte und eine versionsspezifische Render-Brücke. Screenshots der tatsächlich laufenden 1.21.11-Ansichten: [Ingame-UX](ux/implemented/README.md).

## Vor dem stabilen 1.2.0-Release prüfen

- Bedienung und Skalierung bei verschiedenen GUI-Größen, Auflösungen und Eingaben praktisch testen.
- Cache-Anzeigen, Datenalter, Leerzustände und gemeinsame Einstellungen im laufenden Client prüfen.
- Minecraft 26.2 ebenfalls praktisch starten und die native UX dort prüfen.
- Windows-Laufzeitprüfung nachholen.

Die Projektversion wird zentral in `gradle.properties` als `mod_version` gesetzt. Gradle ergänzt im Artefakt und in den Fabric-Metadaten `+mc1.21.11` beziehungsweise `+mc26.2`; Versionsanzeigen lesen die Metadaten. Ein Wechsel auf `1.2.0` ohne Pre-Suffix erfolgt erst nach den praktischen Prüfungen.
