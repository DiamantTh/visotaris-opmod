# Projektdokumentation

Die versionierte Projektdokumentation liegt zentral in `docs/`.

- `DEPENDENCIES.md`: direkte Abhängigkeiten und Lizenzübersicht.
- `THIRD_PARTY_NOTICES.md`: Lizenz- und Herkunftshinweis zur separat benötigten MaLiLib-Mod.
- `OPMOD-Architektur-und-Bauplan.md`: technische Analyse des OPMOD-Artefakts.
- `ingame-ux-bestandsaufnahme.md`: Funktionsinventar, Menüentscheidung und Umsetzungsstand der nativen Ingame-UX.
- `RELEASE_NOTES.md`: Vorabversions-Notizen und offene Prüfungen für 1.2.0.
- `AUCTIONS.md`: getrennte, ausschließlich lesende Auktionshaus-Synchronisierung und ihre API-Grenzen.
- `ux/`: visuelle UX-Artefakte und Entwürfe, die als Dokumentation geteilt und geprüft werden sollen.

Build-Ausgaben gehören nicht hierher. Gradle-Artefakte werden im ignorierten `out/`-Verzeichnis abgelegt. Die UX-PDF ist ein erzeugtes Konzeptdokument und liegt in `docs/ux/`, damit sie zusammen mit der dazugehörigen Bestandsaufnahme auffindbar und versionierbar bleibt.
