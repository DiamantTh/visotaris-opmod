# Repository Guidelines

These are the project-specific instructions for Visotaris. Apply them together with the global Codex `AGENTS.md`; this file clarifies the repository's architecture and safety boundaries. The current source, tests, and configuration are authoritative: inspect related components and relevant history before declaring a feature missing or replacing existing behavior. Keep implementation findings separate from what older documentation says.

## Project Structure and Platform

Visotaris 1.2.x targets Minecraft 26.x, currently 26.2, and uses Java 25. Use the Gradle wrapper. Shared sources are in `src/main/java`, `src/main/kotlin`, and `src/main/resources`; tests and fixtures are in `src/test/java` and `src/test/resources`. `26.x/` is the Minecraft/Loom subproject. `webui/` contains the Svelte/Vite source, and maintained documentation belongs under `docs/` (notably `docs/AUCTIONS.md`).

`out/`, Gradle build/cache directories, `26.x/run/`, `webui/node_modules/`, logs, and local runtime/cache data are generated or machine-specific and must not be committed. The built WebUI files in `src/main/resources/assets/webui/` are intentionally tracked runtime resources: when relevant, rebuild them from `webui/` and review their diff alongside the source.

## Build, Test, and Client Checks

- `./gradlew :26.x:test` — run the supported platform's JUnit suite.
- `./gradlew buildAll` — build the 26.x distributable JAR into `out/`.
- `./gradlew :26.x:runClient` — launch the local Minecraft development client.
- `./gradlew npmBuildWebui` — build the Svelte UI and its bundled runtime resources.
- `git diff --check` — check whitespace before staging or committing.

For Java/Kotlin, API, cache, or configuration changes, run tests and `buildAll`. For WebUI changes, run `npmBuildWebui`; also run the relevant Gradle checks when integrated into the mod. For Minecraft UX/render changes, run available automated tests and `buildAll`, then check in a real client when practical. For an `AGENTS.md`-only or documentation-only edit, use `git diff --check` and inspect the content; do not run an unnecessary full client build. Report checks that could not be performed accurately.

## Implementation and Data Boundaries

Visotaris is client-only. Market and auction integrations are read-only: do not add automated bids, purchases, auction creation/cancellation, or other server-side write actions without an explicit change in scope. Never expose credentials, tokens, proxy, webhook, or machine-local path data in HTTP responses or logs.

Keep Minecraft UX and Web UX on shared domain services, models, caches, configuration, validation, and state where practical (including `MarketCache`, `AuctionCache`, `ProfileCache`, and `AuctionIconCache`). Different presentation is fine; avoid parallel business logic. UI changes are drafts until the user explicitly saves them.

Navigation, rendering, scrolling, searching, sorting, category changes, and opening details must read local caches only. Network activity is limited to deliberate refreshes, explicitly enabled background functions, or asynchronous cache misses for needed resources; never perform HTTP on the render thread. New-install auction live updates default to off; manual fetch remains the normal path. `VISOTARIS_AUCTIONS_STREAM_DEV=true` is a separate development/test override, not an end-user setting.

Auction identity is `uid`, not item type, price, material, or slot. `/auctions/active` supplies the starting snapshot; stream events match it through `data.uid`. Handle `auction.created`, `auction.bid_placed`, `auction.updated`, `auction.instant_bought`, `auction.sold`, `auction.expired`, `auction.cancelled`, `auction.removed`, and `stream.reset`; `:ping` is a keepalive. When a complete auction object arrives, treat it as the new current state, including a changed `endTime`. A UID represents one auction, so the same item may have multiple UIDs.

Do not classify or value stacks solely by vanilla material. Consider custom name, lore, item name, custom model data, and relevant components without assuming every component-bearing item is custom; preserve recognition of ordinary market items and spawn eggs. Inventory valuation includes only real player inventory slots, never server-GUI slots, and retains recursive Shulker valuation. Prefer cached/API Auction icons with asynchronous deduplicated loading, then vanilla or neutral fallback. Resolve player UUIDs asynchronously through the shared profile cache and available profile services; do not automate chat commands.

Profile lookup priority is local `ProfileCache`, Minecraft's resolver, a suitable web fallback, then shortened UUID. Resolve asynchronously, coalesce duplicate UUID requests, and never automate chat commands.

## UX, Style, and Dependencies

Keep source UTF-8, four-space indentation, and follow the surrounding Java, Kotlin, and Svelte conventions. Use PascalCase for types, camelCase for methods/fields, preserve `systems.diath.visotaris_opmod`, and name Java tests for the behavior ending in `Test` (for example, `AuctionCacheTest`). Add focused JUnit 5 tests for behavior changes. Keep the Minecraft UI native, legible at scaled sizes, and consistent with its dark navy, ice-blue, and Minecraft-green accents. Fixed navigation/actions must remain separate from clipped, scrollable content; preserve German text and real umlauts. See `docs/` for fuller UX guidance rather than duplicating design specifications here.

Avoid dependencies without a concrete need. Review licenses before adopting third-party code or assets; do not copy AGPL, GPL, or ARR material without assessing compatibility and obligations.

## Git and GUI Workflow

Respect the existing worktree: inspect `git status`, understand all relevant changes, and do not include unrelated edits from the user, another session, or an earlier task. Never blindly use `git add .`, `git add -A`, or equivalent broad staging. Explicitly stage only files/hunks for the completed task, then review the entire staged diff before committing. Keep commits focused and use clear Conventional Commit subjects such as `feat:`, `fix:`, or `docs:`. Tested development milestones may be committed and pushed to the existing branch without asking again. Do not force-push, create or move tags, or publish releases as routine development.

Avoid extensive GUI automation on the user's active desktop. Prefer unit/integration tests and direct service/API checks; use an isolated client/display such as Xvfb where practical. Reserve GUI end-to-end checks for behavior that needs them, and avoid long mouse, keyboard, or focus-taking sequences.
