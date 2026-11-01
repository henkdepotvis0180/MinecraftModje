package nl.henk.helpers;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.UuidArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.npc.Villager;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.minecraft.core.registries.Registries;
import java.util.Comparator;
import java.util.concurrent.atomic.AtomicBoolean;

@Mod(HenkHelpers.ID)
public final class HenkHelpers {
    public static final String ID = "henkhelpers";
    private static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, ID);
    public static final DeferredHolder<EntityType<?>, EntityType<Worker>> WORKER = ENTITIES.register("worker", () ->
            EntityType.Builder.<Worker>of(Worker::new, MobCategory.CREATURE).sized(.6F, 1.95F)
                    .clientTrackingRange(10).build(ID + ":worker"));
    private static final ModConfigSpec.Builder CONFIG = new ModConfigSpec.Builder();
    public static final ModConfigSpec.ConfigValue<String> MODEL = CONFIG.comment("Exact locally installed Ollama model name.")
            .define("model", "qwen2.5:3b");
    private static final ModConfigSpec.BooleanValue AI = CONFIG.comment("Allow local model requests via /helper ask.")
            .define("enableLocalAi", true);
    private static final ModConfigSpec SPEC = CONFIG.build();
    private static final AtomicBoolean AI_BUSY = new AtomicBoolean(false);

    public HenkHelpers(IEventBus modBus, ModContainer container) {
        ENTITIES.register(modBus);
        modBus.addListener(this::attributes);
        container.registerConfig(ModConfig.Type.COMMON, SPEC);
        NeoForge.EVENT_BUS.addListener(this::commands);
    }
    private void attributes(EntityAttributeCreationEvent event) {
        event.put(WORKER.get(), Villager.createAttributes().add(net.minecraft.world.entity.ai.attributes.Attributes.FOLLOW_RANGE, 64).build());
    }
    private static Worker worker(ServerPlayer player) {
        return player.serverLevel().getEntitiesOfClass(Worker.class, player.getBoundingBox().inflate(64),
                        candidate -> candidate.isAlive() && candidate.ownedBy(player)).stream()
                .min(Comparator.comparingDouble(player::distanceToSqr)).orElseThrow(() ->
                        new IllegalArgumentException("Geen eigen hulpje binnen 64 blokken. Gebruik /helper spawn."));
    }
    private static int safely(ServerPlayer player, Runnable action) {
        try { action.run(); return 1; }
        catch (IllegalArgumentException exception) {
            player.sendSystemMessage(Component.literal(exception.getMessage())); return 0;
        }
    }
    private static Worker conversationWorker(ServerPlayer player, java.util.UUID id) {
        var entity = player.serverLevel().getEntity(id);
        if (!(entity instanceof Worker npc) || !npc.isAlive() || !npc.ownedBy(player)
                || npc.distanceToSqr(player) > 8 * 8)
            throw new IllegalArgumentException("Ga dichter bij je eigen Henkhulp staan om te praten.");
        return npc;
    }
    private void commands(RegisterCommandsEvent event) {
        var root = Commands.literal("helper")
                .then(Commands.literal("follow").executes(context -> {
                    var p = context.getSource().getPlayerOrException();
                    return safely(p, () -> worker(p).setFollowing(p, true));
                }))
                .then(Commands.literal("stay").executes(context -> {
                    var p = context.getSource().getPlayerOrException();
                    return safely(p, () -> worker(p).setFollowing(p, false));
                }))
                .then(Commands.literal("talk").then(Commands.argument("worker", UuidArgument.uuid())
                        .then(Commands.argument("message", StringArgumentType.greedyString()).executes(context -> {
                            var p = context.getSource().getPlayerOrException();
                            return safely(p, () -> ask(p, conversationWorker(p, UuidArgument.getUuid(context, "worker")),
                                    StringArgumentType.getString(context, "message")));
                        }))))
                .then(Commands.literal("control").then(Commands.argument("worker", UuidArgument.uuid())
                        .then(Commands.argument("action", StringArgumentType.word()).executes(context -> {
                            var p = context.getSource().getPlayerOrException();
                            return safely(p, () -> {
                                var npc = conversationWorker(p, UuidArgument.getUuid(context, "worker"));
                                switch (StringArgumentType.getString(context, "action")) {
                                    case "stop" -> npc.stop("Ik stop.");
                                    case "give" -> npc.receive(p);
                                    case "collect" -> npc.collect(p);
                                    case "forget" -> npc.forgetChat();
                                    case "inventory" -> npc.openSupplies(p);
                                    case "bind" -> npc.bindSupplies(p);
                                    case "follow" -> npc.setFollowing(p, !npc.isFollowing());
                                    case "gap" -> npc.cycleFollowGap(p);
                                    default -> throw new IllegalArgumentException("Onbekende hulpje-actie.");
                                }
                            });
                        }))))
                .then(Commands.literal("spawn").requires(source -> source.hasPermission(2)).executes(context -> {
                    var player = context.getSource().getPlayerOrException();
                    var existing = player.serverLevel().getEntitiesOfClass(Worker.class, player.getBoundingBox().inflate(64), w -> w.ownedBy(player));
                    if (!existing.isEmpty()) { player.sendSystemMessage(Component.literal("Je hebt hier al een hulpje.")); return 0; }
                    var entity = WORKER.get().create(player.serverLevel());
                    if (entity == null) return 0;
                    entity.setOwner(player.getUUID());
                    entity.setCustomName(Component.literal("Henkhulp"));
                    entity.setCustomNameVisible(true);
                    entity.moveTo(player.getX() + 1.5, player.getY(), player.getZ(), player.getYRot(), 0);
                    if (!player.serverLevel().noCollision(entity)) return 0;
                    player.serverLevel().addFreshEntity(entity);
                    player.sendSystemMessage(Component.literal("Hulpje klaar. Geef een bijl via /helper give."));
                    return 1;
                }))
                .then(Commands.literal("give").executes(context -> {
                    var p = context.getSource().getPlayerOrException(); return safely(p, () -> worker(p).receive(p));
                }))
                .then(Commands.literal("collect").executes(context -> {
                    var p = context.getSource().getPlayerOrException(); return safely(p, () -> worker(p).collect(p));
                }))
                .then(Commands.literal("chop").then(Commands.argument("count", IntegerArgumentType.integer(1, 64)).executes(context -> {
                    var p = context.getSource().getPlayerOrException();
                    return safely(p, () -> worker(p).assign(new TaskPlan(TaskPlan.Action.CHOP, IntegerArgumentType.getInteger(context, "count")), p));
                })))
                .then(Commands.literal("build").then(Commands.literal("hut").executes(context -> {
                    var p = context.getSource().getPlayerOrException();
                    return safely(p, () -> worker(p).assign(new TaskPlan(TaskPlan.Action.BUILD, 0), p));
                })))
                .then(Commands.literal("stop").executes(context -> {
                    var p = context.getSource().getPlayerOrException(); return safely(p, () -> worker(p).stop("Ik stop."));
                }))
                .then(Commands.literal("status").executes(context -> {
                    var p = context.getSource().getPlayerOrException();
                    return safely(p, () -> p.sendSystemMessage(Component.literal(worker(p).status())));
                }))
                .then(Commands.literal("forget").executes(context -> {
                    var p = context.getSource().getPlayerOrException(); return safely(p, () -> worker(p).forgetChat());
                }))
                .then(Commands.literal("ask").then(Commands.argument("message", StringArgumentType.greedyString()).executes(context -> {
                    var p = context.getSource().getPlayerOrException();
                    return safely(p, () -> ask(p, StringArgumentType.getString(context, "message")));
                })));
        event.getDispatcher().register(root);
    }
    private static void ask(ServerPlayer player, String message) {
        ask(player, worker(player), message);
    }
    private static void ask(ServerPlayer player, Worker npc, String message) {
        String direct = message.strip().toLowerCase(java.util.Locale.ROOT);
        if (java.util.Set.of("volg mij", "volg me", "kom mee").contains(direct)) { npc.setFollowing(player, true); return; }
        if (java.util.Set.of("blijf hier", "stop met volgen").contains(direct)) { npc.setFollowing(player, false); return; }
        if (!AI.get()) throw new IllegalArgumentException("Lokale AI staat uit in de serverconfiguratie.");
        if (!AI_BUSY.compareAndSet(false, true)) throw new IllegalArgumentException("Het lokale model is nog bezig. Probeer straks opnieuw.");
        var server = player.serverLevel().getServer();
        int generation = npc.generation();
        try {
            Ollama.chat(message, npc.status(), npc.chatMemory.snapshot()).whenComplete((reply, error) -> server.execute(() -> {
                AI_BUSY.set(false);
                if (server.getPlayerList().getPlayer(player.getUUID()) != player) return;
                if (error != null) {
                    player.sendSystemMessage(Component.literal("AI-opdracht mislukt. Controleer Ollama, modelnaam en /helper status. Handmatige opdrachten blijven werken."));
                    return;
                }
                if (!npc.isAlive() || npc.isRemoved() || !npc.ownedBy(player) || npc.level() != player.level()
                        || npc.distanceToSqr(player) > 64 * 64 || generation != npc.generation()) {
                    player.sendSystemMessage(Component.literal("AI-antwoord vervallen: hulpje is verplaatst of de taak is veranderd.")); return;
                }
                npc.chatMemory.remember(message, reply.message());
                player.sendSystemMessage(Component.literal("Henkhulp: " + reply.message()));
                if (reply.task() != null) npc.assign(reply.task(), player);
            }));
            player.sendSystemMessage(Component.literal("Ik vraag je lokale Ollama-model om een antwoord..."));
        } catch (RuntimeException exception) {
            AI_BUSY.set(false); throw exception;
        }
    }
}
