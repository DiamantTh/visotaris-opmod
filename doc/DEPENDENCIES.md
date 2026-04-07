# Visotaris – Abhängigkeiten und Lizenzen

Dieses Dokument listet alle direkten Abhängigkeiten des Projekts mit ihren jeweiligen Lizenzen auf.
Zuletzt aktualisiert: 2026-04-07

---

## Build-Plugins (Gradle)

| Abhängigkeit | Version | Lizenz | SPDX-ID |
|---|---|---|---|
| [Fabric Loom](https://github.com/FabricMC/fabric-loom) | 1.14.6 | MIT License | `MIT` |
| [Kotlin JVM Gradle Plugin](https://kotlinlang.org/) | 2.2.21 | Apache License 2.0 | `Apache-2.0` |
| [Shadow (GradleUp)](https://github.com/GradleUp/shadow) | 9.4.1 | Apache License 2.0 | `Apache-2.0` |

---

## Laufzeit-Abhängigkeiten

### Minecraft & Plattform

| Abhängigkeit | Version (1.21.4) | Version (1.21.11) | Lizenz | SPDX-ID |
|---|---|---|---|---|
| [Minecraft](https://www.minecraft.net/de-de/eula) | 1.21.4 | 1.21.11 | Minecraft EULA (proprietär) | — |
| [Yarn Mappings](https://github.com/FabricMC/yarn) | 1.21.4+build.8 | 1.21.11+build.1 | Creative Commons Zero v1.0 (CC0) | `CC0-1.0` |
| [Fabric Loader](https://github.com/FabricMC/fabric-loader) | 0.18.5 | 0.18.5 | Apache License 2.0 | `Apache-2.0` |
| [Fabric API](https://github.com/FabricMC/fabric) | 0.113.0+1.21.4 | 0.141.3+1.21.11 | Apache License 2.0 | `Apache-2.0` |

### Mod-Abhängigkeiten (modImplementation)

| Abhängigkeit | Version (1.21.4) | Version (1.21.11) | Lizenz | SPDX-ID |
|---|---|---|---|---|
| [Mod Menu](https://github.com/TerraformersMC/ModMenu) | 13.0.0 | 14.0.0 | MIT License | `MIT` |
| [fabric-language-kotlin](https://github.com/FabricMC/fabric-language-kotlin) | 1.13.7+kotlin.2.2.21 | 1.13.7+kotlin.2.2.21 | Apache License 2.0 | `Apache-2.0` |

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

---

## Kotlin-Standardbibliothek

Die Kotlin-Stdlib sowie Kotlin-Coroutinen werden **nicht** direkt eingebettet,
sondern zur Laufzeit von `fabric-language-kotlin` (FLK) bereitgestellt.
Entsprechende `exclude group: 'org.jetbrains.kotlin'`-Direktiven sind in den Build-Dateien gesetzt.

| Bereitgestellt durch | Enthält | Lizenz |
|---|---|---|
| fabric-language-kotlin 1.13.7+kotlin.2.2.21 | kotlin-stdlib 2.2.21, kotlin-coroutines | Apache License 2.0 |

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
