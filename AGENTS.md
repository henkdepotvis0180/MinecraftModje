# Repository Guidelines

## Project Structure & Module Organization

Run project commands from this repository root. In the original Windows workspace, the extracted working copy lives in `MinecraftModje-main/`; ZIP archives are not active source trees.

- `helpers/`: Java 21 NPC mod for Minecraft 1.21.1 and NeoForge 21.1.172. Production classes use `src/main/java/nl/henk/helpers/`; mod metadata lives in `src/main/resources/`.
- `helpers/src/test/`: standalone Java core and chat tests.
- `helpers/src/gameTest/`: Minecraft server integration tests and structure fixtures, excluded from release JARs.
- `worldgen/` and `resourcepack/`: datapack JSON and optional naming assets.
- `tools/`, `tests/`, and `docs/`: Python utilities, worldgen tests, and implementation/test notes. Generated packs go into `dist/`.

## Build, Test, and Development Commands

From the repository root:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\helpers\build-windows.ps1
py -m unittest discover -s tests -v
py tools/build_worldgen.py
```

These build the mod with checksum-verified Gradle, run worldgen tests, and generate pack ZIPs respectively.

From `helpers/`, use `.\.tools\gradle-8.14.3\bin\gradle.bat` with:

- `build`: compile and run `coreTest` and `chatTest`.
- `-PgameTests runGameTestServer`: run behavioral tests in an isolated Minecraft world.
- `liveChatTest`: test real local Ollama with installed `qwen2.5:3b`.
- `runClient`: launch the development client.

## Coding Style & Naming Conventions

Use four-space indentation and UTF-8. Follow existing Java conventions: PascalCase classes, camelCase members, UPPER_SNAKE_CASE constants, and package `nl.henk.helpers`. Python uses snake_case functions and `test_*.py` filenames. Keep Minecraft resource identifiers lowercase. No formatter or linter is configured; avoid unrelated formatting changes.

## Testing Guidelines

Add regression coverage for changed behavior. Use `*Test.java` for standalone checks, `WorkerGameTests` for world interactions, and Python `unittest` for pack integrity. No numeric coverage threshold exists. Verify item conservation, ownership, persistence, navigation, and actual block changes. Report visual client checks separately from compilation and server tests.

## Commit & Pull Request Guidelines

History uses descriptive imperative subjects, such as “Add NeoForge helper NPC prototype with local Ollama task planning,” without mandatory prefixes. Keep commits focused. PRs should explain resulting behavior, reference relevant issues, list executed tests and limitations, and include screenshots for UI changes when available.

## Configuration & Safety

Preserve approved worldgen unless explicitly changing it. Keep AI local; never execute arbitrary model-generated commands. Exclude build caches, runtime worlds, logs, and secrets. Test in separate worlds and retain TLS/checksum verification.
