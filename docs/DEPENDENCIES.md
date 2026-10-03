# Visotaris OPMod – Abhängigkeiten und Lizenzen

Dieses Dokument listet alle direkten Abhängigkeiten des Projekts mit ihren jeweiligen Lizenzen auf.
Zuletzt aktualisiert: 2026-10-03. Gültig für Visotaris 1.2.x / Minecraft 26.x (aktuell 26.2).

---

## Build-Plugins (Gradle)

| Abhängigkeit | Version | Lizenz | SPDX-ID |
|---|---|---|---|
| [Fabric Loom](https://github.com/FabricMC/fabric-loom) | 1.17-SNAPSHOT (26.2) | MIT License | `MIT` |
| [Kotlin JVM Gradle Plugin](https://kotlinlang.org/) | 2.4.20 | Apache License 2.0 | `Apache-2.0` |
| [Shadow (GradleUp)](https://github.com/GradleUp/shadow) | 9.4.1 | Apache License 2.0 | `Apache-2.0` |

---

## Laufzeit-Abhängigkeiten

### Minecraft & Plattform

| Abhängigkeit | Version (26.x, aktuell 26.2) | Lizenz | SPDX-ID |
|---|---|---|---|
| [Minecraft](https://www.minecraft.net/de-de/eula) | 26.2 | Minecraft EULA (proprietär) | — |
| [Fabric Loader](https://github.com/FabricMC/fabric-loader) | 0.19.3 | Apache License 2.0 | `Apache-2.0` |
| [Fabric API](https://github.com/FabricMC/fabric) | 0.155.2+26.2 | Apache License 2.0 | `Apache-2.0` |

### Separate Mod-Abhängigkeiten (26.x: implementation)

| Abhängigkeit | Version (26.x, aktuell 26.2) | Lizenz | SPDX-ID |
|---|---|---|---|
| [Mod Menu](https://github.com/TerraformersMC/ModMenu) | 20.0.1 | MIT License | `MIT` |
| [fabric-language-kotlin](https://github.com/FabricMC/fabric-language-kotlin) | 1.13.14+kotlin.2.4.20 | Apache License 2.0 | `Apache-2.0` |
| [MaLiLib](https://modrinth.com/mod/malilib) | 0.29.3 | GNU Lesser General Public License v3.0 only | `LGPL-3.0-only` |

MaLiLib wird als separate, erforderliche Client-Mod für den konfigurierbaren
Mehrfach-Tasten-Hotkey des Ingame-Menüs verwendet. Die gezielte Version ist für Minecraft 26.2 festgelegt. MaLiLib wird nicht in Visotaris-JARs eingebettet
oder verändert. Der Hotkey ist über MaLiLibs Hotkeyverwaltung deaktivierbar.
Siehe auch [Dritthersteller-Hinweise](THIRD_PARTY_NOTICES.md).

### Eingebettete Bibliotheken (shade / shadowJar)

Diese Bibliotheken werden direkt in den Mod-JAR eingebettet und zur Laufzeit
mit relokierten Paketen ausgeliefert (Prefix: `systems.diath.visotaris.shade.*`).
Dadurch sind Konflikte mit anderen Mods ausgeschlossen.

| Abhängigkeit | Version | Lizenz | SPDX-ID | Relokiertes Paket |
|---|---|---|---|---|
| [OkHttp](https://github.com/square/okhttp) | 5.3.2 | Apache License 2.0 | `Apache-2.0` | `…shade.okhttp3` |
| [Okio](https://github.com/square/okio) | (transitiv via OkHttp) | Apache License 2.0 | `Apache-2.0` | `…shade.okio` |
| [night-config (TOML)](https://github.com/TheElectronWill/night-config) | 3.8.1 | GNU LGPL v3.0 | `LGPL-3.0-only` | `…shade.nightconfig` |

> **Hinweis night-config / LGPL-3.0:**  
> Da night-config als LGPL-Bibliothek statisch via Shadow eingebettet wird, muss sichergestellt sein,
> dass Endnutzer die Bibliothek durch eine eigene Version austauschen können (LGPL §6).
> Beim Einbetten in einen Mod-JAR ist dies durch Veröffentlichung des Quellcodes (dieses Repository)
> und die mitgelieferte Gradle-Konfiguration erfüllt – Nutzer können den JAR damit selbst neu bauen.
>
> **Kompatibilitätswarnung:** Das Austauschen gegen eine eigene night-config-Version kann zu Laufzeitfehlern
> führen, wenn die API-Kompatibilität nicht gewährleistet ist (z. B. geänderte Methoden-Signaturen,
> entfernte Klassen oder inkompatible Verhaltensänderungen beim TOML-Parsing). Der Mod wird in einem
> solchen Fall nicht unterstützt.

---

## Kotlin-Standardbibliothek

Die Kotlin-Stdlib sowie Kotlin-Coroutinen werden **nicht** direkt eingebettet,
sondern zur Laufzeit von `fabric-language-kotlin` (FLK) bereitgestellt.
Entsprechende `exclude group: 'org.jetbrains.kotlin'`-Direktiven sind in den Build-Dateien gesetzt.

| Bereitgestellt durch | Enthält | Lizenz |
|---|---|---|
| fabric-language-kotlin 1.13.14+kotlin.2.4.20 | kotlin-stdlib 2.4.20, kotlin-coroutines | Apache License 2.0 |

---

## Lizenz-Übersicht (SPDX)

| SPDX-ID | Vollständiger Name | Typ |
|---|---|---|
| `Apache-2.0` | Apache License, Version 2.0 | Permissiv |
| `MIT` | The MIT License | Permissiv |
| `CC0-1.0` | Creative Commons Zero v1.0 Universal | Public Domain |
| `LGPL-3.0-only` | GNU Lesser General Public License v3.0 only | Copyleft (schwach) |
| — | Minecraft EULA | Proprietär |

---

## Vollständige Lizenztexte

- Apache-2.0: <https://www.apache.org/licenses/LICENSE-2.0>
- MIT: <https://opensource.org/licenses/MIT>
- CC0-1.0: <https://creativecommons.org/publicdomain/zero/1.0/>
- LGPL-3.0: <https://www.gnu.org/licenses/lgpl-3.0.html>
- Minecraft EULA: <https://www.minecraft.net/de-de/eula>
