# OPMOD Technische Bewertung und Implementierungshinweise

## Zweck dieses Dokuments

Dieses Dokument fasst die technische Pruefung der Datei `opmod-1.1.0-beta+mc1.21.11.jar` in konsolidierter Form zusammen.

Ziel ist eine nachvollziehbare, sachliche Beschreibung von:

- Architektur und Funktionsumfang
- nachweisbaren Datenquellen und Hook-Punkten
- OPSUCHT-spezifischen Analyse- und Ableitungsmechanismen
- den Bausteinen, die fuer einen eigenstaendigen Nachbau in Java oder Kotlin erforderlich waeren

Der Bericht ist so formuliert, dass er als technische Grundlage fuer interne Bewertung, Architekturvergleich oder Ableitung eines eigenen Implementierungsansatzes verwendet werden kann.

## Executive Summary

OPMOD ist eine umfangreiche Fabric-Client-Mod fuer Minecraft mit Fokus auf OPSUCHT-spezifische Komfort- und Auswertungsfunktionen. Die Mod basiert technisch hauptsaechlich auf:

- Fabric Entry Points
- Mixins in Client-, Screen- und Chat-Hooks
- periodische HTTP-Abfragen gegen OPSUCHT-APIs
- Parsing sichtbarer Chat- und Itemdaten
- Caches fuer Preis- und Shardwerte
- eigene UI- und Config-Schicht

Fuer die technische Einordnung ist der wichtigste Punkt:

Die Mod liest das meiste nicht aus "geheimer" Serverlogik, sondern aus drei Quellen:

- sichtbare OPSUCHT-APIs
- sichtbare Chat-/Screen-/Itemdaten
- harte Regeln und Mapping-Logik im Client

Die Dekompilation bestaetigt zusaetzlich einen fuer OPSUCHT wichtigen Punkt:

- viele "Custommenues" werden clientseitig nicht als eigene magische Speziallogik behandelt
- stattdessen nutzt OPMOD normale Minecraft-Screens als Andockpunkt
- Menues werden meist ueber feste OPSUCHT-Kommandos geoeffnet
- danach werden sichtbare Titel, Slots, Items und Eingabefelder ausgewertet

Das ist fuer einen Eigenbau zentral: Der eigentliche Mehrwert liegt in der Kombination aus Command-Shortcuts, Screen-Hooks, Slot-Scans und serverbezogenen Stringregeln, nicht in versteckter Protokollkenntnis.

## Was die Mod technisch macht

Die Kernfunktionen sind:

- Marktpreise laden und in Tooltips, Inventar und Containern anzeigen
- Merchant-/Shardwerte laden und in Tooltips und Berechnungen einbeziehen
- Jobtracker aus Chatmeldungen ableiten
- HUD fuer Job, XP, Geld und Stundenwerte rendern
- Inventar-Buttons fuer OPSUCHT-Kommandos anzeigen
- `/rename` und `/sign` ueber Bestaetigungslogik absichern
- Inventar voll erkennen und warnen
- Item-Seltenheit hervorheben
- Offhand-Interaktionen blockieren
- Settings-GUI und Keybinds bereitstellen

Ergaenzend aus der Dekompilation nachgewiesen:

- das Inventar des Spielers wird als zentrale Startflaeche fuer OPSUCHT-Menues benutzt
- es gibt zwei getrennte Buttonleisten: Hauptbuttons am Inventarrahmen und kleinere Rezept-/Amboss-Buttons im rechten Bereich
- Custom-/Servermenues werden nicht nur ueber offene Container erkannt, sondern oft indirekt ueber den Weg "Client fuehrt OPSUCHT-Command aus, Server oeffnet danach ein GUI"
- anvil-aehnliche Eingabemenues werden ueber Titelheuristiken wie `Preis setzen`, `Bieten` oder `Geld einzahlen` erkannt und dann lokal normalisiert

## Nachgewiesener Initialisierungsablauf

Die Dekompilation von `OPMODClient` zeigt einen klaren, reproduzierbaren Startpfad:

1. Versionspruefung initialisieren
2. Offhand-Blocker registrieren
3. Rarity-Highlighting registrieren
4. HUD registrieren
5. Inventarbutton-Register ausfuehren
6. Rezept-/Ambossbutton-Register ausfuehren
7. Shard- und Marktpreise asynchron laden
8. Tooltip-Hooks registrieren
9. Container-Overlay registrieren
10. Client-Commands registrieren
11. Command-Normalizer aktivieren
12. Inventarwarnung, Keybinds und Tick-Events aktivieren

Das ist architektonisch relevant, weil sich daraus bereits die reale Modulreihenfolge fuer einen Nachbau ableiten laesst:

- zuerst Daten und Basishooks
- dann Anzeige und Eingriffspunkte
- dann Komfort- und Schutzlogik

## Nachgewiesene Datenquellen

### OPSUCHT-APIs

- `https://api.opsucht.net/market/prices`
  Zweck: Marktpreise fuer Kauf/Verkauf
- `https://api.opsucht.net/merchant/rates`
  Zweck: Merchant-/Shardkurse

### Drittquellen

- Modrinth API
  Zweck: Versionspruefung
- Discord IPC
  Zweck: vorbereitete lokale Rich Presence, offenbar aktuell nicht sauber eingebunden

### Lokale Datenquellen

- Chat-/Game-Messages
- gehaltene Items
- Containerinhalte
- NBT/Item-Komponenten wie Shulker-Inhalte
- Config-Datei
- Screentitel von Inventar-/Anvil-/Container-Screens
- Tastaturstatus wie `Shift`
- Hotbar-Slot und aktuell gehaltenes Item fuer Schutzlogik

## Technische Risikobewertung

Im geprueften Umfang der Bytecode- und Decompiler-Analyse ergaben sich keine Hinweise auf:

- Shell-Ausfuehrung
- Prozessstarts
- versteckte Socket-Kommunikation ausser HTTP und Discord-IPC
- generische Dateiexfiltration
- verdeckte Remote-Code-Ausfuehrung

Nachweisbare Reflection beschraenkt sich auf die automatische Suche und Instanziierung von Client-Command-Klassen.

Nachweisbare Dateizugriffe betreffen im Wesentlichen:

- Laden und Schreiben der lokalen Konfiguration
- Loeschen der Konfigurationsdatei beim Zuruecksetzen der Settings

Nachweisbare Netzwerknutzung betrifft:

- Marktpreise
- Merchant-/Shardkurse
- Versionspruefung
- optionale lokale Discord-IPC-Kommunikation

## Architektur auf hoher Ebene

Die Mod laesst sich in acht Hauptschichten aufteilen.

### 1. Bootstrap

Zentrale Klassen:

- `dev.opmod.OPMOD`
- `dev.opmod.OPMODClient`
- `dev.opmod.ModMenuApiImpl`

Aufgaben:

- Initialisierung
- Registrierung aller Features
- Event-Verkabelung
- Settings-Anbindung an Mod Menu

### 2. Datenbeschaffung

Zentrale Klassen:

- `dev.opmod.opmarkt.PriceFetcher`
- `dev.opmod.shards.ShardRateFetcher`
- `dev.opmod.misc.VersionCheck`

Aufgaben:

- HTTP-Abfragen
- JSON-Parsing
- Cache-Befuellung
- Update-Hinweise

### 3. Caches und Modelle

Zentrale Klassen:

- `dev.opmod.opmarkt.MarketCache`
- `dev.opmod.opmarkt.MarketPrice`
- `dev.opmod.shards.ShardCache`
- `dev.opmod.shards.ShardRate`
- `dev.opmod.jobsystem.tracking.JobData`

Aufgaben:

- In-Memory-Ablage
- normierte Datenobjekte
- Zugriff fuer UI, Tooltips und Berechnung

### 4. Ableitung aus Spielzustand

Zentrale Klassen:

- `dev.opmod.mixin.client.GameMessageMixin`
- `dev.opmod.mixin.client.PlayerInventoryScreenMixin`
- `dev.opmod.mixin.client.HandledScreenClickMixin`
- `dev.opmod.ui.speed.InventoryInfoPanel`
- `dev.opmod.ui.containerOverlay.priceOverlay`
- `dev.opmod.TimeTracker`

Aufgaben:

- Chat-Parsing
- Screen-Hooking
- Button-Click-Abfanglogik
- Inventarwert-Berechnung
- Containerwert-Berechnung
- Zeitmessung

### 5. Schutz- und Komfortlogik

Zentrale Klassen:

- `dev.opmod.features.Protection.RenameProtection`
- `dev.opmod.features.Protection.SignProtection`
- `dev.opmod.features.OffhandBlocker`
- `dev.opmod.features.InventoryFullWarning`
- `dev.opmod.features.ItemRarityHighlighter`
- `dev.opmod.features.AnvilPriceNormalizer`
- `dev.opmod.features.PayCommandNormalizer`
- `dev.opmod.features.BankCommandNormalizer`

Aufgaben:

- Intercept und Bestaetigung
- Eingaben normalisieren
- UI-Warnung und Spielkomfort

### 6. UI und Overlays

Zentrale Klassen:

- `dev.opmod.ui.HUDOverlay`
- `dev.opmod.ui.UIManager`
- `dev.opmod.ui.components.*`
- `dev.opmod.ui.inventoryButtons.*`
- `dev.opmod.ui.inventoryButtons.craft_anvil.*`
- `dev.opmod.ui.screens.*`

Aufgaben:

- HUD-Ausgabe
- Layout
- eigene UI-Komponenten
- feste OPSUCHT-Shortcut-Buttons
- Editor-/Settings-Screens

### 7. Config-System

Zentrale Klassen:

- `dev.opmod.config.ConfigManager`
- `dev.opmod.config.ConfigScreen`
- `dev.opmod.config.entry.*`

Aufgaben:

- Defaults laden
- Config lesen und schreiben
- grafische Settings

### 8. Commands und Keybinds

Zentrale Klassen:

- `dev.opmod.commands.*`
- `dev.opmod.keybinds.Keybinds`

Aufgaben:

- Client-Commands
- Bestaetigungs-Kommandos
- HUD- und Screen-Steuerung

## Wie OPMOD OPSUCHT-spezifische Logik analysiert

Die Mod verwendet vier Hauptmethoden.

### A. API-basiert

Das betrifft:

- Marktpreise
- Merchant-/Shardkurse
- teilweise Update-Status

Beispiel:

- `ShardRateFetcher` liest `source` und `exchangeRate` aus dem Merchant-Endpoint
- die Daten werden in den `ShardCache` ueberfuehrt

### B. String- und Chat-Parsing

Das betrifft:

- Jobname
- XP
- Geld
- Level
- Prozentfortschritt

Beispiel:

- `GameMessageMixin` sucht Nachrichten mit `Level`, `$`, `•`
- extrahiert vier Zahlen per Regex
- aktualisiert `JobData`

### C. Item- und NBT-/Komponenten-Auswertung

Das betrifft:

- Inventarwert
- Containerwert
- Shulker-Inhalt
- Tooltip-Anreicherung
- Shard-Zuordnung fuer Items

Beispiel:

- `PriceFetcher` und `priceOverlay` lesen Shulker-Komponenten aus
- innere Items werden aufaddiert

### D. Hart kodierte Clientregeln

Das betrifft:

- bekannte Jobnamen
- feste Commands
- Screentitel fuer serverseitige Eingabemenues
- Label- und Key-Mappings
- Default-Shardwerte
- Schutz- und GUI-Verhalten

## OPSUCHT- und Custommenue-Anbindung im Detail

Gerade fuer "diverse Custommenues" auf OPSUCHT zeigt die Dekompilation ein wichtiges Bild: OPMOD besitzt keine allgemein generische "erkenne jedes Servermenu"-Engine, sondern mehrere gezielte Integrationsarten.

### 1. Command-gesteuerte Menueoeffnung

Ueber `ButtonRegistrar` werden im Inventarscreen feste Buttons registriert, die direkt OPSUCHT-Kommandos ausfuehren:

- `bank`
- `ah`
- `markt`
- `merchant`
- `quests`
- `tresor`
- `ec`
- `belohnung`
- `friend`
- `pets`
- `minion`
- `plot menu`
- `home`
- `nav`

Ueber `RecipeAreaButtonRegistrar` kommen weitere Buttons hinzu:

- `craft`
- `anvil`

Technische Bedeutung:

- OPMOD oeffnet viele OPSUCHT-Menues nicht selbst per GUI-Protokoll
- der Client sendet schlicht bekannte Serverkommandos
- das serverseitig danach geoeffnete Menu wird anschliessend ueber sichtbare Minecraft-Screens genutzt

