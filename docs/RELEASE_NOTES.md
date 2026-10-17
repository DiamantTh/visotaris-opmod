# Release Notes

## Visotaris OPMod 1.2.0-pre.8 — Vorabversion

### Native Auktionshaus- und Tooltip-UX

- Auktionszeilen teilen Gebote und Sofortkäufe in eine rechtsbündige Preisspalte
  mit Innenabstand zur Kartenkante auf. Bei schmalen Inhaltsbereichen wechseln die
  Zeilen in ein gestapeltes Layout; lange Namen und Preise werden kontrolliert gekürzt.
- Auktionsdetails gliedern Preise, Gebote, Beschreibung, Verzauberungen und
  Auktionsdaten mit kompakten Abständen. Mehrzeilige Lore bleibt als eigener Text
  lesbar.
- Verzauberungen verwenden deutsche Minecraft-Sprachressourcen und die üblichen
  römischen Stufen. Itemnamen, die nur eine Vanilla-Material-ID wiederholen, werden
  ebenfalls mit dem deutschen Minecraft-Namen angezeigt.
- Auktionskategorien, Status und Zeitangaben werden als deutsche, lesbare Werte
  statt als Registry-/API-Schlüssel dargestellt.

## Visotaris OPMod 1.2.0-pre.7 — Vorabversion

### Korrekte Währungssemantik

- Marktpreise und Auktionspreise werden in nativer UX und WebUX als Dollarwerte
  mit vorangestelltem `$` formatiert. Auktionskategorien bestimmen dabei nie die
  Währung eines Gebots.
- Merchant-API-Targets bleiben intern `opshards` und `redcoins`; sichtbar heißen
  sie vollständig `OPShards` beziehungsweise `Redcoins`. Für beide wird kein
  eigenes Währungssymbol erfunden.
- Unbekannte künftige Merchant-Targets behalten einen lesbaren API-Fallback.

### WebUX-Lieferkette

- Das transitive Build-Werkzeug `source-map-js` wurde von 1.2.1 auf den
  kompatiblen Sicherheits-Patch 1.2.2 aktualisiert (GHSA-68fv-2mgg-jv7q).
  Der anschließende `npm audit` meldet keine High- oder Critical-Findings.
- Der History-Einstieg bleibt ohne ECharts klein; Charts laden den weiterhin
  separaten `historyCharts`-Chunk erst bei vorhandenen Verlaufsdaten. Benannte
  ECharts-Core-Imports vermeiden dabei unnötige Namespace-Exporte.
- Der WebUI-Build entfernt nach erfolgreicher Ausgabe ausschließlich veraltete
  erzeugte JS-/CSS-/HTML-Bundles. Mitgelieferte Fonts und andere statische Ressourcen
  bleiben erhalten.
- Statische WebUX-Ressourcen werden beim Build zusätzlich als Brotli- und
  Zstandard-Dateien erzeugt. Der lokale Webserver wählt `br`, danach `zstd`,
  sonst die unveränderte Originalressource; gzip wird bewusst weder erzeugt
  noch ausgeliefert. Die Aushandlung setzt `Vary: Accept-Encoding` und
  komprimiert nie erst zur Laufzeit.

## Visotaris OPMod 1.2.0-pre.6 — Vorabversion

Diese Vorabversion baut auf pre.5 auf. Visotaris bleibt auf Minecraft 26.x
(aktuell 26.2) ausgerichtet; dies ist kein stabiler 1.2.0-Release.

### WebUX: Itemnamen in der aktiven Minecraft-Sprache

- Vanilla-Itemnamen in den Web-Ansichten folgen der ausgewählten Sprache des
  laufenden Minecraft-Clients. `/api/meta` stellt dafür `clientLanguage` bereit;
  der gemeinsame `WebItemNameResolver` verwendet Minecrafts Sprachressourcen.
- Mit `de_de` erscheinen beispielsweise `Dirt` als `Erde`, `Ender Pearl` als
  `Enderperle`, `Carrot` als `Karotte`, `Deepslate` als `Tiefenschiefer`,
  `Spruce Log` als `Fichtenstamm` und `Beacon` als `Leuchtfeuer`. Mit einem
  englischen Client werden die englischen Vanilla-Namen verwendet.
- OPSUCHT-Custom-Items behalten ihren API-Namen. Suche und Namenssortierung
  verwenden die sichtbaren lokalisierten Namen; die Suche löst keine Requests aus.
- Diese Änderung betrifft die WebUX-Namensdarstellung, nicht das Verhalten der
  nativen Minecraft-UX.

### Native Navigation

Die sichtbare Reihenfolge lautet:

`Übersicht → Auktionshaus → Markt → Preisalarme → Schutz & Komfort → Shard & Händler → System → Tooltips`

Die fachlichen Page-IDs bleiben stabil; nur die separate `NAVIGATION_ORDER` legt
die sichtbare Reihenfolge fest. Direkte Seitenwahl, aktive Markierung,
Scrollzustände und MaLiLib-Rücknavigation bleiben an ihre Page-IDs gebunden.
Der Vorwärts- und Rückwärtswechsel durch alle Seiten, die Auktionshaus-Wechsel,
MaLiLib-Escape und der sichtbare Zurück-Button wurden im isolierten Client geprüft,
auch bei kleiner Fenstergröße. Details stehen im [pre.6-Navigationsprüfbericht](ux/implemented/pre6-navigation-check.md).

## Visotaris OPMod 1.2.0-pre.5 — Vorabversion

- Der MaLiLib-Hotkeyeditor zeigt in der Mod-Auswahl jetzt den Anzeigenamen
  „Visotaris OPMod“ und nicht mehr die technische Mod-ID.
- Der Editor kehrt per Esc oder über den sichtbaren Button „Zurück zu Visotaris“
  zur genau zuvor geöffneten Visotaris-Seite zurück.
- Der Menüshortcut bleibt über MaLiLib konfigurierbar, speicherbar und deaktivierbar.
  Eine vorhandene eigene Belegung wird bei der Migration nicht überschrieben; eine
  absichtlich leere Belegung bleibt deaktiviert.

## Visotaris OPMod 1.2.0-pre.4 — Vorabversion (historischer Entwicklungsstand)

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

### Zum pre.4-Prüfzeitpunkt noch offen (historisch)

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

## Zum pre.3-Prüfzeitpunkt noch offen (historisch)

- Bedienung und Skalierung bei verschiedenen GUI-Größen, Auflösungen und Eingaben praktisch testen.
- Cache-Anzeigen, Datenalter, Leerzustände und gemeinsame Einstellungen im laufenden Client prüfen.
- Hauptmenü, lokale Welt, Rückkehr, Seitenwechsel und Auktionssuche wurden unter Minecraft 26.2 praktisch geprüft. Nachweise und offene Transportpunkte: [26.2-Prüfung](ux/implemented/26.2-mainmenu-registry-check.md).
- Windows-Laufzeitprüfung nachholen.

Die Projektversion wird zentral in `gradle.properties` als `mod_version` gesetzt. Gradle ergänzt im Artefakt und in den Fabric-Metadaten `+mc26.2`; Versionsanzeigen lesen die Metadaten. Ein Wechsel auf `1.2.0` ohne Pre-Suffix erfolgt erst nach den praktischen Prüfungen.
