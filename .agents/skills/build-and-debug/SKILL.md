---
name: build-and-debug
description: Use when a build fails, the game crashes, a feature does not work in-game, or before claiming a change works. Runs Gradle, reads logs and crash reports, finds the root cause, and only then proposes a fix.
---

# Build and debug

Never guess. Find the root cause from evidence first.

## Verify a change

1. `./gradlew build` (use `./gradlew compileJava` for a faster check while iterating).
2. If data generation changed: `./gradlew runData`, then build again.
3. For gameplay changes: `./gradlew runClient` or `runServer`. If you cannot run the game yourself, give the user exact steps to test and what they should see.
4. Do not say something works unless it built and, where relevant, was tested.

## When the build fails

1. Read the **first** error, not the last. Later errors are often follow-ups.
2. Classify it:
   - Missing symbol / wrong signature: probably the wrong API or version. Check the decompiled sources of the project's Minecraft/NeoForge version. Do not guess method names.
   - Mapping errors: confirm Mojang names (and Parchment if used).
   - Gradle/dependency errors: check `build.gradle(.kts)`, `gradle.properties`, Java version (21), and that the NeoForge version matches the Minecraft version.
   - Resource errors: check `neoforge.mods.toml` and the `META-INF` path.
3. Fix the cause, rebuild, repeat. Do not suppress errors or add hacks.

## When the game crashes or misbehaves

1. Read `run/crash-reports/` (newest file) and `run/logs/latest.log`.
2. In a crash report, read the **exception message** and the **first stack frames that belong to our mod package**. The "Suspected Mod(s)" section is a hint, not proof.
3. Search the log for our mod ID, `ERROR`, `WARN`, `Missing`, `Failed to load`.
4. Common causes:
   - Registry/registration order or missing registration.
   - Client-only class loaded on the server (`NoClassDefFoundError`, `Dist` issues).
   - Missing model, texture, lang, loot table or tag (look for "Missing" / "Unable to load" lines).
   - Wrong `ResourceLocation` namespace or path (case, typos).
   - Mixin target or signature errors (see `mixin-helper`).
   - Codec / StreamCodec mismatch between client and server (networking).
5. State your hypothesis and the evidence for it before changing code. If the evidence is thin, add a targeted log line, reproduce, and read again.

## Report format

```
Symptom: ...
Evidence: <log/crash lines>
Root cause: ...
Fix: <files changed>
Verified: <what was run and the result>
```

## Rules

- Never delete the `run/` folder or the user's worlds without asking.
- Remove temporary debug logging after the fix.
- If you cannot reproduce it, say so and ask for the crash report or log.