Fuer einen Eigenbau ist das die wichtigste Abgrenzung: Die Menue-Integration besteht primar aus bequemer Command-Orchestrierung, nicht aus Reverse Engineering eines proprietaeren Menuekanals.

### 2. Sichtbare Inventar-Screens als Andockpunkt

`PlayerInventoryScreenMixin` rendert zusaetzliche Komponenten direkt im Spielerinventar:

- Inventar-Infopanel links vom Standardinventar
- obere Inventar-Buttonleiste
- untere Inventar-Buttonleiste
- Rezept-/Ambossbuttons im rechten Rezeptbereich

`HandledScreenClickMixin` faengt Mausklicks auf diese Buttons ab und verhindert dann die normale Screen-Verarbeitung.

Das heisst konkret:

- OPMOD erweitert vorhandene Vanilla-Screens
- es fuehrt keine vollstaendige eigene Inventar-GUI fuer OPSUCHT
- die Bedienlogik sitzt als Overlay ueber bestehenden `HandledScreen`-Instanzen

### 3. Teilweise Erkennung serverseitiger Eingabemenues

`AnvilPriceNormalizer` zeigt, dass bestimmte OPSUCHT-Eingabedialoge ueber Titelerkennung behandelt werden. Nachgewiesene erlaubte Titel sind:

- `Preis setzen`
- `Anzahl`
- `Geld abheben`
- `Geld einzahlen`
- `Bieten`
- `Startgebot`
- `Sofortkaufpreis`

Diese Menues werden offenbar als Anvil- oder anvil-aehnliche Screens praesentiert. OPMOD nutzt:

- den sichtbaren Screentitel
- das Textfeld des Anvil-Screens
- Platzhaltererkennung wie `Eingabe...`, `Betrag eingeben...`, `Suche...`
- lokale Erweiterung von Kurzschreibweisen wie `1k`, `2m`, `3b`

Das ist fuer OPSUCHT besonders relevant, weil viele serverseitige Custommenues technisch nur Vanilla-Container oder Anvil-Dialoge mit anderer Semantik sind.

### 4. Containerwertung nur fuer bestimmte Menuearten

`priceOverlay` wird nicht auf beliebige Custom-GUIs angewendet, sondern nur wenn einer der folgenden Faelle eintritt:

- Shulker-Screen
- generischer Container-Screen mit Vanilla-artigem Translation-Key, der mit `container.` beginnt

Folgerung:

- OPMOD kann zwar viele OPSUCHT-Menues per Command oeffnen
- die Wert-Overlay-Logik ist aber bewusst enger gefasst
- stark customisierte Menues ohne passenden Container-/Titeltyp fallen eher aus diesem Overlay heraus

Das sollte im Bauplan explizit beruecksichtigt werden: "Custommenue-Unterstuetzung" ist in OPMOD kein monolithisches Feature, sondern verteilt sich auf Command-Shortcuts, Titelerkennung, Inventaroverlays und punktuelle Containeranalyse.

## Hook-Matrix der realen Eingriffspunkte

Die Dekompilation zeigt folgende operative Hook-Klassen:

- `GameMessageMixin`
  Aufgabe: Chatzeilen fuer Jobtracker auswerten
- `PlayerInventoryScreenMixin`
  Aufgabe: Buttons, Tooltips und Inventar-Infopanel rendern
- `HandledScreenClickMixin`
  Aufgabe: Klicks auf Overlay-Buttons konsumieren
- `HandledScreenMixin`
  Aufgabe: Rarity-Highlighting in Screen-Renderpfad einhaengen
- `SignProtectionMixin`
  Aufgabe: `/sign` und `/rename` vor dem Senden abfangen
- `ClientPlayerInteractionMixin`
  Aufgabe: Offhand-Interaktionen auf Clientseite abbrechen

Diese Hook-Matrix ist fuer einen Nachbau wichtiger als die reine Paketliste, weil sich daraus exakt ergibt, an welchen Stellen OPMOD Verhalten einblendet, aendert oder blockiert.

## Weitere reale Einhaengepunkte

Neben den offensichtlichen Mixins haengt sich OPMOD an mehreren weiteren Stellen in den Clientfluss ein.

### Fabric-Events statt nur Mixins

Nachgewiesene Event-Einhaenge:

- `ItemTooltipCallback.EVENT`
  fuer Markt-/Shard-Tooltips
- `ScreenEvents.AFTER_INIT`
  fuer Container-Overlay-Initialisierung
- `ScreenEvents.afterRender`
  fuer wiederholtes Zeichnen der Containerwerte
- `ClientSendMessageEvents.MODIFY_COMMAND`
  fuer `/pay` sowie `bank einzahlen` und `bank auszahlen`
- `ClientTickEvents.END_CLIENT_TICK`
  fuer Preisrefresh, Inventarwarnung, Keybinds, Anvil-Normalisierung und Version-Check
- `ClientPlayConnectionEvents.JOIN`
  fuer sessionbezogene Initialisierung wie Update-Pruefung
- `ClientPlayConnectionEvents.DISCONNECT`
  fuer Ruecksetzen von Sessionzustand
- `UseBlockCallback`, `UseItemCallback`, `UseEntityCallback`
  fuer Offhand-Blockierung

Das heisst praktisch:

- OPMOD haengt sich nicht nur in einzelne Minecraft-Methoden
- es haengt sich auch breit in den Event-Lifecycle von Fabric ein

### Eingriff vor dem eigentlichen Command-Senden

Es gibt mehrere voneinander getrennte Vorverarbeitungsstellen fuer Client-Kommandos:

- `SignProtectionMixin`
  blockiert `/sign` und `/rename`, bis der Nutzer bestaetigt
- `PayCommandNormalizer`
  rewritet Kurzschreibweisen wie `1k`, `2m`, `3b`
- `BankCommandNormalizer`
  rewritet Kurzschreibweisen fuer `bank einzahlen` und `bank auszahlen`

Das ist ein wichtiger zusaetzlicher Einhaengepunkt, weil die Mod damit nicht nur Screens erweitert, sondern den Befehlsfluss selbst veraendert.

### Tick-getriebene Dauerintegration

Mehrere Features leben vollstaendig von wiederholten Tick-Hooks:

- Marktpreis-Refresh
- Keybind-Auswertung
- Inventar-voll-Warnung
- Anvil-Textnormalisierung
- verzoegert gestartete Versionspruefung

Fuer einen Eigenbau ist das relevant, weil diese Features einen kleinen Laufzeit-Orchestrator brauchen und nicht als lose Utilities behandelt werden sollten.

### Session- und Lifecycle-Einhaenge

`VersionCheck` ist ein gutes Beispiel fuer spaeter gestartete Integration:

- beim Modstart wird nur die Event-Verkabelung registriert
- die eigentliche Pruefung wird erst nach `JOIN` vorbereitet
- ausgefuehrt wird sie nochmals spaeter im Tick, wenn Spieler und Welt verfuegbar sind

Dasselbe Muster taucht auch bei vorbereiteten, aber aktuell ungenutzten Features wie `PresenceBuilder` und `AutoIdleRender` auf.

### Reflection als indirekter Einhaengepunkt

`CommandAutoRegistrar` scannt zur Laufzeit die Klassen unter `dev.opmod.commands`, laedt passende Typen reflektiv und registriert sie als Client-Commands.

Dadurch existiert noch ein weiterer Integrationspfad:

- nicht nur statisch verdrahtete Commands
- sondern dynamisch entdeckte Command-Bausteine innerhalb der Mod

### Teilweise vorhandene, aber nicht aktiv verdrahtete Hooks

Die erneute Dekompilation zeigt mehrere Bausteine, die fuer weitere Einhaengepunkte sprechen, aktuell aber nicht im Startpfad auftauchen:

- `PresenceBuilder`
  besitzt Discord-IPC plus Tick-/Stop-Hooks, wird aber nicht gestartet
- `AutoIdleRender`
  besitzt Tick-, Join-, Disconnect- und Stop-Hooks, wird aber nicht registriert
- `GameMenuScreenMixin`
  existiert als Mixin-Skelett, enthaelt in der geprueften Fassung aber keine aktive Injection

Fuer die Dokumentation ist diese Unterscheidung wichtig:

- aktiv eingehangene Features
- vorbereitete, aber unverdrahtete Features
- leere Platzhalterklassen

## Aktivitaets- und Reifegrad-Matrix

Nach dem zweiten Vollscan laesst sich der Bestand sinnvoll in vier Gruppen zerlegen.

### A. Aktiv verdrahtet und nachweisbar genutzt

Dazu gehoeren in der geprueften Fassung insbesondere:

- `OPMODClient`
- `PriceFetcher`
- `ShardRateFetcher`
- `HUDOverlay`
- `priceOverlay`
- `ButtonRegistrar`
- `RecipeAreaButtonRegistrar`
- `GameMessageMixin`
- `PlayerInventoryScreenMixin`
- `HandledScreenClickMixin`
- `HandledScreenMixin`
- `SignProtectionMixin`
- `ClientPlayerInteractionMixin`
- `PayCommandNormalizer`
- `BankCommandNormalizer`
- `AnvilPriceNormalizer`
- `InventoryFullWarning`
- `Keybinds`
- `CommandAutoRegistrar`
- `ConfigScreen`
- `ModMenuApiImpl`
- `VersionCheck`

Das sind die Klassen, die aktuell den tatsaechlichen Laufzeitpfad formen.

### B. Aktiv erreichbar, aber eher sekundaire oder interne Pfade

- `RenameCommand`
- `SignCommand`
- `SettingsCommand`
- `TrackerCommand`
- `ShardkursCommands`
- `RenameProtection`
- `SignProtection`
- `InventoryButtonManager`
- `RecipeAreaButtonManager`
- `InventoryInfoPanel`

Diese Klassen sind nicht selbst der zentrale Entry Point, aber eindeutig im aktiven Funktionspfad eingebunden.

### C. Vorhanden, aber vermutlich historisch oder alternativ

- `SettingsScreen`
- `SettingsModel`
- `SettingsManager`
- `SettingsConfig`
- `UIManager`
- `UIOverlay`
- `payoutTracker`

Interpretation:

- hier gibt es Hinweise auf ein aelteres oder alternatives UI-/Settings-Konzept
- die aktuelle Mod benutzt aber primaer `ConfigScreen` statt `SettingsScreen`
- Teile davon koennen noch erreichbar sein, sind aber nicht der dominante aktuelle Integrationsweg

### D. Vorhanden, aber in der geprueften Fassung nicht aktiv gestartet

- `PresenceBuilder`
- `AutoIdleRender`
- `GameMenuScreenMixin`

Interpretation:

- diese Teile zeigen geplante oder begonnene Erweiterungen
- sie zaehlen zur Architektur, aber nicht zum aktuell gesicherten Aktivpfad

## Vollanalyse der oft nachgefragten OPMOD-Punkte

Dieser Abschnitt beantwortet die praktische Rueckfrage explizit:

- was OPMOD in diesen Bereichen bereits real macht
- was OPMOD nur teilweise oder nur in enger Form macht
- was OPMOD in diesen Bereichen gerade nicht als generische Loesung besitzt

Wichtig:

- die folgende Liste beschreibt den nachgewiesenen Ist-Zustand von OPMOD
- sie beschreibt nicht primaer eine Wunschliste fuer `Visotaris OPMod`
- fuer den Nachbau ist sie deshalb besonders wichtig, weil sie zwischen vorhandener Funktion und fehlender Generalisierung trennt
- gemeint ist hier die analysierte Mod `OPMOD` aus der vorliegenden JAR `opmod-1.1.0-beta+mc1.21.11.jar`
- inhaltlich bezieht sich die Analyse damit auf das Modrinth-Projekt `https://modrinth.com/mod/opmod`

Zur Klarstellung:

- wenn in diesem Dokument von `OPMOD` gesprochen wird, ist der technische Ist-Zustand der analysierten JAR gemeint
- Aussagen wie `vorhanden`, `aktiv`, `teilweise vorhanden` oder `nicht generisch geloest` beziehen sich in diesem Abschnitt ausschliesslich auf diese Mod
- der Abschnitt ist damit bewusst keine Beschreibung eines separaten Eigenprojekts, sondern eine Analyse des Vorbilds selbst

### 1. Inventory-Buttons im Spielerinventar

Was OPMOD bereits nachweislich macht:

- aktive Inventar-Buttons im Spielerinventar rendern
- zwei getrennte Buttonbereiche verwenden
- Klicks auf diese Buttons abfangen und konsumieren
- ueber diese Buttons bekannte OPSUCHT-Kommandos direkt ausfuehren

Nachgewiesene technische Bausteine:

