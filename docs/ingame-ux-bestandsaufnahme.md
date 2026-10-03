# Ingame-UX: Bestandsaufnahme und Konzept (Entscheidungsvorlage)

Stand: 27. September 2026. Der erste technische Bestandsstand und die inzwischen umgesetzte A+C-Hybrid-Oberfläche sind hier zusammen dokumentiert. Die UX-Referenz liegt in [`ux/visotaris-ingame-ux-konzepte.pdf`](ux/visotaris-ingame-ux-konzepte.pdf); Beispielzahlen im Konzept-PDF sind keine Produktdaten. Screenshots der laufenden Implementierung liegen unter [`ux/implemented/`](ux/implemented/README.md).

## Tatsächlich verfügbare Bausteine

| Bereich | Ist-Zustand | Eignung im neuen Menü |
| --- | --- | --- |
| Markt | `MarketCache` hält Item-Key, Kauf-/Verkaufspreis, aktive Kauf-/Verkaufsaufträge, Kategorie und Zeitstempel; JSON-Warmstart. Reguläre Hintergrundsynchronisierung. | Lokale Übersicht, Suche/Kategorien, kompakte Aktivitätswerte und Frischehinweis. Keine Live-Abfrage beim Navigieren. |
| Händler/Shard/Redcoins | `ShardCache` hält Quelle, Zielwährung, Basis-, Wechselkurs, Anzeigename und Zeitstempel; JSON-Warmstart. | Nach Zielwährung gruppierte Kurstabelle; fehlende oder alte Werte sichtbar markieren. |
| Preisalarme | Regeln für Kauf/Verkauf/Spanne über/unter Schwelle, einmalig/wiederholt, Cooldown und Rearm. Zustand und letzte Events im Service. Lokales HUD/Web-UI. | Aktive Regeln, Zustand und globaler Schalter ingame; vollständige Bearbeitung verbleibt im Web. |
| Item-Tooltip | `ItemTooltipCallback` ergänzt den normalen Slot-Tooltip aus lokalen Caches; separater Kauf/Verkauf, Händler/Shard, Datenalter und Stale-Verhalten. | Häufige Schalter in eigener Gruppe; die Web-UX bleibt für detaillierte Frischegrenze zuständig. |
| HUD/Container | `HudOverlay` zeigt Job-/Inventarwerte und Warnungen; `HandledScreenMixin` bietet Containerwert und Schnellbuttons. | Status/Schalter in Anzeigegruppe, strikt getrennt vom Item-Tooltip. |
| Schutz/Komfort | Rename-/Sign-Schutz, Offhand-Blocker, Inventarwarnung, Job-Tracker, Command-Kurzformen, Amboss-Normalisierung; Discord RPC/Screenshot-Ziele. | Schutzgruppe mit klaren Wirkhinweisen. Discord/Web/Proxy getrennt als erweiterte Einrichtung. |
| Verlauf | `PriceHistoryCache` lädt Itemverläufe bei Bedarf nach. | **Nicht** auf Menüöffnung oder Seitenwechsel zugreifen; detaillierte Charts verbleiben im Web. |

Das Ingame-Fenster verwendet den bereits vorhandenen, konfigurierbaren Öffnen-Keybind „Einstellungen öffnen“; es wird kein zweiter Menü-Keybind registriert. Der Keybind bleibt standardmäßig ungebunden; dasselbe gilt für HUD-Umschalten, manuellen Markt-/Händler-Refresh und fünf Screenshot-Tasten.

## Wirkung, Überschneidung und Lücken

