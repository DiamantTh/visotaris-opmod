# Visotaris pre.6: Navigation im isolierten Minecraft-Client

Praktischer UI-Test der sichtbaren Navigation von Visotaris
`1.2.0-pre.6+mc26.2`. Der Minecraft-26.2-Dev-Client lief auf einem separaten
Xvfb-Display (`:99`) innerhalb einer eigenen systemd-User-cgroup mit
`MemoryHigh=5G` und `MemoryMax=7G`. Der gemessene Peak lag bei etwa 206 MiB;
die Limits wurden nicht erreicht. Der aktive Desktop wurde nicht verwendet.

## Geprüfte Navigation

Alle acht sichtbaren Einträge wurden einzeln angeklickt und vorwärts sowie rückwärts
durchlaufen:

`Übersicht → Auktionshaus → Markt → Preisalarme → Schutz & Komfort → Shard & Händler → System → Tooltips`

Für jeden Wechsel stimmten Zielseite, Überschrift/Inhalt und aktive Markierung.
Es fehlte keine Seite, keine erschien doppelt, und wiederholte Wechsel blieben stabil.
Auch die Wechsel `Übersicht → Auktionshaus → Markt` und
`Markt → Auktionshaus → Übersicht` wurden geprüft. Auktionsdaten waren sichtbar;
Suche und Scrollen blieben funktionsfähig.

Auf einem verkleinerten Fenster (740×430) blieben alle acht kurzen Navigationslabels
lesbar und erreichbar; jeder Eintrag öffnete weiterhin seine richtige Seite.

## MaLiLib-Rückkehr und Grenzen

Vom Systembereich aus öffnete sich der MaLiLib-Hotkeyeditor mit dem sichtbaren Namen
„Visotaris OPMod“. Sowohl Escape als auch „Zurück zu Visotaris“ führten zurück zum
Systembereich, von dem der Editor geöffnet wurde.

Dieser Bericht dokumentiert einen praktischen Navigationstest. Er ist kein zusätzlicher
Build- oder Integrationstest. Es wurden keine Screenshots dieses Durchlaufs als
versionierte Dokumentation aufgenommen; temporäre Testaufnahmen lagen außerhalb des
Repositories.