- `ButtonRegistrar`
- `InventoryButtonManager`
- `RecipeAreaButtonRegistrar`
- `RecipeAreaButtonManager`
- `PlayerInventoryScreenMixin`
- `HandledScreenClickMixin`

Nachgewiesene Commands:

- `bank`
- `ah`
- `markt`
- `merchant`
- `quests`
- `tresor`
- `ec`
- `belohnung`
- `friend`
- `pets`
- `minion`
- `plot menu`
- `home`
- `nav`
- `craft`
- `anvil`

Was OPMOD dabei nicht als generische Loesung besitzt:

- keine universelle Servermenu-API
- keine generische Erkennung beliebiger Custom-Buttons aus Serverdaten
- keine automatische Ableitung neuer Menues aus dem Spielzustand

Fachliche Bewertung:

- OPMOD hat dieses Feature real und aktiv
- es ist aber command- und layoutbasiert, nicht datengetrieben oder generisch

### 2. AnvilPriceNormalizer

Was OPMOD bereits nachweislich macht:

- anvil-aehnliche Eingabemenues erkennen
- ueber einen Tick-Hook das sichtbare Textfeld pruefen
- Platzhaltertexte entfernen
- Kurzschreibweisen wie `1k`, `2m`, `3b` in volle Zahlen expandieren

Nachgewiesene technische Bausteine:

- `AnvilPriceNormalizer`
- `AnvilScreenAccessor`
- `ClientTickEvents.END_CLIENT_TICK`

Nachgewiesene erlaubte Titel:

- `Preis setzen`
- `Anzahl`
- `Geld abheben`
- `Geld einzahlen`
- `Bieten`
- `Startgebot`
- `Sofortkaufpreis`

Nachgewiesene Platzhalter:

- `Eingabe...`
- `Betrag eingeben...`
- `Suche...`

Was OPMOD dabei nicht als generische Loesung besitzt:

- keine allgemein konfigurierbare Menueklassifikation fuer alle Texteingabe-Screens
- keine serverunabhaengige Normalisierungsengine
- keine erkennbare Abstraktion fuer mehrere Titelprofile oder Serverprofile

Fachliche Bewertung:

- OPMOD hat dieses Feature real und aktiv
- es ist stark titelheuristisch und servergebunden

### 3. Shulker-Inhalt-Rekursion in Tooltip-/Inventar-/Containerwerten

Was OPMOD bereits nachweislich macht:

- Shulker-Inhalte aus Item-Komponenten lesen
- Inhalte im Tooltip gesondert ausweisen
- Inhalte in Inventarwerten mit einrechnen
- Inhalte in Containerwerten mit einrechnen
- Markt- und Shardwerte dabei beide beruecksichtigen

Nachgewiesene technische Bausteine:

- `PriceFetcher`
- `InventoryInfoPanel`
- `priceOverlay`

Nachgewiesene Wirkung:

- Tooltip zeigt bei Shulkern Inhalts-Kaufwert
- Tooltip zeigt bei Shulkern Inhalts-Verkaufswert
- Tooltip zeigt bei Shulkern Inhalts-Shardwert
- Inventarwert summiert Shulkerinhalt mit auf
- Containerwert summiert Shulkerinhalt mit auf

Was OPMOD dabei nicht als generische Loesung besitzt:

- keine zentrale gemeinsame Bewertungsengine fuer alle drei Pfade
- keine klare einheitliche Service-Schicht fuer Rekursion
- keine erkennbare Unterstuetzung fuer beliebige verschachtelte Sonderfaelle ausser den konkret behandelten Container-Items

Fachliche Bewertung:

- OPMOD hat dieses Feature real und nicht nur als TODO
- die Funktion ist vorhanden, aber architektonisch verteilt statt zentral gebuendelt

### 4. Inventar-voll-Warnung

Was OPMOD bereits nachweislich macht:

- Tick-basierte Pruefung auf volles Inventar
- Cooldown zur Vermeidung permanenter Warnungen
- zwei Pruefmodi fuer "voll"
- Overlay-/Toast-Ausgabe
- optionalen Soundeffekt

Nachgewiesene technische Bausteine:

- `InventoryFullWarning`
- Tick-Aufruf aus `OPMODClient`

Nachgewiesene Zusatzlogik:

- modifizierte Items werden gesondert behandelt
- volle Stacks und freie Slots werden je nach Modus unterschiedlich gewertet
- die Warnung verwendet nicht nur Chat, sondern einen visuellen Ingame-Hinweis

Was OPMOD dabei nicht als generische Loesung besitzt:

- keine universelle Inventarqualitaets-Engine
- keine erkennbare Kontexttrennung nach Spielmodus oder Containerart

Fachliche Bewertung:

- OPMOD hat dieses Feature real und aktiv
- es ist voll funktionsfaehig, aber relativ eng auf den Clientkomfortfall zugeschnitten

### 5. OffhandBlocker

Was OPMOD bereits nachweislich macht:

- Offhand-Blockinteraktionen abbrechen
- Offhand-Itemnutzung abbrechen
- Offhand-Entityinteraktionen abbrechen
- zusaetzlich direkte Interaktionspfade auf Mixin-Ebene absichern

Nachgewiesene technische Bausteine:

- `OffhandBlocker`
- `UseBlockCallback.EVENT`
- `UseItemCallback.EVENT`
- `UseEntityCallback.EVENT`
- `ClientPlayerInteractionMixin`

Was OPMOD dabei nicht als generische Loesung besitzt:

- keine regelbasierte Matrix fuer unterschiedliche Offhand-Sperrstrategien
- keine erkennbare Feinsteuerung nach Itemtyp, Entitytyp oder Kontext

Fachliche Bewertung:

- OPMOD hat dieses Feature real und aktiv
- es ist eine bewusste Schutz-/Komfortsperre, aber kein hochabstraktes Input-Regelsystem

### 6. Keybinds

Was OPMOD bereits nachweislich macht:

- Keybinds registrieren
- Keybinds im Tick pollen
- je nach Taste Screens oeffnen oder Trackerzustand aendern

Nachgewiesene technische Bausteine:

- `Keybinds`
- `KeyBindingHelper.registerKeyBinding`
- `ClientTickEvents.END_CLIENT_TICK`

Nachgewiesene Aktionen:

- Settings oeffnen
- UI-Editor oeffnen oder schliessen
- Jobtracker reset
- Jobtracker pausieren oder fortsetzen
- Jobtracker-HUD ein- oder ausblenden

Was OPMOD dabei nicht als generische Loesung besitzt:

- keine zentral modellierte Action-Bind-Matrix
- keine konfigurierbare Hotkey-Aktionsschicht jenseits der fest eincodierten Funktionen

Fachliche Bewertung:

- OPMOD hat dieses Feature real und aktiv
- die Hotkeys sind funktional vorhanden, aber klassisch hart verdrahtet

### 7. SessionEvents wie JOIN/DISCONNECT

Was OPMOD bereits nachweislich macht:

- `JOIN` fuer sessionbezogene Initialisierung verwenden
- `DISCONNECT` fuer sessionbezogenen Reset verwenden

Nachgewiesene aktive Nutzung:

- `VersionCheck` setzt bei `JOIN` ein Pending-Flag
- `VersionCheck` setzt bei `DISCONNECT` den Status zurueck

Nachgewiesene vorbereitete, aber nicht aktiv verdrahtete Nutzung:

- `AutoIdleRender` besitzt Join-/Disconnect-Hooks
- `PresenceBuilder` besitzt Tick-/Stop-Hooks fuer sessionbezogene Presence-Aktualisierung

Was OPMOD dabei nicht als generische Loesung besitzt:

- keine zentrale SessionLifecycle-Schicht, die alle sessiongebundenen Features konsolidiert
- keine sichtbare gemeinsame Reset-Strategie fuer alle zustandsbehafteten Systeme

Fachliche Bewertung:

- OPMOD nutzt SessionEvents real
- aber aktuell nicht als vollstaendig vereinheitlichte Session-Architektur

## Kurzzusammenfassung dieser Punkte

Fuer die genannten Punkte gilt insgesamt:

- OPMOD besitzt den Grossteil dieser Funktionen bereits real
- offen ist dort meist nicht das "ob", sondern die Tiefe der Abstraktion
- die Grenzen liegen eher in fehlender Generalisierung, verteilter Logik und historisch gewachsenem Code

Die sauberste fachliche Formulierung lautet deshalb:

- OPMOD macht diese Dinge ueberwiegend bereits
- aber meist als konkrete OPSUCHT-nahe Loesung
- nicht als universelle, klar abstrahierte Systemarchitektur

Das ist genau der Punkt, an dem `Visotaris OPMod` besser werden kann:

- gleiche oder aehnliche Funktionen
- aber mit saubereren Service-Grenzen
- klarerer Hook-Trennung
- einer einheitlichen Config- und Session-Architektur

## Einordnung gegenueber typischen Rueckfragen

Die genannten Punkte werden in Rueckfragen oft so formuliert, als seien sie in OPMOD noch offen. Die Dekompilation der hier analysierten JAR zeigt jedoch:

- viele dieser Punkte sind in OPMOD bereits real implementiert
- offen ist dort haeufig nicht die Grundfunktion, sondern eher die fehlende Zentralisierung oder Generalisierung
- die staerksten funktionalen Grenzen liegen weniger im `ob`, sondern im `wie sauber` und `wie generisch`

Das gilt insbesondere fuer:

- Shortform-Commands
- Inventar-Buttons
- anvilbasierte Eingabenormalisierung
- Shulker-Inhaltsrekursion
- Shardwertberechnung im Inventarkontext
- Offhand-Blockierung
- Inventar-voll-Warnung
- Keybinds

Anders gesagt:

- OPMOD ist in diesen Bereichen funktional bereits deutlich weiter als eine oberflaechliche Gap-Liste vermuten laesst
- die eigentlichen architektonischen Schwaechen liegen eher in verteilten Regeln, parallelen Altpfaden und fehlender Service-Zentralisierung

## Detailbewertung der Funktionsbereiche

### Markt- und Shardsystem

### Marktpreise

Zentrale Klassen:

- `PriceFetcher`
- `MarketCache`
- `MarketPrice`

Verhalten:

- Fetch per `HttpURLConnection`
- JSON wird in Preisobjekte ueberfuehrt
- Kauf- und Verkaufspreise werden getrennt gespeichert
- Tooltipdaten werden zusaetzlich gecacht
- Refresh laeuft asynchron ueber Single-Thread-Executor
- zyklischer Reload erfolgt alle `1200` Client-Ticks
- Tooltipwerte werden bei Preisupdates aktiv invalidiert

Technische Beobachtung:

- der Tooltip greift nur dann, wenn bereits ein `MarketPrice` fuer den Registry-Key des Items vorhanden ist
- `Shift` aktiviert Mengenberechnung fuer Itemstacks
- Shulkerinhalte werden im Tooltip separat als Inhaltswerte ausgewiesen

### Merchant- und Shardwerte

Zentrale Klassen:

- `ShardRateFetcher`
- `ShardCache`
- `ShardRate`
- `ShardDefaults`
- `ShardkursCommands`

Verhalten:

- Fetch gegen Merchant-Endpoint
- pro Eintrag werden `source` und `exchangeRate` gelesen
- `source` wird normalisiert
- Werte werden fuer Tooltips, Inventar und Commands bereitgestellt

Technische Beobachtung:

- `source` ist offenbar nicht direkt sauber, sondern wird adaptiv geparst
- der Client baut daraus stabile Schluessel
- die Normalform ist effektiv `baseId|displayName|customModelData`, sofern diese Teile vorhanden sind
- Anzeigenamen werden aus serialisiertem Chat-JSON extrahiert
- das deutet auf OPSUCHT-Items hin, die sich nicht nur ueber nackte Item-IDs unterscheiden lassen

Architektonische Relevanz:

- fuer einen Nachbau reicht eine reine Registry-ID-Matrix nicht aus
- man braucht zusaetzlich einen serverbezogenen Item-Fingerprint-Layer
- insbesondere bei Merchant- oder Spezialitems kann `displayName` plus `custom_model_data` der eigentliche Primarschluessel sein

### Jobtracker

Zentrale Klassen:

- `GameMessageMixin`
- `JobData`
- `TimeTracker`
- `HUDOverlay`

Verhalten:

- Chatmessage wird gefiltert
- Regex extrahiert Werte
- bekannte Jobnamen werden aus Stringvorkommen erkannt
- `TimeTracker` fuehrt Messzeit
- `HUDOverlay` berechnet Anzeige und Stundenwerte

Auffaellige technische Beobachtung:

