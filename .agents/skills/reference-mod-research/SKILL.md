---
name: reference-mod-research
description: Use before implementing any non-trivial feature in this NeoForge 1.21 mod. Finds how vanilla and open-source mods already solve it, checks licenses, summarizes the pattern, and records credits, so we adapt proven solutions instead of building from scratch.
---

# Reference mod research

Goal: learn from existing code, then write our own small implementation. Never start a non-trivial feature without doing this first.

## Steps

1. **Define the question** in one sentence (e.g. "how do other mods sync a block entity's inventory to the client?").
2. **Check vanilla first.** Find the closest vanilla class in the decompiled sources of our dev environment and read it. Often vanilla is the best reference.
3. **Check our own project** for an existing helper or similar feature (`rg` in `src/`).
4. **Find 1 to 3 reference mods** on GitHub:
   - Must target **NeoForge 1.21.x**. Check the branch, tag or `gradle.properties` for the version. Ignore Forge/Fabric-only code.
   - Prefer actively maintained, well-known mods with a clear license.
   - If you cannot browse, ask the user for a repo URL or ask them to clone it.
5. **Clone shallow, outside the project**:
   ```bash
   mkdir -p ../_reference && cd ../_reference
   git clone --depth 1 --branch <branch-or-tag> <repo-url>
   ```
   Never commit the reference clone to our repo.
6. **Locate the relevant code with `rg`** instead of reading everything. Useful patterns:
   - Registration: `DeferredRegister`, `DeferredHolder`, `@Mod(`
   - Networking: `RegisterPayloadHandlersEvent`, `CustomPacketPayload`
   - Capabilities: `RegisterCapabilitiesEvent`
   - Data attachments / components: `AttachmentType`, `DataComponentType`
   - Datagen: `GatherDataEvent`, `BlockStateProvider`, `RecipeProvider`
   - Menus/screens: `RegisterMenuScreensEvent`, `AbstractContainerMenu`
7. **Check the license** (`LICENSE`, `README`, `neoforge.mods.toml`):
   - MIT / Apache-2.0 / CC0 / BSD: adaptation allowed with attribution.
   - LGPL / GPL / AGPL: only if compatible with our license. When unsure, do not copy; reimplement from the idea.
   - No license or "All Rights Reserved": do **not** copy code. Learn the idea only.
   - Textures, models and sounds often have a **different license** than the code. Never copy assets without checking.
8. **Summarize the pattern** for the user (template below), then implement our own version, adapted to our naming, package layout and version.
9. **Record credits** if anything was adapted: add a short comment at the adapted code (`// Adapted from <repo> (<license>)`) and a line in `CREDITS.md`.

## Summary template

```
Question: ...
Vanilla reference: <class/method>
Reference mods: <repo @ tag> (<license>)
Pattern: <3-6 lines on how it works>
What we'll do: <our approach, what we reuse vs write>
Credits needed: yes/no
```

## Rules

- Prefer understanding to copy-paste. Explain the pattern briefly.
- Do not paste large blocks of third-party code. Adapt small, necessary parts only, and only when the license allows it.
- If reference code is for another version or loader, translate it and say what you changed.
- If nothing suitable is found, say so and propose the simplest own solution.
