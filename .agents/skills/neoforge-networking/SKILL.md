---
name: neoforge-networking
description: Use when the mod needs client-server communication (syncing data, GUI actions, keybind actions) on NeoForge 1.21. Provides the payload + StreamCodec + registration recipe and the safety rules for handling packets.
---

# NeoForge 1.21 networking

Use `CustomPacketPayload` with `StreamCodec`. Do **not** use the old `SimpleChannel`.

First check whether you really need a packet: block entity sync can use `getUpdateTag`/`getUpdatePacket`, menu data uses `ContainerData`/`DataSlot`, and attachments can sync via NeoForge's attachment sync options. Prefer those over custom packets.

## Recipe

1. **Payload record**:
   ```java
   public record ExamplePayload(BlockPos pos, int value) implements CustomPacketPayload {
       public static final Type<ExamplePayload> TYPE =
           new Type<>(ResourceLocation.fromNamespaceAndPath(MyMod.MODID, "example"));

       public static final StreamCodec<RegistryFriendlyByteBuf, ExamplePayload> STREAM_CODEC =
           StreamCodec.composite(
               BlockPos.STREAM_CODEC, ExamplePayload::pos,
               ByteBufCodecs.VAR_INT, ExamplePayload::value,
               ExamplePayload::new);

       @Override
       public Type<? extends CustomPacketPayload> type() { return TYPE; }
   }
   ```
2. **Registration** on the mod event bus:
   ```java
   @SubscribeEvent
   public static void register(RegisterPayloadHandlersEvent event) {
       PayloadRegistrar registrar = event.registrar("1"); // protocol version
       registrar.playToServer(ExamplePayload.TYPE, ExamplePayload.STREAM_CODEC, ServerPayloadHandler::handle);
       registrar.playToClient(OtherPayload.TYPE, OtherPayload.STREAM_CODEC, ClientPayloadHandler::handle);
   }
   ```
3. **Handlers** run on the network thread. Use `context.enqueueWork(() -> { ... })` before touching game state.
4. **Sending**:
   - Client to server: `PacketDistributor.sendToServer(payload)`
   - Server to one player: `PacketDistributor.sendToPlayer(serverPlayer, payload)`
   - Other targets exist (`sendToPlayersTrackingChunk`, `sendToAllPlayers`, ...). Check `PacketDistributor` in the sources.

## Safety rules

- **Never trust the client.** In a server-bound handler, validate everything: player exists, is in range of the position, the chunk is loaded, the block/entity is the expected type, values are within bounds, the player may do this action.
- Keep client-only handler code in a separate client-only class, and reference it only from client-side registration paths.
- Keep payloads small. Send ids and small values, not whole objects.
- Bump the registrar version string when the packet format changes in an incompatible way.
- Use `ByteBufCodecs` and existing vanilla `STREAM_CODEC`s (BlockPos, ItemStack, ResourceLocation, etc.) instead of manual buffer writes.

## Steps

1. Run `reference-mod-research` for the exact use case if unsure.
2. Implement payload, registration, handler, and sender.
3. Test with `runClient` plus `runServer` (or a dedicated-server run), not just singleplayer, because singleplayer hides many sync bugs.