- XP und Geld werden im Code nur mit `/ 2.0` aufaddiert
- Chatzeilen mit `»` werden explizit ausgeschlossen
- die Logik reagiert also nicht auf jede sichtbare Servernachricht, sondern auf ein enges Nachrichtenformat
- die bekannte Jobmenge ist hart kodiert: `Holzfaeller`, `Minenarbeiter`, `Fischer`, `Graeber`, `Jaeger`, `Builder`, `Farmer`

### Schutzlogik fuer `/rename` und `/sign`

Zentrale Klassen:

- `RenameProtection`
- `SignProtection`
- `RenameCommand`
- `SignCommand`
- `SignProtectionMixin`

Verhalten:

- Command wird abgefangen
- `Pending*`-Objekt speichert Zustand
- Validierung auf Slotwechsel, Itemwechsel, Timeout
- Bestaetigung ueber interne Client-Commands
- Darstellung der Bestaetigung ueber klickbare Chat-Komponenten
- Farbe/Formatcodes wie `&a` werden lokal in Stilobjekte umgesetzt

Nachgewiesene interne Commands:

- `/.confirmRename`
- `/.cancelRename`
- `/.confirmSign`
- `/.cancelSign`

### Inventar- und Containerbewertung

Zentrale Klassen:

- `InventoryInfoPanel`
- `priceOverlay`
- `PriceFetcher`
- `MarketCache`
- `ShardCache`

Verhalten:

- Inventarwert ueber alle Slots
- Containerwert fuer Kisten/Faesser/Shulker
- Shulker-Inhalte werden rekursiv mitgerechnet
- Ausgabe im Screen

Architektonische Beobachtung:

- Inventarwert und Containerwert verwenden nahezu dieselbe Kernidee
- OPMOD hat aber keine gemeinsame zentrale `ValuationEngine`, sondern verteilt die Berechnung auf `InventoryInfoPanel`, `priceOverlay` und `PriceFetcher`
- genau hier liegt fuer einen Eigenbau ein offensichtlicher Konsolidierungspunkt

### GUI und Config

Zentrale Klassen:

- `ConfigManager`
- `ConfigScreen`
- `CategorySidebarWidget`
- `ConfigEntryListWidget`
- `BooleanEntry`
- `IntegerSliderEntry`
- `DoubleSliderEntry`
- `EnumEntry`
- `MultiSelectDropdownEntry`

Verhalten:

- Defaults aus eingebetteter JSON
- Config auf Platte
- GUI mit Sidebar und spezialisierten Eintraegen
- Reset loescht Config-Datei und laedt neu

Nachgewiesene Defaultbereiche:

- `general`
- `jobtracker`
- `inventory`
- `tooltip`
- `visuals`

Wichtige Beobachtung:

- im dekompilierten Code wird fuer Tooltips `boldTooltip` abgefragt
- in der eingebetteten JSON steht aber `TooltipBold`
- das ist ein Hinweis auf moegliche inkonsistente Schluessel oder evolvierten Codebestand

### Command- und Laufzeit-Integrationssystem

Zentrale Klassen:

- `CommandAutoRegistrar`
- `SettingsCommand`
- `TrackerCommand`
- `ShardkursCommands`
- `RenameCommand`
- `SignCommand`
- `VersionCheck`
- `Keybinds`

Verhalten:

- Client-Commands werden reflektiv gefunden und registriert
- sichtbare Nutzerbefehle und interne Arbeitsbefehle existieren nebeneinander
- Keybinds oeffnen Screens oder toggeln Zustandswerte
- Join-, Tick- und Disconnect-Zustaende beeinflussen, wann bestimmte Features aktiv werden

Architektonische Bedeutung:

- Befehle sind in OPMOD nicht nur UI-Komfort
- sie bilden auch einen internen Steuerkanal fuer Schutzlogik, Settings und Tracker

### Doppeltes Settings-/UI-System

Der zweite Durchgang zeigt, dass OPMOD offenbar zwei unterschiedliche Konfigurations- und Settings-Linien im Code traegt:

1. das aktuell klar aktive `ConfigScreen`-System
   mit `ConfigManager`, `ConfigEntryListWidget`, `CategorySidebarWidget` und `config.entry.*`
2. ein aelteres oder alternatives `SettingsScreen`-System
   mit `SettingsModel`, `SettingsManager`, `Setting`, `ToggleSetting`, `SliderSetting`

Technische Bedeutung:

- die Mod besitzt nicht nur "eine" Settings-Schicht
- es gibt deutliche Spuren einer Evolution oder Umstellung
- fuer einen Eigenbau sollte man diese Doppelung bewusst vermeiden und nur eine konsistente Settings-Architektur bauen

Zusatzbeobachtung:

- `OPMODClient.scheduleOpenSettings()` oeffnet `SettingsScreen`
- `SettingsCommand` und `ModMenuApiImpl` oeffnen dagegen `ConfigScreen`
- das ist ein konkreter Hinweis auf parallele UI-Pfade im Bestand

### HUD-Editor als eigener Integrationspfad

`UIEditorScreen` ist mehr als nur ein Hilfsscreen:

- der Screen ueberlagert die normale Ansicht vollflaechig
- das Job-HUD wird live als Preview gerendert
- Dragging, Snapping und Persistenz laufen direkt im Screen
- Schliessen erfolgt ueber ESC oder denselben Keybind

Das ist ein eigener Einhaengepunkt, weil hier nicht nur Configwerte gesetzt werden, sondern ein temporaerer Interaktionsmodus fuer die HUD-Position erzeugt wird.

### Toast-/Overlay- und Audio-Hooks

`InventoryFullWarning` haengt sich nicht nur logisch in den Tick ein, sondern triggert auch:

- einen Ingame-Toast/Overlay-Hinweis
- optional einen Soundeffekt

Auch das zaehlt als eigener Integrationstyp:

- nicht nur Screen- oder Chat-Ausgabe
- sondern direkter Eingriff in die Feedbackkanaele des Clients

### ModMenu-Integration

Ueber `ModMenuApiImpl` ist OPMOD zusaetzlich in ein externes Mod-UI eingehangen:

- Mod Menu oeffnet direkt `ConfigScreen`
- dadurch existiert ein weiterer offizieller Einstieg in die Mod-Konfiguration ausser Chat-Command und Keybind

Das ist fuer die Architektur relevant, weil Konfigurationszugang nicht nur intern, sondern auch ueber ein anderes Mod-Oekosystem erfolgt.

### Historische oder inkonsistente Konfigurationspfade

Der Vollscan zeigt mehrere Hinweise auf evolvierten oder teilweise inkonsistenten Bestand:

- aktive Pfade verwenden `tooltip.boldTooltip`
- die eingebettete Default-JSON enthaelt `TooltipBold`
- `SettingsManager` referenziert alte Sektionen wie `HUD`
- `SettingsManager` kennt Schluessel wie `autoDownloadUpdate` und `showYaw`, die im aktiven aktuellen Pfad nicht sichtbar sind
- `AutoIdleRender` liest `general.autoIdleRender`, dieser Schluessel taucht im aktuellen Standardpfad aber nicht auf

Das ist fuer einen Eigenbau sehr lehrreich:

- ueber Jahre gewachsene Komfortmods akkumulieren oft tote oder halb migrierte Configpfade
- genau diese Altlasten sollte man in einem Nachbau systematisch vermeiden

## Nachgewiesene Bedienoberflaechen fuer OPSUCHT

Die Mod hat faktisch vier getrennte UI-Ebenen fuer OPSUCHT-spezifische Nutzung:

### 1. Tooltip-Ebene

- Markt-Kaufpreis
- Markt-Verkaufspreis
- Shardwert
- bei Shulkern zusaetzlich Inhalts-Kaufwert, Inhalts-Verkaufswert und Inhalts-Shardwert

### 2. Inventarrahmen-Ebene

- feste Shortcut-Buttons fuer serverseitige Menues
- linksseitiges Infopanel fuer Speed und Inventarwert

### 3. Container-Ebene

- Overlay fuer Gesamtwert von Containerinhalten
- Zusatzhinweis, wenn Shulker plus Shulkerinhalt eingerechnet wurden

### 4. HUD-Ebene

- Jobname
- Level
- Fortschritt
- Trackingzeit
- Geld und XP
- Hochrechnungen pro Stunde
- Restzeit bis Level-Up

Fuer den Bauplan bedeutet das:

- nicht "eine grosse GUI" nachbauen
- sondern mehrere kleine, zustandsgetriebene Anzeigeebenen mit klar getrennten Triggern


## Klassenlandkarte nach Paketen

### `dev.opmod`

Wichtige Klassen:

- `OPMOD`
- `OPMODClient`
- `TimeTracker`
- `ModMenuApiImpl`

Rolle:

- Einstiegspunkt
- Initialisierung
- globale Steuerung

### `dev.opmod.commands`

Wichtige Klassen:

- `CommandAutoRegistrar`
- `RenameCommand`
- `SettingsCommand`
- `ShardkursCommands`
- `SignCommand`
- `TrackerCommand`

Rolle:

- Client-Commands
- Command-Glue fuer Schutz- und UI-Funktionen

### `dev.opmod.config`

Wichtige Klassen:

- `ConfigManager`
- `ConfigScreen`
- `SettingsConfig`
- `UITheme`
- `CategorySidebarWidget`
- `ConfigEntryListWidget`

Rolle:

- Persistenz
- Theme
- Settings-Screen

### `dev.opmod.config.entry`

Rolle:

- einzelne Eingabetypen fuer die Settings

Nachgewiesene Typen:

- `AbstractConfigEntry`
- `ActionEntry`
- `BooleanEntry`
- `CategoryEntry`
- `ConfigEntry`
- `DoubleSliderEntry`
- `DualActionEntry`
- `EnumEntry`
- `IntegerSliderEntry`
- `MultiSelectDropdownEntry`
- `StringEntry`

### `dev.opmod.features`

Rolle:

- konkrete Spielverbesserungen und Schutzschichten

Nachgewiesene Typen:

- `AnvilPriceNormalizer`
- `BankCommandNormalizer`
- `InventoryFullWarning`
- `ItemRarityHighlighter`
- `OffhandBlocker`
- `PayCommandNormalizer`

### `dev.opmod.features.Protection`

Rolle:

- Bestaetigung und Validierung sensibler Aktionen

Nachgewiesene Typen:

- `RenameProtection`
- `RenameProtection$PendingRename`
- `SignProtection`
- `SignProtection$PendingSign`

### `dev.opmod.features.performance`

Rolle:

- Performance/Idle-Logik

Nachgewiesener Typ:

- `AutoIdleRender`

Hinweis:

- im Code vorhanden, aber aktuell kein klarer aktiver Registrierungspfad nachgewiesen

### `dev.opmod.jobsystem.tracking`

Rolle:

- Jobzustand und Trackingdaten

Nachgewiesene Typen:

- `JobData`
- `payoutTracker`

### `dev.opmod.keybinds`

Rolle:

- Hotkeys fuer HUD, Editor und Settings

Nachgewiesener Typ:

- `Keybinds`

### `dev.opmod.misc`

Rolle:

- Versionierung, Logging, Presence

Nachgewiesene Typen:

- `Logger`
- `PresenceBuilder`
- `VersionCheck`

Hinweis:

- `PresenceBuilder` ist implementiert, aber aktuell ohne klaren Startaufruf

### `dev.opmod.misc.interfaces`

Rolle:

- kleine Integrationsschnittstellen und Metadaten

Nachgewiesene Typen:

- `ClientCommand`
- `ConfigOption`

Technische Bedeutung:

- `ClientCommand` ist die Grundlage der reflektiv gefundenen Command-Registrierung
- `ConfigOption` ist die Grundlage des alternativen annotationsgetriebenen Settings-Modells

### `dev.opmod.mixin.client`

Rolle:

- Hook-Ebene in Minecraft/Fabric

Nachgewiesene Typen:

- `AnvilScreenAccessor`
- `ClientPlayerInteractionMixin`
- `GameMenuScreenMixin`
- `GameMessageMixin`
- `HandledScreenAccessor`
- `HandledScreenClickMixin`
- `HandledScreenMixin`
- `PlayerInventoryScreenMixin`
- `SignProtectionMixin`

### `dev.opmod.opmarkt`

Rolle:

- Marktpreislogik

Nachgewiesene Typen:

- `MarketCache`
- `MarketPrice`
- `PriceFetcher`

### `dev.opmod.shards`

Rolle:

- Merchant-/Shardlogik

Nachgewiesene Typen:

- `ShardCache`
- `ShardDefaults`
- `ShardRate`
- `ShardRateFetcher`

### `dev.opmod.settings`

Rolle:

- alternatives oder aelteres Settings-Modell

Nachgewiesene Typen:

