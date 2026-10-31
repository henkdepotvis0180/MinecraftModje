package nl.henk.helpers;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.BlockEvent;
import java.util.UUID;

public final class Worker extends Villager {
    private UUID owner;
    public final ChatMemory chatMemory = new ChatMemory();
    private TaskPlan task;
    private BlockPos anchor;
    private BlockPos target;
    private int progress;
    private int idleTicks;
    private int generation;

    public Worker(EntityType<? extends Villager> type, Level level) {
        super(type, level);
        setPersistenceRequired();
    }
    public void setOwner(UUID id) { owner = id; }
    public boolean ownedBy(ServerPlayer player) { return player.getUUID().equals(owner); }
    public int generation() { return generation; }
    public void forgetChat() { chatMemory.clear(); generation++; tell("Ons korte gespreksgeheugen is gewist."); }
    public String status() { return task == null ? "Ik wacht op werk." : task.action() + ": " + progress + " gedaan."; }
    private void tell(String text) {
        if (owner != null && level() instanceof ServerLevel server) {
            var player = server.getServer().getPlayerList().getPlayer(owner);
            if (player != null) player.sendSystemMessage(Component.literal("Henkhulp: " + text));
        }
    }
    public void stop(String text) {
        task = null; target = null; generation++; getNavigation().stop(); tell(text);
    }
    public void assign(TaskPlan next, ServerPlayer player) {
        if (!ownedBy(player)) return;
        if (next.action() == TaskPlan.Action.STATUS) { tell(status()); return; }
        if (next.action() == TaskPlan.Action.STOP) { stop("Ik stop."); return; }
        if (task != null) { tell("Ik ben al bezig. Gebruik eerst /helper stop."); return; }
        if (next.action() == TaskPlan.Action.CHOP && !(getMainHandItem().getItem() instanceof AxeItem)) {
            tell("Geef me eerst een bijl met /helper give."); return;
        }
        if (next.action() == TaskPlan.Action.BUILD && countPlanks() < Blueprint.hut().size()) {
            tell("Voor de hut heb ik 96 eikenplanken nodig. Gebruik /helper give."); return;
        }
        task = next; progress = 0; idleTicks = 0; target = null; generation++;
        anchor = next.action() == TaskPlan.Action.BUILD
                ? player.blockPosition().relative(player.getDirection(), 4) : blockPosition();
        tell(next.action() == TaskPlan.Action.BUILD ? "Ik bouw een hut vanaf vier blokken voor je, op voethoogte." : "Ik ga hout hakken binnen 16 blokken.");
    }
    private int countPlanks() {
        int count = 0;
        for (int i = 0; i < getInventory().getContainerSize(); i++) {
            var stack = getInventory().getItem(i);
            if (stack.is(Items.OAK_PLANKS)) count += stack.getCount();
        }
        return count;
    }
    private void consumePlank() {
        for (int i = 0; i < getInventory().getContainerSize(); i++)
            if (getInventory().getItem(i).is(Items.OAK_PLANKS)) { getInventory().removeItem(i, 1); return; }
    }
    public void receive(ServerPlayer player) {
        var held = player.getMainHandItem();
        if (held.isEmpty()) { tell("Houd een bijl of eikenplanken vast."); return; }
        if (held.getItem() instanceof AxeItem) {
            if (!getMainHandItem().isEmpty()) { tell("Ik heb al een bijl. Gebruik eerst /helper collect."); return; }
            setItemSlot(EquipmentSlot.MAINHAND, held.split(1));
        } else if (held.is(Items.OAK_PLANKS)) {
            var rest = getInventory().addItem(held.copy());
            held.setCount(rest.getCount());
        } else { tell("Ik neem een bijl of eikenplanken aan."); return; }
        tell("Bedankt, materiaal ontvangen.");
    }
    public void collect(ServerPlayer player) {
        if (task != null) { tell("Stop mijn taak voordat je spullen ophaalt."); return; }
        for (int i = 0; i < getInventory().getContainerSize(); i++) {
            var stack = getInventory().getItem(i);
            player.getInventory().add(stack);
            getInventory().setItem(i, stack);
        }
        var axe = getMainHandItem();
        player.getInventory().add(axe);
        setItemSlot(EquipmentSlot.MAINHAND, axe);
        tell("Spullen overgedragen voor zover je inventaris ruimte heeft.");
    }
    @Override protected void customServerAiStep() {
        // Replace villager profession/trading brain with our task controller.
        if (task == null || !(level() instanceof ServerLevel server)) return;
        var player = owner == null ? null : server.getServer().getPlayerList().getPlayer(owner);
        if (player == null || player.level() != level() || player.distanceToSqr(this) > 64 * 64) {
            stop("Ik pauzeer: mijn eigenaar is te ver weg."); return;
        }
        if (++idleTicks > 600) { stop("Ik kan mijn doel niet bereiken. Maak ruimte en geef de opdracht opnieuw."); return; }
        if (tickCount % 10 != 0) return;
        if (task.action() == TaskPlan.Action.BUILD) build(server, player); else chop(server, player);
    }
    private boolean approach(BlockPos pos) {
        if (getEyePosition().distanceToSqr(pos.getCenter()) <= 4.5 * 4.5) {
            getNavigation().stop(); getLookControl().setLookAt(pos.getX() + .5, pos.getY() + .5, pos.getZ() + .5); return true;
        }
        getNavigation().moveTo(pos.getX() + .5, getY(), pos.getZ() + .5, .65);
        return false;
    }
    private boolean allowed(ServerLevel server, ServerPlayer player, BlockPos pos) {
        return server.hasChunkAt(pos) && server.getWorldBorder().isWithinBounds(pos)
                && !server.isOutsideBuildHeight(pos) && server.mayInteract(player, pos)
                && player.mayBuild();
    }
    private void chop(ServerLevel server, ServerPlayer player) {
        if (!(getMainHandItem().getItem() instanceof AxeItem)) { stop("Mijn bijl is op."); return; }
        if (progress >= task.count()) { stop("Houtkap klaar. Haal de buit op met /helper collect."); return; }
        if (target == null || !server.getBlockState(target).is(BlockTags.LOGS)) {
            target = null; double best = Double.MAX_VALUE;
            for (var cursor : BlockPos.betweenClosed(anchor.offset(-16, -2, -16), anchor.offset(16, 5, 16))) {
                if (!server.hasChunkAt(cursor) || !server.getBlockState(cursor).is(BlockTags.LOGS)) continue;
                double score = cursor.distSqr(blockPosition());
                if (score < best) { best = score; target = cursor.immutable(); }
            }
            if (target == null) { stop("Geen boomstammen binnen mijn werkgebied gevonden."); return; }
        }
        if (!allowed(server, player, target)) { stop("Ik mag hier geen blokken hakken."); return; }
        if (!approach(target)) return;
        // Do not work through walls; normal block reach applies.
        var hit = server.clip(new net.minecraft.world.level.ClipContext(getEyePosition(), target.getCenter(),
                net.minecraft.world.level.ClipContext.Block.COLLIDER, net.minecraft.world.level.ClipContext.Fluid.NONE, this));
        if (!hit.getBlockPos().equals(target)) return;
        var state = server.getBlockState(target);
        if (NeoForge.EVENT_BUS.post(new BlockEvent.BreakEvent(server, target, state, player)).isCanceled()) {
            stop("Houtkap is hier geblokkeerd."); return;
        }
        var drops = Block.getDrops(state, server, target, server.getBlockEntity(target), this, getMainHandItem());
        if (!server.destroyBlock(target, false, this)) { stop("Dit blok kon niet worden gehakt."); return; }
        for (var drop : drops) {
            var rest = getInventory().addItem(drop);
            if (!rest.isEmpty()) spawnAtLocation(rest);
        }
        getMainHandItem().hurtAndBreak(1, this, EquipmentSlot.MAINHAND);
        progress++; idleTicks = 0; target = null;
    }
    private void build(ServerLevel server, ServerPlayer player) {
        var plan = Blueprint.hut();
        if (progress >= plan.size()) { stop("De hut is klaar!"); return; }
        var cell = plan.get(progress);
        var pos = anchor.offset(cell.x(), cell.y(), cell.z());
        if (!allowed(server, player, pos)) { stop("Ik mag hier niet bouwen."); return; }
        var state = server.getBlockState(pos);
        if (state.is(Blocks.OAK_PLANKS)) { progress++; idleTicks = 0; return; }
        if (!state.canBeReplaced() || !state.getFluidState().isEmpty()) { stop("Bouwplaats bezet op " + pos.toShortString() + ". Kies een vrije plek."); return; }
        if (countPlanks() == 0) { stop("Mijn planken zijn op."); return; }
        if (!approach(pos)) return;
        if (!server.getEntities(this, new AABB(pos)).isEmpty() || getBoundingBox().intersects(new AABB(pos))) return;
        var snapshot = net.neoforged.neoforge.common.util.BlockSnapshot.create(server.dimension(), server, pos);
        if (!server.setBlock(pos, Blocks.OAK_PLANKS.defaultBlockState(), 3)) { stop("Plaatsen is mislukt."); return; }
        if (net.neoforged.neoforge.event.EventHooks.onBlockPlace(this, snapshot, net.minecraft.core.Direction.UP)) {
            snapshot.restore(true, false); stop("Bouwen is hier geblokkeerd."); return;
        }
        consumePlank(); progress++; idleTicks = 0;
    }
    @Override public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (owner != null) tag.putUUID("HenkOwner", owner);
    }
    @Override public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.hasUUID("HenkOwner")) owner = tag.getUUID("HenkOwner");
        // Never resume an old job after a world reload.
        task = null;
    }
}
