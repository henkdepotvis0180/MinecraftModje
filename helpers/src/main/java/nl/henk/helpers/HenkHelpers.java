package nl.henk.helpers;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.Commands;
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
        event.put(WORKER.get(), Villager.createAttributes().build());
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
    private void commands(RegisterCommandsEvent event) {
        var root = Commands.literal("helper")
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
                .then(Commands.literal("ask").then(Commands.argument("message", StringArgumentType.greedyString()).executes(context -> {
                    var p = context.getSource().getPlayerOrException();
                    return safely(p, () -> ask(p, StringArgumentType.getString(context, "message")));
                })));
        event.getDispatcher().register(root);
    }
    private static void ask(ServerPlayer player, String message) {
        if (!AI.get()) throw new IllegalArgumentException("Lokale AI staat uit in de serverconfiguratie.");
        var npc = worker(player);
        if (!AI_BUSY.compareAndSet(false, true)) throw new IllegalArgumentException("Het lokale model is nog bezig. Probeer straks opnieuw.");
        var server = player.serverLevel().getServer();
        int generation = npc.generation();
        try {
            Ollama.plan(message).whenComplete((plan, error) -> server.execute(() -> {
                AI_BUSY.set(false);
                if (server.getPlayerList().getPlayer(player.getUUID()) != player) return;
                if (error != null) {
                    player.sendSystemMessage(Component.literal("AI-opdracht mislukt. Controleer Ollama, modelnaam en /helper status. Handmatige opdrachten blijven werken."));
                    return;
                }
                if (!npc.isAlive() || npc.isRemoved() || npc.level() != player.level()
                        || npc.distanceToSqr(player) > 64 * 64 || generation != npc.generation()) {
                    player.sendSystemMessage(Component.literal("AI-antwoord vervallen: hulpje is verplaatst of de taak is veranderd.")); return;
                }
                player.sendSystemMessage(Component.literal("AI kiest: " + plan.action() + (plan.count() > 0 ? " " + plan.count() : "")));
                npc.assign(plan, player);
            }));
            player.sendSystemMessage(Component.literal("Ik vraag je lokale Ollama-model om een taak..."));
        } catch (RuntimeException exception) {
            AI_BUSY.set(false); throw exception;
        }
    }
}