- `showMarketTooltips` ist der Master-Schalter für Marktpreise im Tooltip; `tooltipShowBuyPrice` und `tooltipShowSellPrice` sind Details, keine Duplikate. Merchant-/Shard-Toggles wirken unabhängig davon. Die Detailoptionen sind heute in der Web-UX, aber nicht im Ingame-Fenster erreichbar.
- `showHud` schaltet das bestehende dauerhafte HUD einschließlich Inventarwarnung aus. Die kurzlebigen Preisalarm-HUD-Meldungen sind getrennt. Deshalb nicht beide als einen unklaren „HUD“-Schalter darstellen.
- `showContainerOverlay`, `showQuickButtons` und `shulkerRecursion` betreffen Container/Handled Screens, nicht den normalen Item-Tooltip.
- `priceAlertsEnabled` und die einzelnen Regeln sind jetzt ingame und in der Web-UX über denselben `ConfigManager` erreichbar. Beide Editoren verwenden dieselbe Eingabevalidierung; der Ingame-Editor bietet Suche, Bedingungen, Schwelle, Wiederholung, Cooldown, Rearm und HUD/Web-Kanäle.
- `tooltipMaxAgeSeconds` und `tooltipShowStaleData` sind wirksame Optionen, brauchen aber eine gemeinsame Erklärung: Datenalter kennzeichnen versus veraltete Werte ausblenden.
- Beim Aktivieren einiger Anzeigeoptionen ruft das alte Einstellungsfenster `triggerDataRefresh()` auf. Für das neue Menü ist dies zu entkoppeln: Öffnen/Navigieren/Anzeigeoptionen ändern lesen nur Cache; ausschließlich ein ausdrücklich beschrifteter manueller Refresh darf die öffentliche OPSucht-API anfragen.
- `PriceHistoryCache` ist ein Sonderfall: Der Web-Endpunkt `/api/history/{material}` kann bei Cache-Miss extern abrufen. Für die Ingame-Übersicht nur die vorhandenen Markt-/Händler-Snapshots benutzen.
- Für Marktaktivität stehen **aktive Auftragszahlen** pro Item bereit, nicht gesicherte abgeschlossene Handelsvolumina. UI daher „aktive Kauf-/Verkaufsaufträge“ nennen; keine Umsatz- oder Trendbehauptung ableiten.
- `showHud` und Alarm-HUD, Markt-Tooltip und Containerwert sowie Shardziel und sonstige Händlerziele haben verwandte Namen, aber unterschiedliche Wirkbereiche. Das Menü sollte diese sauber trennen, nicht Konfigurationsschlüssel entfernen.

## Observer-Modus und Versionen

Der Observer-Modus sperrt laut `ingameFeaturesEnabled()` Ingame-Eingriffe: Tooltips, dauerhaftes HUD, Container-Overlay, Quickbuttons, Schutz-/Offhand- und Komfortlogik. Hintergrund-Sync, Caches, lokales Webinterface und Settings-/Refresh-Keybind bleiben nutzbar. Im geplanten Menü sollten Markt-/Kurs-/Cache- und Alarmstatus weiter lesbar sein; deaktivierte Eingriffsoptionen sichtbar, aber als „im Observer-Modus inaktiv“ gekennzeichnet werden. Das aktuelle Alarm-HUD wird auch im Observer-Modus angezeigt und liegt getrennt vom dauerhaften HUD-Schalter; vor der Menüumsetzung sollte diese beabsichtigte Ausnahme ausdrücklich bestätigt werden.

Visotaris 1.2.0+ unterstützt ausschließlich Minecraft 26.x (aktuell 26.2). Alle Produktionsquellen liegen unter `src/main/`; 26.x-spezifische Überschreibungsfilter und der alte 1.21.11-Build wurden entfernt. Die gemeinsamen Service-/API-/Config-Tests wurden nach `src/test/` übernommen und sind an `:26.x:test` angebunden. Alte 1.1.x-Releases behalten ihre Kompatibilität.

## Lokale Web-API und Sicherheit

Vorhanden sind lesende Routen für Markt, Top-Aktivität, Einzelitem, Verlauf, Shard, Redcoins, Händler und Cache-Metadaten sowie geschützte `/api/system/*`-Routen für System-/MC-Info, Alarmregeln/-Events und Tooltip-Optionen. Regel- und Tooltip-Schreibzugriffe sind gezielt validiert. Die Ingame-Ansicht braucht **keinen** neuen HTTP-Endpunkt: Sie kann Services und Config direkt lesen. Lokale Anzeigeänderungen gehen über denselben `ConfigManager` wie die Web-UX; kein separater Zustand. Sensible Port-/Proxy-/Webhook- und Systemzugangsdaten gehören nicht in öffentliche Informationskarten.

