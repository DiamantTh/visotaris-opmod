# Responsive WebUX-Datenansichten: pre.9

## Prüfumgebung

Die WebUX wurde am 17. Oktober 2026 mit Chromium 155 im headless Browser und
separatem Browserprofil geprüft. Der lokale Vite-Server stellte synthetische,
vollständig abgefangene API-Fixtures bereit. Es wurden keine Live-API-Daten
abgerufen. Die Browser-Viewports hatten Device Scale Factor 1. Es wurden
1600×900, 1180×820, 740×900 und 420×900 geprüft.

Die Fixtures enthielten 1.200 Auktionen, 180 Marktpreise, je 150 Shard- und
Redcoin-Kurse sowie 300 Händlerzeilen. Das ausgewählte Auktionsbeispiel enthielt
einen langen Itemnamen, langen Verkäufernamen, Lore mit Absatz, vier
Verzauberungen, Startgebot, aktuelles Gebot, Sofortkauf, Gebotsverlauf,
Höchstbietenden, Start-/Endzeit und UID.

## Auktionshaus

- Bei 1600×900 erscheinen zwei Kachelspalten in einem gemeinsamen scrollenden
  Ergebnisbereich neben einer eigenen Detailspalte. Die Kachel-Mindestbreite ist
  24rem; bei 1180×820 passt eine Kachelspalte neben die Details.
- Bis 920 CSS-Pixel Breite wird Master/Detail verwendet: zuerst die Kachelliste,
  nach Auswahl die Detailansicht mit „Zurück zur Liste“. Das wurde bei 740×900
  und 420×900 geprüft.
- Die Karte zeigt Icon, Itemname, Menge, Verkäufer, Kategorie, Gebot, optionalen
  Sofortkauf und Restzeit. Lange Namen werden höchstens zweizeilig, Verkäufernamen
  und Kategorien kontrolliert gekürzt.
- Details gruppieren Auktion, Preise, Beschreibung, Verzauberungen, Gebote sowie
  Zeiten und UID. Lore belegt die verfügbare Breite und erhält Leerzeilen als
  Absatzabstände. Für vier Verzauberungen erschienen „Schärfe V“, „Haltbarkeit
  III“, „Plünderung III“ und „Verbrennung II“. Bei langen Daten scrollt die
  Detailinsel intern.
- Die Fixture-Kacheln enthielten sowohl Gebot als auch Sofortkauf. Alle Preise im
  AH wurden als `$600.000,00` bzw. `$900.000,00` formatiert.

## Markt, Shards, Redcoins und Händler

- Das Marktgrid nutzt `auto-fill` und eine Mindestbreite von 250px bei größeren
  Fenstern (vorher etwa 230px). Bei höchstens 520px fällt sie auf 230px zurück;
  bei 420px füllt eine Karte 372px. Die gemessenen Grid-Spalten waren vier bei
  1600px, drei bei 1180px und eine bei 420px. Die Marktkarte bleibt dadurch
  kompakter als eine AH-Kachel.
- Listen- und Kachelansicht verwenden denselben internen Datenarbeitsbereich. Bei
  1600×900 scrollten 180 Zeilen beziehungsweise Karten im rund 494px hohen
  Datenbereich. Der Tabellenkopf blieb beim Scrollen am oberen Rand. Top 10 und
  Kategorie-Rail blieben erreichbar; auf schmalen Ansichten erscheinen
  Kategoriechips.
- Shards (1180×820), Redcoins (740×900) und Händler (420×900) hielten ihre
  150/150/300 Zeilen in internen Tabellenbereichen mit sticky Kopf und sichtbarer
  Anzahl/API-Stand-Zeile.
- Die sichtbaren Namen lauten `$` für Markt und AH, `OPShards` für Shards und
  `Redcoins` für Redcoins/Händler. Die Suche der WebUX-Quellen nach sichtbaren
  `OPS`-/`RDC`-Bezeichnungen ergab keine weiteren Treffer; Währungstargets
  `ops`/`rdc` werden in der Anzeige als unbekannt behandelt.

## Preisverlauf

Bei 1600×900 stand der große Preisverlauf links, Transaktionen und gehandelte
Items rechts darunter. Bis 1100px wird der Preisverlauf vollbreit und die beiden
kleineren Charts stehen nebeneinander; bis 700px ordnen sie sich untereinander.
ECharts passte Canvas-Breiten bei 1600, 1180, 740 und 420px an. Auf 420px bleibt
der gesamte Inhalt im eigenen Scrollbereich; die Dokumenthöhe blieb bei 900px.

## Viewport, Netzwerk und Nachweise

Für alle geprüften Ansichten entsprachen `documentElement` und `body` der
Viewportgröße; die langen Ergebnislisten verlängerten die Seite nicht. Die
1.200 AH-Kacheln wurden gleichzeitig gerendert. Beim lokalen Suchen und Scrollen
blieb die Zahl der Auction-/Kategorie-/Meta-Datenrequests im Prüfabschnitt bei
drei. Die Profilauflösung wird weiterhin nur bei Auswahl einer Auktion über den
vorhandenen Profilendpunkt angefragt; es wurde keine Anfrage pro Kachel-Render,
Hover oder Scrollen ergänzt.

![Auktionshaus bei 1600×900](pre9-auctions-wide-1600.png)

![Auktionsdetails im schmalen Master/Detail-Modus bei 420×900](pre9-auctions-detail-420.png)

![Markt-Kachelansicht bei 1600×900](pre9-market-grid-1600.png)

![Markt-Listenansicht mit sticky Tabellenkopf bei 1600×900](pre9-market-list-1600.png)

![History-Dashboard bei 1600×900](pre9-history-1600.png)

![History-Dashboard bei 420×900](pre9-history-420.png)

## Restpunkte

Die Screenshots verwenden synthetische Testdaten, keine echten Spieler- oder
Marktdaten. Ein echter Browser mit aktiven Minecraft-Serverdaten wurde in diesem
Lauf nicht verwendet.