- `Setting`
- `SettingsCategory`
- `SettingsManager`
- `SettingsModel`

Hinweis:

- im Code vorhanden und teils noch ueber `SettingsScreen` erreichbar
- gegenueber dem aktuell dominanten `config.*`-Pfad aber eher nachrangig

### `dev.opmod.settings.types`

Rolle:

- Eingabetypen fuer das alternative Settings-System

Nachgewiesene Typen:

- `SliderSetting`
- `ToggleSetting`

Hinweis:

- diese Typen existieren parallel zu den neueren `config.entry.*`-Eintraegen

### `dev.opmod.ui`

Rolle:

- HUD und UI-Organisation

Nachgewiesene Typen:

- `HUDOverlay`
- `UIManager`

### `dev.opmod.ui.components`

Rolle:

- UI-Bausteine

Nachgewiesene Typen:

- `Clickable`
- `UIButton`
- `UIComponent`
- `UIFormatter`
- `UIFrame`
- `UILabel`
- `UIOverlay`
- `UISlider`
- `UITextLine`
- `UITextLine$Builder`

### `dev.opmod.ui.containerOverlay`

Rolle:

- Screen-Overlay fuer Containerwerte

Nachgewiesener Typ:

- `priceOverlay`

### `dev.opmod.ui.inventoryButtons`

Rolle:

- GUI-Shortcuts fuer OPSUCHT-Kommandos

Nachgewiesene Typen:

- `ButtonRegistrar`
- `InventoryButton`
- `InventoryButton$Builder`
- `InventoryButtonManager`

### `dev.opmod.ui.inventoryButtons.craft_anvil`

Rolle:

- Buttons im Rezept-/Anvil-Bereich

Nachgewiesene Typen:

- `RecipeAreaButton`
- `RecipeAreaButton$Builder`
- `RecipeAreaButtonManager`
- `RecipeAreaButtonRegistrar`

### `dev.opmod.ui.screens`

Rolle:

- vollwertige Screens

Nachgewiesene Typen:

- `SettingsScreen`
- `UIEditorScreen`

### `dev.opmod.ui.speed`

Rolle:

- Speed- und Inventarinfopanel

Nachgewiesene Typen:

- `InventoryInfoPanel`
- `InventoryInfoPanel$SlotPair`
- `InventoryInfoPanel$SpeedResult`

### `dev.opmod.utils`

Rolle:

- Hilfsfunktionen

Nachgewiesene Typen:

- `Logger`
- `NumberFormatter`
- `NumberParser`

## Zweiter Strang: abgeleitete Hinweise fuer einen Eigenbau

Die folgenden Punkte beschreiben nicht den geprueften Ist-Zustand der JAR, sondern technische Ableitungen aus der vorstehenden Bewertung. Sie sind bewusst vom eigentlichen Pruefteil getrennt.

### Architekturelle Leitlinien

- Preislogik, Shardlogik, Chatparser und UI voneinander trennen
- Hook-Punkte ueber Mixins und Events schlank halten
- Businesslogik in Services oder Engines kapseln
- sichtbare Serverannahmen zentral dokumentieren statt verteilt im UI-Code zu halten

### Ableitungen aus Markt- und Merchantlogik

- einen klaren Preisservice fuer Marktwerte vorsehen
- rohes API-JSON in stabile interne Modelle mappen
- fuer Merchant-`source`-Strings einen dedizierten Normalizer verwenden
- Cache-Layer und Darstellung voneinander trennen
- einen eigenstaendigen `ItemFingerprintResolver` einfuehren, der Registry-ID, Anzeigename und `custom_model_data` kombinieren kann

### Ableitungen aus dem Jobtracker

- Chatparser, Trackerzustand und HUD strikt trennen
- Serverformat nicht direkt in Rendercode einbetten
- Heuristiken wie der beobachtete `/ 2.0`-Faktor explizit dokumentieren und konfigurierbar halten

### Ableitungen aus der Schutzlogik

- Intercept, Pending-Status und Bestaetigungs-UI als getrennte Module behandeln
- Schutzlogik nicht in Mixins selbst unterbringen
- die Validierung von Slot, Item und Timeout isoliert testbar halten

### Ableitungen aus den vielen Einhaengepunkten

- Mixins, Fabric-Events und Tick-Loop nicht in einer Schicht vermischen
- pro Einhaengeart kleine Adapterklassen vorsehen
- Startpfad, Sessionpfad und Screenpfad getrennt modellieren
- explizit dokumentieren, ob ein Feature beim Modload, Join, Tick oder Screen-Render aktiv wird
- zwischen aktivem Pfad, Altpfad und vorbereitetem Pfad sauber unterscheiden

### Ableitungen aus Bewertungslogik und Config

- eine gemeinsame Bewertungsengine fuer Tooltip, Inventar und Container verwenden
- Shulker-Inhalte konsistent in allen Ausgaben einbeziehen
- Config-Backend und Config-UI getrennt halten
- Defaults nicht nur im Code, sondern in einer transportablen Defaultquelle pflegen

### Ableitungen aus OPSUCHT-Custommenues

- Command-basierte Menueoeffnung als eigene Schicht modellieren
- Screen-Erkennung in `MenuContextResolver` auslagern
- Anvil-, Container- und Inventar-Screens getrennt behandeln
- serverseitige Screentitel und Placeholder nicht in UI-Klassen verstreuen
- pro Menuetyp klar definieren, ob nur Oeffnung, nur Auswertung oder beides unterstuetzt wird

### Ableitungen aus dem Doppel-Settings-Bestand

- nur ein einziges aktives Settings-System betreiben
- Altpfade nicht nur liegenlassen, sondern explizit migrieren oder entfernen
- Config-Keys versionsfest halten und Umbenennungen sauber migrieren
- UI-Zugaenge ueber ModMenu, Command und Keybind auf dieselbe Zielimplementierung zeigen lassen

## Konkreter Bauplan fuer eine eigene Mod in Java/Kotlin

Wenn du eine eigene Mod sauber bauen willst, ohne OPMOD 1:1 nachzukopieren, solltest du die Architektur in getrennte Module schneiden.

### Mindestmodule

1. `bootstrap`
   Aufgabe: Fabric Entry Points, Registrierungen, Lifecycle

2. `api`
   Aufgabe: HTTP-Clients fuer Markt und Merchant

3. `model`
   Aufgabe: `MarketPrice`, `ShardRate`, `JobSnapshot`, Config-DTOs

4. `cache`
   Aufgabe: In-Memory-Caches und Refresh-Strategien

5. `parser`
   Aufgabe: Chatparser, Key-Normalizer, Item-/NBT-Mapper

6. `services`
   Aufgabe: Businesslogik wie Bewertungsengine, Trackerengine, Schutzlogik

7. `ui`
   Aufgabe: HUD, Screen-Overlay, Buttons, Config-Screen

8. `screen-integration`
   Aufgabe: Menu-Kontext, Inventarbutton-Anbindung, Container-/Anvil-Resolver

9. `hooks`
   Aufgabe: Mixins, Eventlistener, Eingriffspunkte in Minecraft

10. `commands`
   Aufgabe: Client-Commands und interne Aktionskommandos

11. `config`
    Aufgabe: Persistenz, Defaults, Validierung

### Zusaetzliche Pflichtmodule fuer OPSUCHT-nahe Menues

- `menu-actions`
  Aufgabe: bekannte OPSUCHT-Kommandos kapseln, z.B. `openBank()`, `openMarket()`, `openMerchant()`
- `menu-context`
  Aufgabe: sichtbare Menues aufloesen, etwa `PLAYER_INVENTORY`, `ANVIL_INPUT`, `VANILLA_CONTAINER`, `SERVER_CUSTOM_CONTAINER`
- `fingerprints`
  Aufgabe: Itemschluessel fuer Markt und Merchant robust ableiten
- `runtime-hooks`
  Aufgabe: Tick-, Join-, Disconnect-, Tooltip- und Command-Modifier-Hooks zentral verwalten

### Empfohlene Laufzeitdaten

Halte mindestens diese Modelle explizit:

- `MarketPrice`
  Felder: `buy`, `sell`
- `ShardRate`
  Feld: `rate`
- `TrackedJobState`
  Felder: `jobName`, `xp`, `money`, `level`, `percent`, `trackingSeconds`
- `PendingAction`
  Felder: `command`, `text`, `itemFingerprint`, `slot`, `timestamp`
- `InventoryValuation`
  Felder: `buy`, `sell`, `shard`, `hasShards`, `hasShulkers`
- `MenuContext`
  Felder: `screenType`, `title`, `isVanillaContainer`, `isAnvilInput`, `originCommand`
- `ItemFingerprint`
  Felder: `registryId`, `displayName`, `customModelData`, `normalizedKey`

### Empfohlene Engines

Baue die Logik in Engines, nicht direkt in Mixins oder Screens.

- `MarketSyncService`
- `MerchantSyncService`
- `TooltipValueService`
- `InventoryValuationService`
- `JobTrackerService`
- `PendingConfirmationService`
- `ConfigService`
- `MenuActionService`
- `MenuContextResolver`
- `ItemFingerprintService`
- `CommandRewriteService`
- `SessionLifecycleService`
- `TickOrchestrator`

### Konkreter Nachbau fuer diverse OPSUCHT-Custommenues

Wenn dein Ziel ausdruecklich ist, OPSUCHT-Menues und "diverse Custommenues" sauber abzubilden, dann sollte der Bauplan nicht nur Preis- und HUD-Funktionen enthalten, sondern diese konkrete Pipeline:

1. Benutzeraktion
   Beispiel: Klick auf Inventarbutton oder Hotkey
2. `MenuActionService`
   sendet bewusst ein bekanntes OPSUCHT-Kommando
3. Minecraft oeffnet daraufhin einen Screen
4. `MenuContextResolver`
   klassifiziert den Screen ueber Typ, Titel, Handler und Slotlayout
5. spezialisierte Presenter/Adapter
   entscheiden, welche Overlays, Warnungen oder Normalizer aktiv werden

Die kritische Trennung dabei:

- Menueoeffnung ist nicht dasselbe wie Menueerkennung
- Menueerkennung ist nicht dasselbe wie Menueauswertung
- Menueauswertung ist nicht dasselbe wie Menueautomation

Genau diese Trennung fehlt in vielen schnellen Mods und ist der Punkt, an dem ein Eigenbau deutlich robuster werden kann als der analysierte Bestand.

## Integrationsmatrix nach Art des Einhaengens

Die aktuelle Dekompilation legt folgende Einhaengearten offen:

- Mixin in Netzwerk- und Screenmethoden
  Beispiele: Chat-Parsing, Klickabfang, Command-Intercept
- Fabric-Events auf Tooltips, Ticks und Sessionwechsel
  Beispiele: Preisrefresh, Keybinds, Versionscheck, Offhand-Blocker
- Screen-Overlays auf Vanilla-Screens
  Beispiele: Inventarbuttons, Inventarpanel, Containerwerte, Slot-Highlights
- Command-Rewrite vor dem Senden
  Beispiele: `pay`, `bank`, Schutzlogik fuer `rename` und `sign`
- Reflection-basierte Laufzeit-Discovery
  Beispiele: automatische Client-Command-Registrierung
- vorbereitete, aber unverdrahtete Sessionfeatures
  Beispiele: Discord Presence, AutoIdleRender
- parallele oder historische UI-/Configpfade
  Beispiele: `ConfigScreen` versus `SettingsScreen`

Wenn man also fragt, wo sich die Mod "noch ueberall reinhaengt", dann lautet die belastbare Kurzfassung:

- in Screens
- in Renderpfade
- in Tooltips
- in Maus- und Use-Aktionen
- in den Command-Output des Clients
- in den Tick-Loop
- in Join/Disconnect-Lifecycle
- in die Client-Command-Registrierung
- in ModMenu als externen Konfigurationszugang
- und teilweise sogar in Clientoptionen wie die Render-Distanz

## Pedantische Klassenmatrix: Hook -> Zweck -> Status

Die folgende Matrix ist bewusst moeglichst pedantisch gehalten. Sie ist kein Marketing-Feature-Ueberblick, sondern eine Nachbauhilfe fuer eine eigene Mod wie `Visotaris OPMod`.

### Primäre Laufzeitklassen

