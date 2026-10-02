# Auktionshaus

Visotaris behandelt den OPSUCHT-Marktplatz und das Auktionshaus als getrennte, nur lesbare Datenquellen.

- Marktplatz: `/market/prices`, reguläre Registry-Items, Kauf-/Verkaufspreise und Inventar-/Shulkerwert.
- Auktionshaus: `/auctions/categories`, `/auctions/active` und `/auctions/stream`; aktive Auktionen, Kategorien und Live-Änderungen.

Beim Clientstart lädt Visotaris Kategorien und aktive Auktionen. Anschließend hält ein SSE-Stream den lokalen `AuctionCache` aktuell. Die Ereignis-ID wird für Wiederverbindungen verwendet; `stream.reset` lädt die vollständige aktive Liste erneut. Es gibt keine Biet-, Kauf-, Erstell- oder Abbruchfunktion.

Custom-Stacks mit eigenem Namen, Itemnamen, Lore oder Custom-Model-Data erhalten nicht blind den Preis ihres Vanilla-Trägermaterials. Normale Gegenstände – einschließlich normaler Spawn-Eier – bleiben Marktitems. Verzauberungen oder Schaden allein machen einen Stack nicht zu einem Custom-Item.

In serverseitigen Menüs mit Markt-, Auktions-, Händler- oder Shop-Titel zählen nur Slots des tatsächlichen Spielerinventars zur Overlay-Bewertung. Angebots- und Menü-Slots werden nicht als Besitz gezählt.

Offene API-Details: Die aktuell beobachtete Stream-Verbindung lieferte `stream.reset`. Die Implementierung verarbeitet außerdem die dokumentierten `auction.*`-Ereignisse als vollständige Auktions-Snapshots; falls künftige Events nur Teilfelder enthalten, muss die API diese entweder ergänzen oder Visotaris eine gezielte Snapshot-Erneuerung erhalten.
