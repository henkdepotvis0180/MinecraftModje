---
name: add-content
description: Use when adding a new block, item, or simple entity to this NeoForge 1.21 mod. Gives the full checklist (registration, models, lang, loot, recipe, tags, datagen) so nothing is forgotten and JSON is generated instead of hand-written.
---

# Add content (block / item / entity)

Follow the project's existing registration classes and naming. Read one existing block or item first and mirror it.

## Checklist: item

1. Register with the project's `DeferredRegister.Items` (e.g. `ITEMS.registerSimpleItem("name", props)` or `register(...)` with a custom class).
2. Put it in a creative tab via `BuildCreativeModeTabContentsEvent` or the project's tab setup.
3. Item model (datagen `ItemModelProvider`) and texture at `assets/<modid>/textures/item/<name>.png`.
4. Lang entry (datagen `LanguageProvider` or `en_us.json`).
5. Recipe (datagen `RecipeProvider`) if craftable, plus tags if it belongs in any.
6. If it stores data, use **data components**, not NBT.

## Checklist: block

1. Register block and its `BlockItem` (`DeferredRegister.Blocks#registerSimpleBlock` / `registerBlock`, and `DeferredRegister.Items#registerSimpleBlockItem`).
2. Blockstate + block model + item model (datagen `BlockStateProvider`). Texture at `assets/<modid>/textures/block/`.
3. **Loot table** (datagen `BlockLootSubProvider`), otherwise the block drops nothing.
4. Mining tags: `minecraft:mineable/pickaxe` (etc.) and `minecraft:needs_*_tool` via a `BlockTagsProvider`. Without these the block mines slowly or never drops.
5. Lang entry, recipe, creative tab.
6. Transparent/cutout blocks need the right render type in the block model JSON (`"render_type"`).
7. If it needs a block entity or GUI, use the `block-entity-and-gui` skill.

## Checklist: entity

1. Register `EntityType` (builder, size, tracking range) and attributes via `EntityAttributeCreationEvent`.
2. Spawn egg item, lang entry, loot table.
3. Client: renderer in `EntityRenderersEvent.RegisterRenderers` (client-only class), model layer if needed.
4. Natural spawning: biome modifiers/spawn data via datapack JSON or datagen, not hardcoded.
5. If the entity is animated and complex, consider GeckoLib (ask the user before adding a dependency).

## Steps

1. Run the `reference-mod-research` skill if this is anything beyond a trivial copy of an existing pattern.
2. Implement the checklist in small steps.
3. Run `./gradlew runData`, then `./gradlew build`. Check that generated files appear under `src/generated/resources` and are committed if the project does that.
4. Report which files were added or changed and what to check in-game (place/break the block, check drops, tooltip, creative tab, recipe in the book).

## Rules

- Use datagen for repeated JSON. Hand-write JSON only for one-offs.
- Use the project's `MODID` constant and `ResourceLocation.fromNamespaceAndPath(...)`. No hardcoded namespace strings.
- Keep client-only code out of common classes.
