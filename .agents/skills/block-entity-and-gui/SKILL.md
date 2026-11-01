---
name: block-entity-and-gui
description: Use when adding a block that stores data, ticks, or has an inventory or GUI on NeoForge 1.21. Covers BlockEntity, saving/loading, client sync, item handler capabilities, menu, and screen registration.
---

# Block entity, capabilities, menu and screen (NeoForge 1.21)

Start by reading a similar vanilla block (e.g. furnace, chest, hopper) and our own existing block entities. Mirror the project's structure.

## 1. Block entity

1. Class extends `BlockEntity`. Constructor takes `(BlockPos, BlockState)` and passes our `BlockEntityType`.
2. Register `BlockEntityType` with `DeferredRegister<BlockEntityType<?>>`, using `BlockEntityType.Builder.of(MyBE::new, MY_BLOCK.get()).build(null)`.
3. Block class implements `EntityBlock` (or extends `BaseEntityBlock`): `newBlockEntity` and, if it ticks, `getTicker` (separate server and client tick, static `tick` method).
4. **Save/load** (1.21 signatures include the registry lookup):
   ```java
   @Override
   protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
       super.saveAdditional(tag, registries);
       // write data
   }

   @Override
   protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
       super.loadAdditional(tag, registries);
       // read data
   }
   ```
5. Call `setChanged()` after every data change.
6. **Client sync** (only if the client needs the data, e.g. for rendering): override `getUpdateTag(HolderLookup.Provider)` and `getUpdatePacket()` (`ClientboundBlockEntityDataPacket.create(this)`), and call `level.sendBlockUpdated(...)` after changes.
7. Drop contents when the block is removed (`onRemove` / `Containers.dropContents`) if it has an inventory.

## 2. Inventory via capabilities

1. Use `ItemStackHandler` (override `onContentsChanged` to call `setChanged()`). Save it with `serializeNBT(registries)` / `deserializeNBT(registries, tag)`.
2. Expose it through `RegisterCapabilitiesEvent` on the mod bus:
   ```java
   event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, MY_BE_TYPE.get(), (be, side) -> be.getItemHandler(side));
   ```
   Return a restricted wrapper per `side` if only some slots should be accessible from outside (hoppers, pipes).
3. Same pattern for fluids (`Capabilities.FluidHandler.BLOCK`) and energy (`Capabilities.EnergyStorage.BLOCK`).
4. Do not use the old `LazyOptional` / `getCapability` style.

## 3. Menu (server and shared)

1. Class extends `AbstractContainerMenu`. Two constructors: one for the server (with the real handler/`ContainerData`) and one for the client (from the network buffer, with dummy handler).
2. Add slots, then player inventory and hotbar. Implement `quickMoveStack` carefully (copy from a vanilla menu) and `stillValid`.
3. Register `MenuType` with `IMenuTypeExtension.create(MyMenu::new)` in a `DeferredRegister<MenuType<?>>`.
4. Sync simple numbers (progress, energy) with `ContainerData`/`DataSlot`, not custom packets.
5. Open it from the block: on the server, `player.openMenu(new SimpleMenuProvider(...), pos)` (use the overload that writes the position to the buffer if the client menu needs it).

## 4. Screen (client only)

1. Class extends `AbstractContainerScreen<MyMenu>`. Implement `renderBg` (texture) and call `renderTooltip` in `render`.
2. Texture at `assets/<modid>/textures/gui/<name>.png`.
3. Register in `RegisterMenuScreensEvent` on the mod bus, in a **client-only** class (`Dist.CLIENT`).

## Steps

1. Run `reference-mod-research` for a block with a similar GUI if unsure (check license before adapting).
2. Implement block entity, then capability, then menu, then screen. Build after each part.
3. Use `add-content` for the block's model, loot, lang and tags.
4. Test in-game: place, insert/extract with a hopper, break (drops?), save and reload the world, and test on a dedicated server.

## Rules

- Server owns the data. The client menu only displays it.
- Never access client-only classes from the block entity or menu.
- Check `Level#isClientSide` before doing logic.
