# OPSUCHT-Händlersymbole in der Ingame-UX

Die Shard-Ansicht bindet Symbole über die vorhandene `ShardRate`-Quelle ein; Kurs, Zielwährung und fachlicher Anzeigename bleiben aus demselben Cache-Eintrag. Es gibt keine zweite Händler- oder Kursdatenbank.

## Eindeutige Zuordnung

Die lokale Web-Ansicht zeigt `displayName` an und verwendet `source` als Icon-Key. Für die drei Paper-basierten OPSUCHT-Angebote löst der lokale Icon-Endpunkt diesen Key zuerst auf das extrahierte Visotaris-Symbol auf; erst für nicht zugeordnete Quellen wird das Vanilla-Icon des Trägermaterials verwendet. Der echte Cache enthält:

| Cache-Quelle | Anzeigename | UI-Symbol |
| --- | --- | --- |
| `paper#626` | Gräbergemisch | zugeschnitten aus der Händlerübersicht |
| `paper#625` | Holzbündel | zugeschnitten aus der Händlerübersicht |
| `paper#635` | Steinplatten | manuell vorbereitetes OPSUCHT-Icon aus `docs/ux/new-steinplatten.png` |
| `pumpkin_pie` | Kürbiskuchen | Minecraft-Itemrenderer |
| `ghast_tear` | Ghast-Träne | Minecraft-Itemrenderer |
| `glistering_melon_slice` | Glitzernde Melonenscheibe | Minecraft-Itemrenderer |
| `book` | Buch | Minecraft-Itemrenderer |
| `bone_block` | Knochenblock | Minecraft-Itemrenderer |
| `diamond_block`, `netherite_ingot` | lokalisierte Vanilla-Namen | Minecraft-Itemrenderer |

Die drei Custom-Symbole liegen als 32×32-Pixel-PNGs unter `src/main/resources/assets/visotaris_opmod/textures/item/shard/`. Jeweils der Dateiname entspricht dem fachlichen Angebot; alle drei wurden aus `screenshots/01_OPSUCHT_Rohstoffhaendler_Uebersicht.png` des Referenzarchivs extrahiert:

| Laufzeitressource | Referenzbild | Angebot |
| --- | --- | --- |
| `graebergemisch.png` | `01_OPSUCHT_Rohstoffhaendler_Uebersicht.png` | Gräbergemisch (`paper#626`) |
| `holzbuendel.png` | `01_OPSUCHT_Rohstoffhaendler_Uebersicht.png` | Holzbündel (`paper#625`) |
| `steinplatten.png` | manuell vorbereitet aus `docs/ux/new-steinplatten.png` | Steinplatten (`paper#635`) |

Das neue Steinplatten-Quellbild ist 1254×1254 RGB mit dunklem Hintergrund. Für die Laufzeitressource wurde der verbundene dunkle Außenhintergrund transparent freigestellt, pixelart-schonend auf 32×32 skaliert und zentriert. Die native Zuordnung bleibt ausschließlich über den stabilen Cache-Key `paper#635` im OPShards-Datensatz erhalten; der Kurs spielt bei der Iconwahl keine Rolle. Die Quelldatei bleibt als Referenz unter `docs/ux/` erhalten.

Die Extraktion machte die Slotfläche transparent; Slotrand und Hintergrund sind nicht enthalten. Beim Zuschneiden blieb die Original-Pixelart erhalten. Die übrigen acht Archivbilder dienten zur Gegenprüfung der benannten Angebote und Währungen; sie werden nicht als Laufzeitressourcen eingebunden.

Quelle: vom Projektinhaber bereitgestelltes Archiv `Visotaris-OPSUCHT-Rohstoffhaendler-Symbole-Screenshots-benannt.zip`; dessen `README.md` dokumentiert die neun Original-Screenshots und Angebotsnamen. Besonders wichtig ist die Trennung von Vanilla-Träger (`paper`) und OPSUCHT-Angebotsname: In der nativen Ingame-UX und der Web-UX wird für diese drei Einträge nicht das generische Papier-Icon gezeigt.

## Scroll- und Aktionslayout

Der zentrale Inhaltsbereich wird jetzt innerhalb seiner festen Fläche geclippt. Kopf, Seitennavigation, Fußzeile und Alarmeditor-Aktionsleiste bleiben außerhalb des Scrollbereichs. Das Scrollziel wird zeitbasiert interpoliert; Widget-Positionen werden während der Animation aktualisiert, statt bei jedem Mausrad-Impuls die Oberfläche neu aufzubauen. Eingaben außerhalb des sichtbaren Viewports werden deaktiviert.

## Laufzeitprüfung

Die Laufzeitansichten aus Minecraft 1.21.11 sind [Redcoins/Vanilla-Angebote](shards-merchants-corrected.png), [OPShards-Customangebote](shards-merchants-custom-icons.png) und [Knochenblock/Kürbiskuchen](shards-merchants-redcoins-more.png). Dabei wurden echte lokale Cacheeinträge angezeigt. Die gerenderten Ansichten zeigen, dass Vanilla- und extrahierte Symbole im nativen Renderer gemeinsam funktionieren. 26.x wurde gebaut, dort ist die Client-Laufzeitansicht aber noch nicht separat aufgenommen.