## Menüstruktur und Umsetzungsstand

Umgesetzt sind **Übersicht** (Cachefrische, letzte Updates, Alarmstatus), **Markt** (lokale Suche/Kategorien, Preise, aktive Auftragszahlen), **Shard & Händler** (Filter nach Zielwährung und Kurse), **Tooltips** (konkrete Slot-Tooltip-Optionen), **Preisalarme** (Regelzustand/globaler Schalter/Link), **Schutz & Komfort** (Observer, Anzeige, Schutzlogik, Kurzformen) und **System** (Status, manueller Refresh, weiterführende lokale Einstellungsseiten). Bedienung: vorhandener Öffnen-Keybind, Escape zurück, Tab/Shift+Tab und Pfeile für Fokus/Navigieren, Enter/Space für Optionen, Maus und Scrollrad. Kleine Höhen sind scrollbar; Seitenbereiche verwenden kompakte Beschriftungen mit Hover-Details.

Menüöffnung, Seitenwechsel, Suche, Filter und Anzeigeoptionen lesen lokale Snapshots und `ConfigManager`; sie stoßen selbst keine API-Abfragen an. Der ausdrücklich betätigte System-Refresh ruft den bestehenden Markt- und Händler-Sync auf. Reguläre Hintergrundsynchronisation bleibt davon unberührt. Der Menücode greift nicht auf den lazy ladenden `PriceHistoryCache` zu.

Abweichungen bzw. absichtlich Web-only: Detaillierte Charts/Preisverläufe bleiben im Web; eine neue Anfrage je Menüaufruf oder Chart wäre nicht cache-lokal. Passwort-, Proxy-, Webhook- und Pfaddaten erscheinen nicht in Systemkarten. Discord/Screenshot- sowie Netzwerkdetails bleiben in den bisherigen eigenen lokalen Unterseiten. Alarmregeln lassen sich jetzt auch nativ vollständig verwalten; die Web-UX bleibt als ausführliche Ansicht verfügbar. Der genaue Funktionsabgleich steht unter [`ux/implemented/web-ingame-abgleich.md`](ux/implemented/web-ingame-abgleich.md).

`ItemNameResolver` verwendet die Vanilla Item-/Block-Übersetzungsschlüssel der aktiven Minecraft-Sprache. Damit werden Markt-/Shard-Keys als normale Spielnamen angezeigt; unbekannte, nicht in der installierten Sprache auflösbare Items fallen auf den API-Key/Anzeigenamen zurück. Es wird keine eigene Font- oder Itemdatenbank ausgeliefert.

Der native Renderer verwendet Minecraft 26.x `GuiGraphicsExtractor`. Itemicons verwenden bei geladener Welt `ClientLevel.registryAccess()` und die dort gebundene Item-Registry. Ohne Welt erzeugt das Menü keine ItemStacks; neutrale Icons halten Markt-/Auktions-/Händleransichten im Hauptmenü nutzbar. Lokalisierte Namen und Cachewerte benötigen keine Welt.

Variantenentscheidung: A priorisiert klare Seitennavigation; B priorisiert Daten und schnelle Marktbeobachtung; C ist die kompakte, tastaturorientierte Minecraft-Ansicht. Alle sechs Beispielseiten pro Variante sind im PDF. Nach Auswahl: (1) Leseschnittstellen und Cache-Frische-Viewmodel, (2) responsive Screen-Shell für 26.x, (3) read-only Datenkarten, (4) gemeinsame Config-Toggles ohne Refresh-Nebeneffekt, (5) Fokus-/Skalierungs-/Observer-Tests. **Keine Implementierung vor Feedback zur Variante.**
