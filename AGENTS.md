# Repository Guidelines

## Project Structure

Visotaris 1.2.x targets Minecraft 26.x. Shared Java and Kotlin code lives in `src/main/java` and `src/main/kotlin`; Fabric metadata, translations, icons, and bundled assets are in `src/main/resources`. Tests and fixtures belong in `src/test/java` and `src/test/resources`. The `26.x/` Gradle subproject supplies the Minecraft/Loom platform configuration. `webui/` contains the Svelte frontend, and maintained project documentation belongs under `docs/`. Build output in `out/` and local game data in `26.x/run/` are generated or machine-local; do not commit them.

## Build, Test, and Development

- `./gradlew :26.x:test` runs the JUnit 5 test suite for the supported platform.
- `./gradlew buildAll` builds the distributable Minecraft 26.x JAR and copies it to `out/`.
- `./gradlew :26.x:runClient` launches the local development client (including Mod Menu).
- `./gradlew npmBuildWebui` builds the Svelte interface; Gradle also runs it before processing resources.
- `git diff --check` catches whitespace errors before submitting changes.

Minecraft 26.x currently requires Java 25. Use the Gradle wrapper so builds use the repository's configured Gradle version.

## Coding Style and Naming

Keep source files UTF-8. Follow the surrounding Java and Kotlin style, using four-space indentation. Use PascalCase for classes and camelCase for methods, fields, and Kotlin functions. Preserve the existing package namespace, `systems.diath.visotaris_opmod`. Name Java tests after the behavior or component they cover, ending in `Test` (for example, `AuctionCacheTest`). Keep frontend changes within the existing Svelte/Vite structure and avoid introducing dependencies without need.

## Testing Expectations

Add or update focused JUnit 5 tests for behavior changes, especially cache, configuration, validation, and API contracts. Run `./gradlew :26.x:test` and `./gradlew buildAll` for code that affects the mod. For frontend changes, also verify `./gradlew npmBuildWebui`. UI or rendering changes should include a real client check when practical; clearly report checks that could not be performed.

## Commits and Pull Requests

Recent history uses concise imperative subjects, often with Conventional Commit prefixes, such as `feat: ...`, `fix: ...`, and `docs: ...`. Keep each commit focused. Pull requests should summarize user-visible behavior, list tests/builds run, note compatibility or configuration effects, and include client screenshots for visual changes. Do not move or create release tags as part of routine development.

## Configuration and Safety

Keep credentials and machine-specific settings out of the repository; use local Gradle or game configuration. Do not commit runtime data, generated JARs, or secrets. Preserve the read-only/client-only boundary when changing market and auction features.
