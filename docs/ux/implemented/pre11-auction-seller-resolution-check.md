# Auktionshaus: sichere Verkäuferauflösung in pre.11

## Verhalten

Die Auktionsliste und ihre Detailansicht bleiben jederzeit aus dem lokalen
`AuctionCache` nutzbar. Sie warten nicht auf Profilauflösungen und lösen beim
Rendern, Scrollen oder Suchen keine solchen Abfragen aus.

Für jede Auktion gilt:

- Eine fehlende oder leere Verkäufer-UUID bedeutet `Anonymer Verkäufer`.
  Weder native noch WebUX starten dafür eine Profilabfrage oder leiten eine
  Identität aus anderen Cache-Daten ab.
- Bei vorhandener UUID und laufendem Eintrag im gemeinsamen `ProfileCache` zeigt
  die native UX `Spielername wird aufgelöst.` bis `...`; die WebUX zeigt
  `Spielername wird aufgelöst …` und einen kleinen animierten Fortschrittsstreifen
  im Verkäuferfeld der Kachel. Die Detailansicht übernimmt denselben Textstatus.
- Nach erfolgreicher Cache-Auflösung wird nur der Verkäufername aktualisiert.
  Ist eine vorhandene UUID nach dem negativen Cache-Ergebnis nicht auflösbar,
  erscheint `Spielername nicht verfügbar`.

## Prüfung

`ProfileCacheTest` blockiert einen kontrollierten asynchronen Resolver und prüft,
dass `isResolving` während des Laufs wahr und nach dem Abschluss falsch ist.
`AuctionSellerNamesTest` deckt anonym, noch nicht aufgelöst, laufende Auflösung
und erfolgreichen Namen ab. Der WebUI-Produktionsbuild wurde mit dem neuen
Statusfeld `sellerResolution` erzeugt; die API liest dafür ausschließlich den
vorhandenen Cache- beziehungsweise In-flight-Status.

Die native Darstellung nutzt dieselbe Abfrage ohne HTTP auf dem Render-Thread.
Eine echte externe Profilauflösung wurde für diesen Test nicht erzwungen, damit
die Prüfung keinen zusätzlichen Netzverkehr auslöst.
