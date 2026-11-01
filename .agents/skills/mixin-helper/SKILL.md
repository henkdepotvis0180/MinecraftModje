---
name: mixin-helper
description: Use before writing any Mixin in this NeoForge 1.21 mod. First checks whether an event, API hook, or subclass can do the job, and if a mixin is really needed, keeps it minimal, correct, and documented.
---

# Mixin helper

A mixin is the last resort, not the first tool. It breaks easily on updates and conflicts with other mods.

## Step 1: avoid the mixin

Check in this order:
1. **NeoForge events** (`NeoForge.EVENT_BUS` / mod bus). Search NeoForge's event classes for the thing you need to hook (e.g. `LivingDamageEvent`, `BlockEvent`, `PlayerInteractEvent`, `ItemTooltipEvent`).
2. **NeoForge extension interfaces** (`IBlockExtension`, `IItemExtension`, `IClientItemExtensions`, ...) that let you override behavior without patching.
3. **Data-driven options**: tags, data maps, loot modifiers (`GlobalLootModifier`), biome modifiers, recipes, data components.
4. **Subclassing or registering your own** block/item/entity instead of changing vanilla ones.
5. **Reference mods**: see how other NeoForge 1.21 mods solve it (`reference-mod-research`).

Only if none of these work, write a mixin and say why in your report.

## Step 2: write it minimal

- Prefer in this order: `@Inject` (with `CallbackInfo`/`CallbackInfoReturnable`), `@ModifyVariable`, `@ModifyArg`, `@ModifyReturnValue`/`@WrapOperation` (MixinExtras, if the project has it), `@Redirect` (careful, only one mod can redirect the same call), and **avoid `@Overwrite`** entirely.
- Target the **smallest** point possible (`@At("HEAD")`, `@At("RETURN")`, or a specific `INVOKE`). Avoid brittle slices and ordinals.
- Use Mojang-mapped names. Look up the exact target method and descriptor in the decompiled sources. Do not guess signatures.
- Put mixins in the project's mixin package and register them in the correct config (`<modid>.mixins.json`), with `client` vs `mixins` lists set properly (client-only mixins in `client`).
- Make sure the mixin config is declared in `neoforge.mods.toml` (`[[mixins]]`).
- Keep logic in the mixin tiny: call a static helper in our own class for the real work.
- Add a comment above each mixin: what it does, why an event/API was not enough, and which vanilla method it targets.
- Use `require`/`defaultRequire` settings so a broken injection fails loudly instead of silently.

## Step 3: verify

1. Build, then run `runClient` (and `runServer` for common mixins). Check the log for mixin errors (`InjectionError`, `Mixin apply failed`, `Critical injection failure`).
2. Test the behavior in-game.
3. Use `build-and-debug` if it crashes.

## Rules

- Never mixin into another mod's classes unless the user explicitly asks.
- Do not use accessors/invokers if NeoForge or vanilla already exposes the member publicly.
- Document every mixin in the report: target, reason, risk when updating Minecraft.