| Klasse | Hook/Event/Methode | Zweck | Status | OPSucht-Bezug |
| --- | --- | --- | --- | --- |
| `OPMODClient` | `onInitializeClient()` | zentraler Client-Bootstrap, registriert nahezu alle aktiven Features | aktiv | indirekt, da alle OPSucht-Funktionen hier zusammenlaufen |
| `OPMODClient` | `ClientTickEvents.END_CLIENT_TICK` | periodischer Preisrefresh, Inventarwarnung, verzögertes Öffnen von Settings | aktiv | mittel |
| `OPMOD` | `onInitialize()` | leerer ModInitializer ohne sichtbare Logik | vorhanden, aber faktisch leer | keiner |
| `ModMenuApiImpl` | `getModConfigScreenFactory()` | externer Konfigurationszugang über Mod Menu | aktiv | indirekt |

### Netzwerk-, Command- und Sessionpfade

| Klasse | Hook/Event/Methode | Zweck | Status | OPSucht-Bezug |
| --- | --- | --- | --- | --- |
| `GameMessageMixin` | Mixin auf `ClientPlayNetworkHandler.method_43596` | Chatzeilen lesen, Jobwerte ableiten, Tracker starten/pausieren | aktiv | hoch |
| `SignProtectionMixin` | Mixin auf `ClientPlayNetworkHandler.method_45730` | `/sign` und `/rename` vor Versand abfangen | aktiv | hoch |
| `PayCommandNormalizer` | `ClientSendMessageEvents.MODIFY_COMMAND` | Kurzschreibweisen in `/pay` expandieren | aktiv | hoch |
| `BankCommandNormalizer` | `ClientSendMessageEvents.MODIFY_COMMAND` | Kurzschreibweisen in `bank einzahlen/auszahlen` expandieren | aktiv | hoch |
| `CommandAutoRegistrar` | `ClientCommandRegistrationCallback.EVENT` indirekt via `OPMODClient` | Client-Commands reflektiv finden und registrieren | aktiv | mittel |
| `CommandAutoRegistrar` | `Files.walk(...)` + `Class.forName(...)` | Command-Klassen im JAR dynamisch entdecken | aktiv | indirekt |
| `SettingsCommand` | Brigadier-Client-Command `opmod` | `ConfigScreen` öffnen | aktiv | indirekt |
| `TrackerCommand` | Brigadier-Client-Command `opmod JobTracker ...` | Tracker reset/pause | aktiv | hoch |
| `ShardkursCommands` | Brigadier-Client-Command `shardkurs` | Shardkurse mit Marktbezug ausgeben | aktiv | hoch |
| `RenameCommand` | Brigadier-Client-Commands `/.confirmRename`, `/.cancelRename` | interner Bestätigungsworkflow für `/rename` | aktiv | hoch |
| `SignCommand` | Brigadier-Client-Commands `/.confirmSign`, `/.cancelSign` | interner Bestätigungsworkflow für `/sign` | aktiv | hoch |
| `VersionCheck` | `ClientPlayConnectionEvents.JOIN` | Update-Prüfung nach Verbindungsaufbau vorbereiten | aktiv | keiner |
| `VersionCheck` | `ClientPlayConnectionEvents.DISCONNECT` | Update-Status zurücksetzen | aktiv | keiner |
| `VersionCheck` | `ClientTickEvents.END_CLIENT_TICK` | Update-Check erst ausführen, wenn Welt/Spieler existieren | aktiv | keiner |
| `VersionCheck` | `CompletableFuture` + `HttpClient` | asynchron gegen Modrinth prüfen | aktiv | keiner |

### Inventar-, Screen- und Renderhooks

| Klasse | Hook/Event/Methode | Zweck | Status | OPSucht-Bezug |
| --- | --- | --- | --- | --- |
| `PlayerInventoryScreenMixin` | Mixin auf `PlayerInventoryScreen.method_25394` `TAIL` | Inventarbuttons, Tooltips und Info-Panel rendern | aktiv | hoch |
| `HandledScreenClickMixin` | Mixin auf `HandledScreen.method_25402` `HEAD` | Klicks auf OPMOD-Buttons konsumieren | aktiv | hoch |
| `HandledScreenMixin` | Mixin auf `HandledScreen.method_25420` `RETURN` | Rarity-Highlighting in Slot-Renderpfad einhängen | aktiv | mittel |
| `ButtonRegistrar` | Inventarbutton-Registrierung | OPSucht-Kommandobuttons auf Inventarscreen setzen | aktiv | hoch |
| `RecipeAreaButtonRegistrar` | Rezept-/Ambossbutton-Registrierung | `craft` und `anvil` als Schnellzugriff setzen | aktiv | hoch |
| `InventoryButtonManager` | interner Render-/Tooltip-/Click-Dispatch | zentrale Verwaltung der Inventarbuttons | aktiv | hoch |
| `RecipeAreaButtonManager` | interner Render-/Tooltip-/Click-Dispatch | zentrale Verwaltung der Rezeptbereich-Buttons | aktiv | hoch |
| `InventoryInfoPanel` | eigener Renderpfad im Inventarscreen | Speed, Inventarwert, Marktwert, Shardwert anzeigen | aktiv | hoch |
| `priceOverlay` | `ScreenEvents.AFTER_INIT` | Container-Overlays initialisieren | aktiv | hoch |
| `priceOverlay` | `ScreenEvents.afterRender` | Containerwerte neben GUI rendern | aktiv | hoch |
| `UIEditorScreen` | eigener Vollbild-Screen | HUD-Position live verschieben, snappen und speichern | aktiv | indirekt |
| `ConfigScreen` | eigener Konfigurationsscreen | aktueller Haupt-Settingsscreen | aktiv | indirekt |
| `SettingsScreen` | eigener alternativer Konfigurationsscreen | älterer oder paralleler Settingspfad | erreichbar, aber nachrangig | indirekt |
| `GameMenuScreenMixin` | Mixin-Skelett ohne Injection | möglicher zukünftiger Hook in Pause-Menü | vorhanden, aber inaktiv | keiner |

### Tooltip-, Bewertungs- und Itempfade

| Klasse | Hook/Event/Methode | Zweck | Status | OPSucht-Bezug |
| --- | --- | --- | --- | --- |
| `PriceFetcher` | `ItemTooltipCallback.EVENT` | Markt- und Shardwerte in Itemtooltips einfügen | aktiv | hoch |
| `PriceFetcher` | asynchroner Executor | Marktpreise periodisch laden | aktiv | hoch |
| `ShardRateFetcher` | asynchroner Executor | Merchant-/Shardwerte laden | aktiv | hoch |
| `ItemRarityHighlighter` | Renderhook indirekt über `HandledScreenMixin` | Slotflächen je nach Tooltip-Rarity einfärben | aktiv | mittel |
| `ItemRarityHighlighter` | Tooltipzeilen lesen | Seltenheit aus sichtbarem Tooltiptext ableiten | aktiv | mittel |
| `InventoryInfoPanel` | Inventarscan | Markt-/Shardwerte des Spielerinventars berechnen | aktiv | hoch |
| `priceOverlay` | Containerscan | Markt-/Shardwerte von Shulker/Containern berechnen | aktiv | hoch |

### Schutz-, Komfort- und Inputpfade

| Klasse | Hook/Event/Methode | Zweck | Status | OPSucht-Bezug |
| --- | --- | --- | --- | --- |
| `RenameProtection` | interner Workflow | Pending-Rename speichern, validieren, bestätigen | aktiv | hoch |
| `SignProtection` | interner Workflow | Pending-Sign speichern, validieren, bestätigen | aktiv | hoch |
| `AnvilPriceNormalizer` | `ClientTickEvents.END_CLIENT_TICK` | anvil-artige OPSucht-Eingaben anhand Titel/Textfeld normalisieren | aktiv | hoch |
| `OffhandBlocker` | `UseBlockCallback.EVENT` | Offhand-Block-Interaktion abbrechen | aktiv | gering |
| `OffhandBlocker` | `UseItemCallback.EVENT` | Offhand-Itemnutzung abbrechen | aktiv | gering |
| `OffhandBlocker` | `UseEntityCallback.EVENT` | Offhand-Entityinteraktion abbrechen | aktiv | gering |
| `ClientPlayerInteractionMixin` | Mixin auf Entity-Interaktionsmethoden | Offhand-Blockade zusätzlich auf Interaktionsmanager-Ebene absichern | aktiv | gering |
| `InventoryFullWarning` | Tick-basierte Prüfung | volles Inventar erkennen | aktiv | mittel |
| `InventoryFullWarning` | Overlay/Toast/Sound | Warnhinweis und optionalen Sound auslösen | aktiv | mittel |
| `Keybinds` | `KeyBindingHelper.registerKeyBinding` | Hotkeys für Settings, UI-Editor und Tracker registrieren | aktiv | mittel |
| `Keybinds` | `ClientTickEvents.END_CLIENT_TICK` | Hotkeys pollen und Aktionen ausführen | aktiv | mittel |

### Historische, alternative oder unverdrahtete Pfade

| Klasse | Hook/Event/Methode | Zweck | Status | OPSucht-Bezug |
| --- | --- | --- | --- | --- |
| `PresenceBuilder` | Discord IPC + Tick-/Stop-Hooks | Rich Presence für Menü, Singleplayer, Multiplayer, Job | vorhanden, aber nicht gestartet | gering |
| `AutoIdleRender` | Tick-/Join-/Disconnect-/Stop-Hooks | Render-Distanz bei Leerlauf absenken | vorhanden, aber nicht registriert | keiner |
| `UIOverlay` | `HudRenderCallback.EVENT` | generisches altes HUD-Overlay-System | vorhanden, aber kein aktiver Registerpfad gefunden | gering |
| `UIManager` | internes Komponenten-Framework | generische UI-Komponenten verwalten | vorhanden, aber im Hauptpfad nachrangig | gering |
| `SettingsModel` | Annotation-Scanning über `ConfigOption` | alternatives Settingsmodell aufbauen | vorhanden, via `SettingsScreen` erreichbar | gering |
| `SettingsManager` | statische Settingsliste | älteres Settingsmodell mit teils alten Keys | vorhanden, aber nicht dominanter Pfad | gering |
| `payoutTracker` | freie Parse-Hilfsmethode | alternative alte Jobmessage-Parsinghilfe | vorhanden, kein aktiver Aufruf nachgewiesen | mittel |

## Alle konkreten Minecraft-/Fabric-Einhängepunkte

Der folgende Abschnitt ist absichtlich von der Klassenmatrix getrennt. Hier geht es nicht primär um Klassen, sondern um die konkreten API- oder Mixin-Angriffsstellen in Minecraft/Fabric.

### 1. Fabric-Client-Lifecycle

| Hook/API | Konkrete Nutzung | Klassen | Status |
| --- | --- | --- | --- |
| `ClientModInitializer.onInitializeClient` | Start aller aktiven Clientfeatures | `OPMODClient` | aktiv |
| `ClientTickEvents.END_CLIENT_TICK` | Preisrefresh, Keybinds, Warnungen, Anvil-Normalisierung, Update-Check | `OPMODClient`, `Keybinds`, `AnvilPriceNormalizer`, `VersionCheck`, `AutoIdleRender` | teils aktiv, teils vorbereitet |
| `ClientPlayConnectionEvents.JOIN` | Sessionstart, Update-Check vorbereiten, Idle-Tracking resetten | `VersionCheck`, `AutoIdleRender` | teils aktiv, teils vorbereitet |
| `ClientPlayConnectionEvents.DISCONNECT` | Sessionstatus resetten | `VersionCheck`, `AutoIdleRender` | teils aktiv, teils vorbereitet |
| `ClientLifecycleEvents.CLIENT_STOPPING` | Cleanup bei Clientende | `PresenceBuilder`, `AutoIdleRender` | vorbereitet |

### 2. Fabric-UI-/Render-APIs

| Hook/API | Konkrete Nutzung | Klassen | Status |
| --- | --- | --- | --- |
| `HudElementRegistry.attachElementAfter` | Jobtracker-HUD an Vanilla-HUD anhängen | `HUDOverlay` | aktiv |
| `HudRenderCallback.EVENT` | generisches alternatives HUD-System | `UIOverlay` | vorhanden, aber im Hauptpfad inaktiv |
| `ItemTooltipCallback.EVENT` | Markt-/Shardwerte in Tooltips einfügen | `PriceFetcher` | aktiv |
| `ScreenEvents.AFTER_INIT` | Container-Overlay nur für bestimmte Screens registrieren | `priceOverlay` | aktiv |
| `ScreenEvents.afterRender` | Werte neben Containern rendern | `priceOverlay` | aktiv |

### 3. Fabric-Command- und Input-APIs

