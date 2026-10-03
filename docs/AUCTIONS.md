# Auktionshaus

Visotaris behandelt den OPSUCHT-Marktplatz und das Auktionshaus als getrennte, nur lesbare Datenquellen.

- Marktplatz: `/market/prices`, reguläre Registry-Items, Kauf-/Verkaufspreise und Inventar-/Shulkerwert.
- Auktionshaus: `/auctions/categories`, `/auctions/active` und `/auctions/stream`; aktive Auktionen, Kategorien und Live-Änderungen.

Eine frische Installation startet **keinen dauerhaften Stream**. Die native Minecraft-UX
und die Web-UX lesen dieselbe `AuctionCache`-Instanz. „Auktionen jetzt aktualisieren“
lädt Kategorien und `/auctions/active` ausdrücklich im Hintergrund; Suche, Filter,
Sortierung und Detailansicht arbeiten anschließend lokal. Die aktive UID ist der
Schlüssel des Caches, nie Itemname, Preis, Material oder Slotposition.

Optionale Liveupdates sind unter `auctions.liveUpdatesEnabled` konfigurierbar und
standardmäßig AUS. Die Einstellung wird gemeinsam für Minecraft- und Web-UX gespeichert.
Wenn sie AN ist, lädt Visotaris zuerst einen `/active`-Snapshot und verbindet danach
`/auctions/stream`. Für lokale Entwicklung/Tests kann separat
`VISOTARIS_AUCTIONS_STREAM_DEV=true` gesetzt werden; das ist keine Endnutzeroption und
ändert die gespeicherte Konfiguration nicht.

Der SSE-Request setzt ausdrücklich `Accept: text/event-stream`; gemeinsame HTTP-Defaults
überschreiben diesen Header nicht. Keepalive-Kommentare wie `:ping` werden ignoriert.
Die SSE-Event-ID wird für Wiederverbindungen (`Last-Event-ID`) verwendet, während
`data.uid` die fachliche Auktion identifiziert. `stream.reset` lädt `/active` erneut.
Gebots- und Update-Events aktualisieren dieselbe UID einschließlich eines geänderten
`endTime`. Verkäufe, Sofortkäufe, Ablauf und Abbruch entfernen das Angebot aus der
aktiven Liste und halten die finale Antwort begrenzt im lokalen Cache.

Die lokale Web-API liest dieselbe `AuctionCache`; Web- und Ingameansichten bauen keine
separate Datenbeschaffung oder Regelbasis auf. Änderungen am Cache werden von beiden
Oberflächen beim nächsten lokalen Lesen sichtbar.

Die API-URL `item.icon` ist die primäre Quelle für OPSUCHT-Custom-Icons. Visotaris lädt
Icons asynchron und dedupliziert Downloads nach SHA-256 einer normalisierten HTTPS-URL
von `opsucht.net`-Hosts oder dem beim Live-API-Abgleich beobachteten Bildhost
`img.mc-api.io`; Redirects werden einzeln gegen dieselbe Allowlist geprüft.
Gültige Bilder werden begrenzt dekodiert, als PNG unter dem lokalen Mod-Cache gespeichert
und im Minecraft-Renderer dynamisch registriert; die Web-UX greift über einen lokalen
UID-Icon-Endpunkt auf dieselben Bytes zu. Solange ein Icon lädt oder ungültig ist, bleibt
das Vanilla-Material beziehungsweise ein neutrales Icon der Fallback. URL-Validierung,
Bildgrößenlimits und ein temporärer Fehlercache verhindern wiederholte Downloads.

Verkäufer-, Höchstbietenden- und Gebots-UUIDs werden vom gemeinsamen `ProfileCache`
asynchron aufgelöst. Zuerst wird Minecrafts `services.profileResolver().fetchById`
verwendet, danach der feste Mojang-Session-Profile-Endpunkt. UUID, letzter Name und
Auflösungszeit werden lokal gespeichert; gleichzeitige Lookups werden zusammengeführt,
Fehler kurz negativ gecacht. Bis zur erfolgreichen Auflösung zeigt die UX eine gekürzte
UUID. Es wird kein Chatbefehl automatisiert eingegeben.

Es gibt keine Biet-, Kauf-, Erstell- oder Abbruchfunktion und keine automatisierte
Minecraft-Serveraktion. Visotaris 1.2.x zielt auf Minecraft 26.x (derzeit 26.2).

Custom-Stacks mit eigenem Namen, Itemnamen, Lore oder Custom-Model-Data erhalten nicht blind den Preis ihres Vanilla-Trägermaterials. Normale Gegenstände – einschließlich normaler Spawn-Eier – bleiben Marktitems. Verzauberungen oder Schaden allein machen einen Stack nicht zu einem Custom-Item.

In serverseitigen Menüs mit Markt-, Auktions-, Händler- oder Shop-Titel zählen nur Slots des tatsächlichen Spielerinventars zur Overlay-Bewertung. Angebots- und Menü-Slots werden nicht als Besitz gezählt.

API-Payloads können vollständige Snapshots oder partielle Felder liefern. Partielle
Events werden feldweise über die vorhandene UID auf den letzten lokalen Snapshot gelegt;
fehlende Felder bleiben erhalten. Nicht beobachtete Live-Events sind weiterhin als
automatisierte Vertragstests zu kennzeichnen und nicht als reale Serverbeobachtung.

### Read-only API-Prüfung

Am 2026-10-03 wurde die öffentliche API mit `curl` geprüft. `/auctions/active` lieferte
1.274 Datensätze mit stabiler `uid`, `item.icon`, Menge, Kategorie, Gebots-/Sofortkauf-
Feldern, Spieler-UUIDs, Lore, Verzauberungen sowie Start- und Endzeit. Der Snapshot
enthielt 1.264 `ACTIVE`, 8 `ENDED` und 2 `CANCELLED`; Visotaris bewahrt explizit
terminale Datensätze in der begrenzten Final-Liste und zählt sie nicht als aktiv.
Die beobachteten Iconhosts waren `items.opsucht.net` und `img.mc-api.io`. Ein separater
`curl -N`-Aufruf an `/auctions/stream` mit `Accept: text/event-stream` blieb verbunden
und lieferte HTTP 200, `Content-Type: text/event-stream`, ein `stream.reset`, `:ping`
Keepalives, mehrere `auction.removed`-Events und ein vollständiges
`auction.instant_bought`-Event. Letzteres enthielt den aktualisierten Endzeitpunkt,
Spieler-UUIDs und Gebotswerte im selben Objekt mit `uid`. Danach wurde der isolierte
Minecraft-26.2-Client mit `VISOTARIS_AUCTIONS_STREAM_DEV=true` (Xvfb, Audio-Nulltreiber)
gestartet. Die Anwendung stellte genau eine SSE-Verbindung her und verarbeitete natürlich
eingehende `auction.created`, `auction.bid_placed`, `auction.removed`, `auction.sold`,
`auction.instant_bought` und `auction.expired`-Events; der Cache wurde dabei aktualisiert.
`auction.updated` und `auction.cancelled` traten in diesem begrenzten Lauf nicht auf und
bleiben durch Vertragstests abgedeckt. UI-Navigation wurde dabei nicht automatisiert.
Die Snapshot-Zahlen sind eine Momentaufnahme, keine feste API-Garantie.
