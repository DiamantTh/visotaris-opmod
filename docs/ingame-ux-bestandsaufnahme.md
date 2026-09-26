# Ingame-UX: Bestandsaufnahme und Konzept (Entscheidungsvorlage)

Stand: 27. September 2026. Dieses Dokument beschreibt den untersuchten Quellstand; das neue Menü ist **nicht** implementiert. Die 18 visuellen Beispielansichten stehen in [`ux/visotaris-ingame-ux-konzepte.pdf`](ux/visotaris-ingame-ux-konzepte.pdf). Zahlen und Uhrzeiten darin sind ausdrücklich Beispieldaten.

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

Das bestehende Ingame-Fenster ist eine zweispaltige, scrollbar angeordnete Einstellungsseite mit Modus-, Anzeige-, Alarm-, Schutz-, Komfort-, Discord-, API- und Web-Gruppen. Es bietet noch keine integrierte Markt-/Kursübersicht. Der vorhandene Keybind „Einstellungen öffnen“ ist konfigurierbar, aber standardmäßig ungebunden; dasselbe gilt für HUD-Umschalten, manuellen Markt-/Händler-Refresh und fünf Screenshot-Tasten. Ein künftiger Menü-Keybind sollte den bisherigen Öffnen-Keybind **weiterverwenden**, nicht doppelt anlegen.

## Wirkung, Überschneidung und Lücken

- `showMarketTooltips` ist der Master-Schalter für Marktpreise im Tooltip; `tooltipShowBuyPrice` und `tooltipShowSellPrice` sind Details, keine Duplikate. Merchant-/Shard-Toggles wirken unabhängig davon. Die Detailoptionen sind heute in der Web-UX, aber nicht im Ingame-Fenster erreichbar.
- `showHud` schaltet das bestehende dauerhafte HUD einschließlich Inventarwarnung aus. Die kurzlebigen Preisalarm-HUD-Meldungen sind getrennt. Deshalb nicht beide als einen unklaren „HUD“-Schalter darstellen.
- `showContainerOverlay`, `showQuickButtons` und `shulkerRecursion` betreffen Container/Handled Screens, nicht den normalen Item-Tooltip.
- `priceAlertsEnabled` ist ingame vorhanden, Regelbearbeitung bisher nur über die geschützte Web-UX. Das ist eine sinnvolle Aufgabenteilung; kein zweiter Regel-Editor nötig.
- `tooltipMaxAgeSeconds` und `tooltipShowStaleData` sind wirksame Optionen, brauchen aber eine gemeinsame Erklärung: Datenalter kennzeichnen versus veraltete Werte ausblenden.
- Beim Aktivieren einiger Anzeigeoptionen ruft das alte Einstellungsfenster `triggerDataRefresh()` auf. Für das neue Menü ist dies zu entkoppeln: Öffnen/Navigieren/Anzeigeoptionen ändern lesen nur Cache; ausschließlich ein ausdrücklich beschrifteter manueller Refresh darf die öffentliche OPSucht-API anfragen.
- `PriceHistoryCache` ist ein Sonderfall: Der Web-Endpunkt `/api/history/{material}` kann bei Cache-Miss extern abrufen. Für die Ingame-Übersicht nur die vorhandenen Markt-/Händler-Snapshots benutzen.
- Für Marktaktivität stehen **aktive Auftragszahlen** pro Item bereit, nicht gesicherte abgeschlossene Handelsvolumina. UI daher „aktive Kauf-/Verkaufsaufträge“ nennen; keine Umsatz- oder Trendbehauptung ableiten.
- `showHud` und Alarm-HUD, Markt-Tooltip und Containerwert sowie Shardziel und sonstige Händlerziele haben verwandte Namen, aber unterschiedliche Wirkbereiche. Das Menü sollte diese sauber trennen, nicht Konfigurationsschlüssel entfernen.

## Observer-Modus und Versionen

Der Observer-Modus sperrt laut `ingameFeaturesEnabled()` Ingame-Eingriffe: Tooltips, dauerhaftes HUD, Container-Overlay, Quickbuttons, Schutz-/Offhand- und Komfortlogik. Hintergrund-Sync, Caches, lokales Webinterface und Settings-/Refresh-Keybind bleiben nutzbar. Im geplanten Menü sollten Markt-/Kurs-/Cache- und Alarmstatus weiter lesbar sein; deaktivierte Eingriffsoptionen sichtbar, aber als „im Observer-Modus inaktiv“ gekennzeichnet werden. Das aktuelle Alarm-HUD wird auch im Observer-Modus angezeigt und liegt getrennt vom dauerhaften HUD-Schalter; vor der Menüumsetzung sollte diese beabsichtigte Ausnahme ausdrücklich bestätigt werden.

Beide unterstützten Builds (Minecraft 1.21.11 und 26.x) haben dieselben fachlichen Gruppen und Keybinds. Unterschiedlich sind Fabric-/Minecraft-Eingabe- und Rendering-APIs sowie einige versionsspezifische Mixins. Eine nur in einer Version angebotene Menüfunktion wurde nicht festgestellt. Die neue Ansicht muss deshalb zwei dünne versionsspezifische Screen-/Render-Anpassungen verwenden und dieselben gemeinsamen Daten-/Config-Services lesen.

## Lokale Web-API und Sicherheit

Vorhanden sind lesende Routen für Markt, Top-Aktivität, Einzelitem, Verlauf, Shard, Redcoins, Händler und Cache-Metadaten sowie geschützte `/api/system/*`-Routen für System-/MC-Info, Alarmregeln/-Events und Tooltip-Optionen. Regel- und Tooltip-Schreibzugriffe sind gezielt validiert. Die Ingame-Ansicht braucht **keinen** neuen HTTP-Endpunkt: Sie kann Services und Config direkt lesen. Lokale Anzeigeänderungen gehen über denselben `ConfigManager` wie die Web-UX; kein separater Zustand. Sensible Port-/Proxy-/Webhook- und Systemzugangsdaten gehören nicht in öffentliche Informationskarten.

## Menüstruktur und Umsetzungsreihenfolge

Empfohlene Struktur: **Übersicht** (Cachefrische, letzte Updates, Alarmstatus), **Markt** (Suche/Kategorie, Preis, aktive Aufträge), **Shard & Händler** (Zielwährungen/Kurse), **Preisalarme** (Status/globaler Schalter/Link zur Web-UX), **Anzeige** (normaler Item-Tooltip getrennt von HUD und Container), **Schutz & Komfort** (Observer, Schutzlogik, Kurzformen), **System** (Status und bewusster Refresh; sensible Einrichtung nur in eigenen geschützten Schirmen). Keyboard: vorhandener Öffnen-Keybind, Escape zurück, Tab oder Pfeile durch Kategorien, Enter/Space für fokussierte Optionen, Maus und Scrollrad parallel. Mindestabstände und Scissor/Scroll für kleine GUI-Skalierungen.

Variantenentscheidung: A priorisiert klare Seitennavigation; B priorisiert Daten und schnelle Marktbeobachtung; C ist die kompakte, tastaturorientierte Minecraft-Ansicht. Alle sechs Beispielseiten pro Variante sind im PDF. Nach Auswahl: (1) Leseschnittstellen und Cache-Frische-Viewmodel, (2) responsive Screen-Shell für beide Builds, (3) read-only Datenkarten, (4) gemeinsame Config-Toggles ohne Refresh-Nebeneffekt, (5) Fokus-/Skalierungs-/Observer-Tests. **Keine Implementierung vor Feedback zur Variante.**