| Hook/API | Konkrete Nutzung | Klassen | Status |
| --- | --- | --- | --- |
| `ClientCommandRegistrationCallback.EVENT` | reflektive Registrierung aller Client-Commands | `CommandAutoRegistrar` | aktiv |
| `ClientSendMessageEvents.MODIFY_COMMAND` | Commands vor Versand umschreiben | `PayCommandNormalizer`, `BankCommandNormalizer` | aktiv |
| `KeyBindingHelper.registerKeyBinding` | Hotkeys anlegen | `Keybinds` | aktiv |
| `UseBlockCallback.EVENT` | Offhand-Block-Interaktionen unterdrücken | `OffhandBlocker` | aktiv |
| `UseItemCallback.EVENT` | Offhand-Itemnutzung unterdrücken | `OffhandBlocker` | aktiv |
| `UseEntityCallback.EVENT` | Offhand-Entityinteraktion unterdrücken | `OffhandBlocker` | aktiv |

### 4. Mixin-Einhängepunkte in Minecraft-Klassen

| Zielklasse | Zielmethode | Mixin-Klasse | Zweck | Status |
| --- | --- | --- | --- | --- |
| `ClientPlayNetworkHandler` | `method_43596` | `GameMessageMixin` | Chatnachrichten für Jobsystem auswerten | aktiv |
| `ClientPlayNetworkHandler` | `method_45730` | `SignProtectionMixin` | `/sign` und `/rename` vor Versand abfangen | aktiv |
| `HandledScreen` | `method_25402` | `HandledScreenClickMixin` | Klicks auf Overlay-Buttons konsumieren | aktiv |
| `HandledScreen` | `method_25420` | `HandledScreenMixin` | Slot-Highlights nach dem Rendern einzeichnen | aktiv |
| `PlayerInventoryScreen` | `method_25394` | `PlayerInventoryScreenMixin` | Inventarpanel und Buttons rendern | aktiv |
| `ClientPlayerInteractionManager` | `method_2905` | `ClientPlayerInteractionMixin` | Offhand-Entityinteraktion blockieren | aktiv |
| `ClientPlayerInteractionManager` | `method_2917` | `ClientPlayerInteractionMixin` | Offhand-Entityinteraktion mit HitResult blockieren | aktiv |
| `AnvilScreen` | Feld-/Accessorzugriff | `AnvilScreenAccessor` | Textfeld des Anvil-Screens auslesen/manipulieren | aktiv indirekt |
| `HandledScreen` | Feld-/Accessorzugriff | `HandledScreenAccessor` | GUI-Koordinaten und Background-Maße auslesen | aktiv indirekt |
| `GameMenuScreen` | keine aktive Injection | `GameMenuScreenMixin` | Platzhalter für künftige Eingriffe | inaktiv |

### 5. Minecraft-interne Screen-/UI-Pfade ohne Fabric-Event

| Pfad | Konkrete Nutzung | Klassen | Status |
| --- | --- | --- | --- |
| `MinecraftClient.setScreen(...)` | Settings, UI-Editor und Config-Screens öffnen/schließen | `OPMODClient`, `Keybinds`, `SettingsCommand`, `ConfigScreen`, `UIEditorScreen` | aktiv |
| Anvil-Textfeld-Zugriff | Titel- und Texteingabe normalisieren | `AnvilPriceNormalizer` | aktiv |
| Toast/Overlay-Manager | Inventarvollwarnung ausgeben | `InventoryFullWarning` | aktiv |
| Sound-Playback | optionale Inventarvollwarnung vertonen | `InventoryFullWarning` | aktiv |

### 6. Externe Integrationen außerhalb von Minecraft/Fabric

| Integration | Konkrete Nutzung | Klassen | Status |
| --- | --- | --- | --- |
| `HttpURLConnection` zu OPSucht-API | Marktpreise und Merchant-/Shardwerte laden | `PriceFetcher`, `ShardRateFetcher` | aktiv |
| `HttpClient` zu Modrinth | Versionsprüfung | `VersionCheck` | aktiv |
| Discord IPC | Rich Presence | `PresenceBuilder` | vorhanden, aber nicht gestartet |
| Mod Menu API | externer Einstieg in `ConfigScreen` | `ModMenuApiImpl` | aktiv |

## Konkrete Priorisierung fuer `Visotaris OPMod`

Wenn `Visotaris OPMod` dieselbe Problemklasse bearbeiten soll, aber sauberer aufgebaut sein soll, dann sind fuer den ersten belastbaren Nachbau diese Einhaengepunkte Pflicht:

- `ClientModInitializer.onInitializeClient`
- `ClientCommandRegistrationCallback.EVENT`
- `ClientSendMessageEvents.MODIFY_COMMAND`
- `ClientTickEvents.END_CLIENT_TICK`
- `ItemTooltipCallback.EVENT`
- `ScreenEvents.AFTER_INIT`
- `ScreenEvents.afterRender`
- Mixins fuer Chat-Parsing, Screen-Klickabfang und optional Command-Intercept
- `HudElementRegistry.attachElementAfter`
- `KeyBindingHelper.registerKeyBinding`

Optional, aber bewusst spaeter:

- ModMenu-Integration
- Rich Presence
- Idle-Render-Optimierung
- alternatives generisches UI-Framework

Bewusst vermeidbar aus heutiger Sicht:

- doppeltes Settings-System
- parallele Config-Key-Namenswelten
- tote Altpfade ohne klare Migrationsstrategie

## Zielarchitektur fuer `Visotaris OPMod`

Der folgende Abschnitt ist kein Rueckblick auf OPMOD, sondern eine konkrete Zielarchitektur, die sich aus der Analyse ableitet. Ziel ist:

- dieselbe Problemklasse loesen
- die Einhaengepunkte bewusst und sauber organisieren
- keine historisch gewachsene Doppelstruktur uebernehmen
- OPSUCHT-/Visotaris-OPMod-spezifische Logik klar von Minecraft-Hooks trennen

### Leitprinzipien fuer `Visotaris OPMod`

- genau ein Settings-System
- genau ein Runtime-Hook-Orchestrator
- genau eine Screen-/Menu-Kontextauflosung
- keine Businesslogik in Mixins
- keine verstreuten Config-Keys ohne Migrationspfad
- jeder Hook soll nur adaptieren, nicht entscheiden

## Empfohlene Paketstruktur

### `dev.noopmod`

Zweck:

- Entry Points
- zentrale Verdrahtung
- Lifecycle-Start

Empfohlene Dateien:

- `NoOpMod.java`
- `NoOpModClient.java`
- `ModMenuApiImpl.java`

### `dev.noopmod.bootstrap`

Zweck:

- registriert alle Hooks, Commands, Services und UI-Bausteine

Empfohlene Dateien:

- `ClientBootstrap.java`
- `HookBootstrap.java`
- `UiBootstrap.java`
- `CommandBootstrap.java`

### `dev.noopmod.config`

Zweck:

- Persistenz
- Defaults
- Migration alter Keys
- Zugriffsschicht

Empfohlene Dateien:

- `ConfigManager.java`
- `ConfigSchema.java`
- `ConfigDefaults.java`
- `ConfigMigrator.java`
- `ConfigKeys.java`

### `dev.noopmod.config.ui`

Zweck:

- genau ein aktiver Settingsscreen
- Config-Widgets
- Kategorienavigation

Empfohlene Dateien:

- `ConfigScreen.java`
- `CategorySidebarWidget.java`
- `ConfigEntryListWidget.java`
- `entry/BooleanEntry.java`
- `entry/IntegerSliderEntry.java`
- `entry/EnumEntry.java`
- `entry/ActionEntry.java`
- `entry/MultiSelectEntry.java`

### `dev.noopmod.model`

Zweck:

- zustandsarme, transportable Runtime-Modelle

Empfohlene Dateien:

- `MarketPrice.java`
- `ShardRate.java`
- `ItemFingerprint.java`
- `TrackedJobState.java`
- `InventoryValuation.java`
- `MenuContext.java`
- `PendingCommandAction.java`

### `dev.noopmod.service`

Zweck:

- gesamte Businesslogik

Empfohlene Dateien:

- `MarketSyncService.java`
- `MerchantSyncService.java`
- `TooltipValueService.java`
- `InventoryValuationService.java`
- `ContainerValuationService.java`
- `JobTrackerService.java`
- `PendingCommandService.java`
- `MenuActionService.java`
- `MenuContextService.java`
- `CommandRewriteService.java`
- `SessionLifecycleService.java`

### `dev.noopmod.service.fingerprint`

Zweck:

- robuste Item-Schluessel fuer Markt-/Merchant- und Spezialitems

Empfohlene Dateien:

- `ItemFingerprintService.java`
- `MerchantSourceNormalizer.java`
- `ItemDisplayNameExtractor.java`
- `CustomModelDataExtractor.java`

### `dev.noopmod.service.opsucht`

Zweck:

- alle server- oder projektbezogenen Heuristiken an einer Stelle sammeln

Empfohlene Dateien:

- `OpsuchtCommandCatalog.java`
- `OpsuchtScreenTitles.java`
- `OpsuchtJobPatterns.java`
- `OpsuchtMenuRules.java`
- `OpsuchtItemRules.java`

Hinweis:

- falls `Visotaris OPMod` spaeter mehrere Server oder Profile unterstuetzen soll, ist das der richtige Abstraktionspunkt

### `dev.noopmod.cache`

Zweck:

- klar getrennte Laufzeit-Caches

Empfohlene Dateien:

- `MarketCache.java`
- `ShardCache.java`
- `TooltipCache.java`
- `ValuationCache.java`

### `dev.noopmod.hook`

Zweck:

- kleine Adapterklassen fuer alle Minecraft-/Fabric-Einhaengepunkte

Empfohlene Dateien:

- `tick/ClientTickHook.java`
- `tick/PriceRefreshTickHook.java`
- `session/JoinHook.java`
- `session/DisconnectHook.java`
- `tooltip/TooltipHook.java`
- `screen/ContainerOverlayHook.java`
- `input/KeybindHook.java`
- `input/UseActionHook.java`
- `command/CommandRewriteHook.java`
- `command/CommandInterceptHook.java`

### `dev.noopmod.mixin.client`

Zweck:

- nur dort Mixins, wo Fabric-Events nicht reichen

Empfohlene Dateien:

- `GameMessageMixin.java`
- `PlayerInventoryScreenMixin.java`
- `HandledScreenMixin.java`
- `HandledScreenClickMixin.java`
- `ClientPlayerInteractionMixin.java`
- `HandledScreenAccessor.java`
- `AnvilScreenAccessor.java`

Regel:

- Mixins lesen Zustand und delegieren sofort in Services
- keine fachliche Entscheidungslogik in der Mixin-Klasse

### `dev.noopmod.ui.hud`

Zweck:

- Job-HUD
- HUD-Editor
- Overlayausgabe

Empfohlene Dateien:

- `JobHudOverlay.java`
- `HudLayoutState.java`
- `HudEditorScreen.java`
- `HudPreviewRenderer.java`

### `dev.noopmod.ui.inventory`

Zweck:

- Inventarbuttons
- Info-Panels
- Spezialoverlays im Inventarkontext

Empfohlene Dateien:

- `InventoryButton.java`
- `InventoryButtonManager.java`
- `InventoryShortcutBar.java`
- `InventoryInfoPanel.java`
- `RecipeAreaButton.java`
- `RecipeAreaButtonManager.java`

### `dev.noopmod.ui.container`

Zweck:

- Wert-Overlay fuer Container/Shulker

Empfohlene Dateien:

- `ContainerValueOverlay.java`
- `ContainerOverlayRenderer.java`

### `dev.noopmod.command`

Zweck:

- sichtbare Client-Commands
- interne Bestaetigungscommands

Empfohlene Dateien:

- `CommandRegistrar.java`
- `SettingsCommand.java`
- `TrackerCommand.java`
- `ShardRateCommand.java`
- `ConfirmRenameCommand.java`
- `CancelRenameCommand.java`
- `ConfirmSignCommand.java`
- `CancelSignCommand.java`

### `dev.noopmod.feature`

Zweck:

- Komfort- und Schutzfeatures, die nicht primar API- oder UI-getrieben sind

Empfohlene Dateien:

- `RenameProtectionFeature.java`
- `SignProtectionFeature.java`
- `AnvilInputNormalizerFeature.java`
- `InventoryFullWarningFeature.java`
- `OffhandBlockerFeature.java`
- `ItemRarityHighlightFeature.java`

## Ziel-Dateien fuer Hooks und ihre Verantwortlichkeiten

Die folgende Liste ist fuer die praktische Umsetzung besonders wichtig.

### `NoOpModClient.java`

Soll nur:

- Config initialisieren
- Services erzeugen
- Hook-Bootstraps aufrufen
- UI- und Command-Bootstrap starten

Soll nicht:

- Marktlogik enthalten
- Titelregeln enthalten
- Pending-Command-Logik enthalten

### `hook/tick/ClientTickHook.java`

