# Umgesetzte Visotaris-Ingame-UX

Aktuelle 26.2-Prüfung für Visotaris 1.2.x: [Hauptmenü, Runtime-Registry und Auktionshaus](26.2-mainmenu-registry-check.md).
Visotaris 1.2.0+ unterstützt Minecraft 26.x. Die folgenden 1.21.11-Aufnahmen bleiben als historische UX-Dokumentation erhalten.

Die folgenden PNGs sind Screenshots der **laufenden nativen Minecraft-Oberfläche** (nicht HTML-Mockups). Aufgenommen mit Minecraft 1.21.11, deutscher Minecraft-Sprache, einer isolierten lokalen Entwicklungsinstanz und den dort geladenen Cache-Snapshots. Die angezeigten Kurse/Auftragszahlen stammen aus dem lokalen Cache; sie sind keine fest codierten Beispieldaten und können sich ändern. Die Web-UX war in dieser Testinstanz deaktiviert, daher zeigt die Alarmseite den echten Leerzustand und verweist auf System-Einstellungen.

| Bereich | Screenshot |
| --- | --- |
| Übersicht | [overview.png](overview.png) |
| Markt | [market.png](market.png) |
| Shard & Händler (Cache-Anfang / Redcoins) | [shards-merchants-corrected.png](shards-merchants-corrected.png) |
| Shard & Händler (OPShards-Customsymbole) | [shards-merchants-custom-icons.png](shards-merchants-custom-icons.png) |
| Shard & Händler (Knochenblock/Kürbiskuchen) | [shards-merchants-redcoins-more.png](shards-merchants-redcoins-more.png) |
| Tooltip-Einstellungen | [tooltips.png](tooltips.png) |
| Preisalarme | [price-alerts.png](price-alerts.png) |
| Schutz & Komfort | [protection-comfort.png](protection-comfort.png) |
| System | [system.png](system.png) |

Herkunft und Zuordnung der OPSUCHT-Händlericons sowie Hinweise zum fixierten Scroll-/Aktionslayout stehen in [shard-symbols.md](shard-symbols.md).
Der dauerhaft erreichbare Alarmeditor nach dem Scrollen ist in [price-alert-editor-scroll.png](price-alert-editor-scroll.png) zu sehen.

## Technische Grenzen und Abweichungen

- Die Screens nutzen das native Minecraft-Rendering und die vorhandene Minecraft-Schrift; es gibt keine Webview, externe Schriftdatei oder Itemdatenbank.
- Der bestehende Öffnen-Keybind bleibt der Einstieg. Die Screens sind im ModMenu über die Mod-Konfiguration und ingame über diesen Keybind erreichbar.
- Die Alarmseite zeigt Regeln, Zustand und globale Aktivierung. Ein nativer Editor verwaltet Item, Bedingung, Schwelle, Wiederholung, Cooldown, Rearm und HUD/Web-Kanal über dieselben gespeicherten Regeln wie die Web-UX.
- Preisverlauf/Charts bleiben ebenfalls web-only, weil die Historien-API bei Cache-Miss Daten nachladen kann. Die Ingame-UX verwendet ausschließlich vorhandene Markt-/Händler-Snapshots.
- Tooltip-Optionen beschreiben den normalen Item-Slot-Tooltip. Permanentes HUD, Alarm-HUD und Containeranzeige befinden sich getrennt unter „Schutz & Komfort“.
- System-/Netzwerkeinstellungen werden nur über ihre vorhandenen dedizierten Ingame-Unterseiten geöffnet. Sensible Werte erscheinen nicht in Statuskarten.
- Die aktive Minecraft-Sprache bestimmt die Lokalisierung regulärer Itemnamen; unbekannte IDs können als API-Key/Cache-Anzeigename erscheinen.

Die älteren Screenshots stammen aus einem 1440×900-Testlauf; die beiden korrigierten Händleraufnahmen aus Minecraft 1.21.11 sind 1600×900. Die Menügeometrie folgt der vom Client gemeldeten GUI-Auflösung und skaliert dynamisch. Ein separater Test auf Windows wurde hier nicht ausgeführt.

Der [Web-vs.-Ingame-Abgleich](web-ingame-abgleich.md) beschreibt die Korrekturrunde für 1.2.0-pre.1. Die Händleraufnahmen wurden nach der Icon-Korrektur neu aus dem laufenden Client erzeugt; die übrigen Bereichsscreenshots sind ältere Vorher-Aufnahmen.
