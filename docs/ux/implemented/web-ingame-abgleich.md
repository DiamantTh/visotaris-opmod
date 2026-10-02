# Web-UX und native Ingame-UX: Abgleich für 1.2.0-pre.1

Die Web-Quellen unter `webui/src/pages/` und die gemeinsame native Ansicht `IngameUxScreenBase` wurden gegen die lokalen Datenmodelle geprüft. Die PDF „Visotaris-UX-AC-Minecraft-OPSUCHT.pdf“ bleibt die visuelle Referenz.

| Bereich | Web-UX | Native Ingame-UX nach Korrektur | Einordnung |
| --- | --- | --- | --- |
| Markt | Suche, Kategorien, Sortierung nach Item/Kauf/Verkauf/aktiven Aufträgen, Top-Aktivität, Charts | Suche nach Key oder lokalisiertem Namen, Kategorien, Sortierung nach Name/Kauf/Verkauf/aktiven Kauf-, Verkaufs- und Gesamtaufträgen, Cache-Alter | Sortierung ergänzt. Aktive Aufträge sind kein abgeschlossenes Handelsvolumen. |
| Shard/Redcoins/Händler | Zielwährung, Suche, Materialname aus `displayName`, Sortierung nach Name/Kurs/Basis/Trend | Zielwährung, Suche, dieselbe `displayName`-Priorität, Sortierung nach Name/Kurs/Basis/Abweichung | `paper#625`, `#626`, `#635` tragen unterschiedliche API-Anzeigenamen; Vanilla-Papier ist für sie kein fachlicher Name. |
| Preisalarme | Regeln anlegen, ändern, pausieren, löschen; Bedingung, Schwelle, Wiederholung, Cooldown, Rearm, HUD/Web-Kanal; Ereignisse | Dieselben Regeln und Optionen mit lokalem Item-Suchfeld, Bestätigung vor Löschen und globalem Schalter | Gleicher `ConfigManager` und `PriceAlertInputValidator`; keine zweite Engine. Ausführliche Ereignisliste bleibt im Web. |
| Tooltips | Markt-Kauf/Verkauf, Shard/Händler, Datenalter, veraltete Daten und Altersgrenze | Dieselben Schalter; Altersgrenze über kompakte Auswahlschritte | Slot-Tooltip bleibt getrennt von permanentem HUD, Alarm-HUD und Container. |
| System | Cachezustand, Clientinfo, geschützte Netzwerk-/Zugangseinstellungen | Cachezustand, Minecraft-Version, lokaler Web-Status, expliziter Refresh und vorhandene Unterseiten | Zugangsdaten/Proxy/Webhook-Werte erscheinen nicht in Karten. |

Bewusst Web-only bleiben Preisverlaufs-Charts und deren bei Bedarf nachladender Verlauf, die große Markttabelle mit Detail-Links sowie die ausführliche Ereignishistorie. Die native Oberfläche liest beim Öffnen und Navigieren nur bereits vorhandene Caches. Der native Alarm-Editor kommuniziert weder mit dem Webserver noch mit OPSUCHT, sondern speichert direkt über den gemeinsamen ConfigManager.

Bezeichnungsregel: Marktitems nutzen intern Registry-/API-Keys und sichtbar die aktive Minecraft-Sprache. Händler-/Shard-Custom-Items zeigen vorrangig den `displayName` aus dem bestehenden `ShardRate`, wie im Web. `Gräbergemisch` (`paper#626`), `Holzbündel` (`paper#625`) und `Steinplatten` (`paper#635`) wurden anhand des gespeicherten API-Caches geprüft; die Preise wurden nicht als Zuordnungsschlüssel verwendet.