Soll:

- Tick-Ereignis entgegennehmen
- an `TickOrchestrator` oder einzelne Services delegieren

Soll nicht:

- direkt Preise laden
- HUD-Logik berechnen

### `hook/command/CommandRewriteHook.java`

Soll:

- `ClientSendMessageEvents.MODIFY_COMMAND` registrieren
- an `CommandRewriteService` delegieren

Der Service entscheidet dann:

- ob `pay`
- ob `bank`
- ob spaeter weitere Rewrite-Regeln

### `hook/command/CommandInterceptHook.java`

Soll:

- nur die wirklich sensiblen Faelle vor Versand blockieren
- an `PendingCommandService` delegieren

Pfad:

- Spieler gibt Befehl ein
- Hook erkennt sensiblen Fall
- Service erzeugt `PendingCommandAction`
- UI-/Chat-Ausgabe zeigt Bestaetigung
- interner Command bestaetigt oder verwirft

### `hook/tooltip/TooltipHook.java`

Soll:

- Tooltip-Event registrieren
- nur das aktuelle Item weiterreichen
- an `TooltipValueService` delegieren

### `hook/screen/ContainerOverlayHook.java`

Soll:

- `ScreenEvents.AFTER_INIT` und `afterRender` registrieren
- `MenuContextService` fragen, ob aktueller Screen unterstuetzt wird
- dann `ContainerOverlayRenderer` nutzen

### `mixin/client/GameMessageMixin.java`

Soll:

- empfangene Chatzeile an `JobTrackerService` geben

Soll nicht:

- selbst Regex pflegen
- selbst `TrackedJobState` manipulieren

### `mixin/client/PlayerInventoryScreenMixin.java`

Soll:

- Inventory-UI-Renderer aufrufen
- keinerlei OPSucht-Strings kennen

### `mixin/client/HandledScreenClickMixin.java`

Soll:

- Klick in Screenkoordinaten umrechnen
- `InventoryButtonManager` bzw. `RecipeAreaButtonManager` fragen
- bei Konsumierung abbrechen

### `mixin/client/HandledScreenMixin.java`

Soll:

- ausschließlich zusätzliche Render-Overlays einhängen
- z. B. Rarity-Highlighting

### `mixin/client/ClientPlayerInteractionMixin.java`

Soll:

- nur Offhand- oder andere Interaktionsregeln absichern

## Empfohlene Service-Grenzen

### `CommandRewriteService`

Eingabe:

- roher Command-String

Ausgabe:

- neuer oder unveraenderter Command-String

Verantwortung:

- `pay`-Kurzformen
- `bank`-Kurzformen
- spaetere weitere Rewrite-Regeln

### `PendingCommandService`

Verantwortung:

- `PendingCommandAction` erzeugen
- Timeout pruefen
- Slotwechsel pruefen
- Itemwechsel pruefen
- bestaetigen
- abbrechen

Soll zentral alle sensiblen Befehle abdecken:

- `rename`
- `sign`
- spaeter eventuell `sell`, `auction`, `bid`, `withdraw`

### `MenuContextService`

Verantwortung:

- aktuelle `Screen`-Instanz klassifizieren
- Typen unterscheiden:
  `PLAYER_INVENTORY`
  `ANVIL_INPUT`
  `VANILLA_CONTAINER`
  `SHULKER_CONTAINER`
  `UNKNOWN_CUSTOM_CONTAINER`

Zusatzdaten:

- Screentitel
- Handler-Typ
- Translation-Key
- bekannte serverbezogene Titelmatches

### `ItemFingerprintService`

Verantwortung:

- Registry-ID lesen
- Anzeigenamen lesen
- `custom_model_data` lesen
- daraus `normalizedKey` erzeugen

Diese Klasse ist fuer `Visotaris OPMod` einer der wichtigsten Architekturpunkte.

### `InventoryValuationService`

Verantwortung:

- Spielerinventar bewerten
- optional mit Shulkerinhalten
- Markt- und Shardwert kombinieren

### `ContainerValuationService`

Verantwortung:

- Container-Slots bewerten
- Spielerinventaranteile sauber ausnehmen
- nur passende Menuekontexte verarbeiten

### `JobTrackerService`

Verantwortung:

- Chatmuster erkennen
- `TrackedJobState` aktualisieren
- `TimeTracker` oder aequivalente Laufzeitdaten fuehren

Wichtig:

- serverbezogene Patterns nicht in die Mixin-Klasse, sondern in `service.opsucht`

## Konkrete Hook-Dateien, die du sehr wahrscheinlich wirklich brauchst

Minimal belastbarer Satz fuer Version `0.1` von `Visotaris OPMod`:

- `NoOpModClient.java`
- `config/ConfigManager.java`
- `config/ui/ConfigScreen.java`
- `model/MarketPrice.java`
- `model/ShardRate.java`
- `model/ItemFingerprint.java`
- `model/TrackedJobState.java`
- `service/MarketSyncService.java`
- `service/MerchantSyncService.java`
- `service/TooltipValueService.java`
- `service/InventoryValuationService.java`
- `service/JobTrackerService.java`
- `service/PendingCommandService.java`
- `service/MenuContextService.java`
- `service/CommandRewriteService.java`
- `service/fingerprint/ItemFingerprintService.java`
- `service/opsucht/OpsuchtCommandCatalog.java`
- `service/opsucht/OpsuchtScreenTitles.java`
- `hook/tick/ClientTickHook.java`
- `hook/tooltip/TooltipHook.java`
- `hook/screen/ContainerOverlayHook.java`
- `hook/command/CommandRewriteHook.java`
- `mixin/client/GameMessageMixin.java`
- `mixin/client/PlayerInventoryScreenMixin.java`
- `mixin/client/HandledScreenClickMixin.java`
- `mixin/client/HandledScreenMixin.java`
- `ui/hud/JobHudOverlay.java`
- `ui/hud/HudEditorScreen.java`
- `ui/inventory/InventoryButtonManager.java`
- `ui/container/ContainerValueOverlay.java`
- `command/CommandRegistrar.java`

## Empfohlene Implementierungsreihenfolge fuer `Visotaris OPMod`

### Stufe 1: Fundament

Baue zuerst:

- `ConfigManager`
- `ConfigScreen`
- `CommandRegistrar`
- `MarketSyncService`
- `MerchantSyncService`
- `ItemFingerprintService`

Ergebnis:

- stabile Datenbasis
- kein UI-Chaos
- klare Modellgrenzen

### Stufe 2: Werte und Tooltips

Baue dann:

- `TooltipHook`
- `TooltipValueService`
- `InventoryValuationService`
- `ContainerValuationService`
- `ContainerOverlayHook`

Ergebnis:

- Markt-/Shardwerte funktionieren
- erste echte OPSucht-/No-OP-relevante Mehrwerte sichtbar

### Stufe 3: Commands und Schutz

Baue danach:

- `CommandRewriteHook`
- `CommandRewriteService`
- `PendingCommandService`
- `Confirm*/Cancel*Commands`

Ergebnis:

- Komfort und Schutz greifen stabil

### Stufe 4: Inventar-UI und HUD

Baue dann:

- `InventoryButtonManager`
- `InventoryInfoPanel`
- `JobHudOverlay`
- `HudEditorScreen`
- `PlayerInventoryScreenMixin`
- `HandledScreenClickMixin`

Ergebnis:

- nutzbares Frontend
- sichtbare Schnellzugriffe
- HUD ordentlich verschiebbar

### Stufe 5: Serverregeln sauber kapseln

Baue oder extrahiere dann:

- `OpsuchtCommandCatalog`
- `OpsuchtScreenTitles`
- `OpsuchtJobPatterns`
- `OpsuchtMenuRules`

Ergebnis:

- alle projektspezifischen Regeln sind zentral
- spaetere Server-/Profilvarianten werden leichter

### Stufe 6: nur wenn wirklich noetig

Erst spaeter:

- ModMenu-Einbindung
- Presence
- Idle-Render-Optimierung
- generisches UI-Komponentenframework

## Was `Visotaris OPMod` bewusst besser machen sollte als OPMOD

- keinen doppelten Settings-Pfad zulassen
- keine alten Keys ohne Migration mitziehen
- keine serverbezogenen Titel oder Regex direkt in Mixins schreiben
- keine Bewertungslogik auf Tooltip, Inventar und Container duplizieren
- keine unverdrahteten Features ohne Statuskennzeichnung im Code liegenlassen

## Praktische Startempfehlung

Wenn du sofort anfangen willst, ist die sinnvollste erste Arbeitsliste:

1. `NoOpModClient`, `ConfigManager`, `ConfigScreen`
2. `MarketSyncService`, `MerchantSyncService`, `ItemFingerprintService`
3. `TooltipHook`, `TooltipValueService`
4. `JobTrackerService`, `GameMessageMixin`, `JobHudOverlay`
5. `CommandRewriteHook`, `PendingCommandService`
6. `InventoryButtonManager`, `ContainerValueOverlay`, `PlayerInventoryScreenMixin`

Damit hast du sehr schnell einen Kern, der bereits den groessten praktischen Nutzen liefert, aber architektonisch sauberer bleibt als der analysierte Bestand.

### Kotlin-spezifische Hinweise

Kotlin eignet sich hier gut fuer:

- `data class`-Modelle
- versiegelte Action-Typen fuer Pending-States
- Coroutines fuer periodische Synchronisation
- klarere Parser und Mapper

Fabric/Mixin-nahe Hooks koennen trotzdem in Java bleiben, wenn du Interop so einfacher findest.

## Was du fuer einen Nachbau zuerst priorisieren solltest

### Phase 1

- Config-Backend
- Markt- und Merchant-Client
- Caches
- Tooltip-Erweiterung
- Item-Fingerprint-Normalisierung

### Phase 2

- Inventar- und Containerbewertung
- Jobtracker-Datenmodell
- Chatparser
- HUD
- MenuContext-Resolver fuer Inventar, Container und Anvil

### Phase 3

- Schutzlogik fuer `/rename` und `/sign`
- Buttons und Keybinds
- Settings-GUI
- Command-basierte Menueaktionen fuer OPSUCHT

### Phase 4

- Komfortfeatures wie Inventar-voll-Warnung, Rarity-Highlighting, optionale Presence
- gezielte Spezialbehandlung fuer besonders haeufige OPSUCHT-Custommenues

## Punkte mit erhöhter fachlicher Unschaerfe

Ohne belastbare interne Entwicklungsdokumentation bleiben insbesondere diese Punkte fachlich sensitiv:

- exakte Semantik der Merchant-`source`-Strings
- warum Jobwerte im Tracker mit `/ 2.0` behandelt werden
- welche OPSUCHT-Chatformate stabil und welche nur zufaellig passend sind
- welche Features im Code vorhanden, aber derzeit gar nicht aktiv verdrahtet sind
- welche serverseitigen Menues wirklich immer Vanilla-Container bleiben und welche nur oberflaechlich so aussehen
- ob bestimmte OPSUCHT-Custommenues absichtlich nicht in `priceOverlay` einbezogen werden
- wie stabil Titel wie `Preis setzen` oder `Sofortkaufpreis` langfristig auf dem Server sind

Das sind genau die Punkte, die du in einer eigenen Mod abstrahieren und testbar machen solltest.

## Praktische Empfehlung fuer ein eigenes Projekt

Wenn das Ziel eine eigenstaendige, nachvollziehbare und technisch wartbare Implementierung ist, dann:

- keine dekompilierten OPMOD-Klassen kopieren
- stattdessen Verhalten und Datenfluesse neu implementieren
- Serverannahmen explizit dokumentieren
- Parser, Mapper und API-Adapter als eigene, klar getrennte Schichten bauen
- jede OPSUCHT-Heuristik an einer Stelle zentral halten

## Anhang: Umfang des geprueften Systems

Nachgewiesener benannter Umfang unter `dev.opmod`:

- `105` benannte Typen
- ca. `542` Felder
- ca. `722` Methoden-/Konstruktor-Signaturen

Die zugrunde liegende Detailpruefung beruhte auf Klasseninventar, Signaturauswertung, Bytecode-Inspektion und paralleler Dekompilation mehrerer Kernklassen.

Fuer die aktuelle Erweiterung wurden zusaetzlich insbesondere diese Klassen erneut dekompiliert und gegengeprueft:

- `OPMODClient`
- `ButtonRegistrar`
- `RecipeAreaButtonRegistrar`
- `PlayerInventoryScreenMixin`
- `HandledScreenClickMixin`
- `priceOverlay`
- `PriceFetcher`
- `ShardRateFetcher`
- `AnvilPriceNormalizer`
- `GameMessageMixin`
