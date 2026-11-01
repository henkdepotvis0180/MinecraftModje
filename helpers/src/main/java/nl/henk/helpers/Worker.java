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
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.Container;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;

public final class Worker extends Villager {
    private static final EntityDataAccessor<String> WORK_STATE = SynchedEntityData.defineId(Worker.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Integer> PLANKS = SynchedEntityData.defineId(Worker.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<String> SUPPLY = SynchedEntityData.defineId(Worker.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Boolean> FOLLOWING = SynchedEntityData.defineId(Worker.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> FOLLOW_GAP = SynchedEntityData.defineId(Worker.class, EntityDataSerializers.INT);
    public static final int MAX_FOLLOW_DISTANCE = 64;
    private boolean walkingToOwner;
    private int followStuckTicks;
    private net.minecraft.world.phys.Vec3 lastFollowPosition;
    private UUID owner;
    public final ChatMemory chatMemory = new ChatMemory();
    private TaskPlan task;
    private BlockPos anchor;
    private BlockPos target;
    private int progress;
    private int idleTicks;
    private int generation;
    private BlockPos supplyPos;
    private String workState = "Wacht op een opdracht";

    @Override protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(WORK_STATE, "Wacht op een opdracht");
        builder.define(PLANKS, 0);
        builder.define(SUPPLY, "Geen voorraadkist gekoppeld");
        builder.define(FOLLOWING, false);
        builder.define(FOLLOW_GAP, 0);
    }
    public boolean isFollowing() { return entityData.get(FOLLOWING); }
    public int followStartDistance() { return switch (entityData.get(FOLLOW_GAP)) { case 1 -> 6; case 2 -> 10; default -> 4; }; }
    public int followStopDistance() { return switch (entityData.get(FOLLOW_GAP)) { case 1 -> 3; case 2 -> 4; default -> 2; }; }
    public void setFollowing(ServerPlayer player, boolean enabled) {
        if (!ownedBy(player) || player.level() != level() || distanceToSqr(player) > MAX_FOLLOW_DISTANCE * MAX_FOLLOW_DISTANCE) return;
        task = null;
        target = null;
        generation++;
        entityData.set(FOLLOWING, enabled);
        walkingToOwner = false;
        followStuckTicks = 0;
        lastFollowPosition = position();
        getNavigation().stop();
        workState = enabled ? "Volgen aan: start >" + followStartDistance() + ", stop <=" + followStopDistance() + " blokken" : "Ik blijf hier";
        syncWork();
        tell(enabled ? "Ik stop mijn werk en volg je. Start boven " + followStartDistance() + ", stop binnen "
                + followStopDistance() + " blokken; maximaal 64 blokken. Zonder teleporteren." : "Ik blijf hier staan.");
    }
    public void cycleFollowGap(ServerPlayer player) {
        if (!ownedBy(player)) return;
        entityData.set(FOLLOW_GAP, (entityData.get(FOLLOW_GAP) + 1) % 3);
        walkingToOwner = false;
        followStuckTicks = 0;
        getNavigation().stop();
        tell("Volgafstand: lopen boven " + followStartDistance() + ", stoppen binnen " + followStopDistance() + " blokken; maximaal 64.");
    }
    public String displayStatus() { return entityData.get(WORK_STATE); }
    public int displayPlanks() { return entityData.get(PLANKS); }
    public String displaySupply() { return entityData.get(SUPPLY); }
    private void syncWork() {
        entityData.set(WORK_STATE, workState);
        entityData.set(PLANKS, countPlanks());
        entityData.set(SUPPLY, supplyPos == null ? "Geen voorraadkist gekoppeld" : "Kist: " + supplyPos.toShortString());
    }
    public void openSupplies(ServerPlayer player) {
        if (!ownedBy(player)) return;
        getNavigation().stop();
        player.openMenu(new SimpleMenuProvider((id, inventory, p) -> new WorkerSuppliesMenu(id, inventory, this),
                Component.literal("Henkhulp: bijl links, rugzak rechts")));
    }
    public void bindSupplies(ServerPlayer player) {
        BlockPos closest = null;
        double distance = Double.MAX_VALUE;
        for (var pos : BlockPos.betweenClosed(player.blockPosition().offset(-8, -3, -8), player.blockPosition().offset(8, 3, 8))) {
            if (!level().hasChunkAt(pos)) continue;
            var blockEntity = level().getBlockEntity(pos);
            if (!(blockEntity instanceof ChestBlockEntity || blockEntity instanceof BarrelBlockEntity)) continue;
            if (pos.distSqr(player.blockPosition()) < distance) {
                closest = pos.immutable(); distance = pos.distSqr(player.blockPosition());
            }
        }
        if (closest == null) { tell("Zet een kist of vat met benodigdheden binnen 8 blokken van je neer."); return; }
        supplyPos = closest;
        tell("Voorraad gekoppeld op " + closest.toShortString() + ". Hier haal ik alleen mijn ontbrekende bijl of eikenplanken op.");
        syncWork();
    }

    public Worker(EntityType<? extends Villager> type, Level level) {
        super(type, level);
        setPersistenceRequired();
    }
    // Keep helpers quiet, including existing entities loaded from a save.
    @Override public boolean isSilent() { return true; }
    @Override public net.minecraft.world.InteractionResult mobInteract(
            net.minecraft.world.entity.player.Player player, net.minecraft.world.InteractionHand hand) {
        // The client opens the conversation; never start inherited villager trading.
        if (player instanceof ServerPlayer serverPlayer && !ownedBy(serverPlayer))
            serverPlayer.sendSystemMessage(Component.literal("Dit is het hulpje van een andere speler."));
        return net.minecraft.world.InteractionResult.sidedSuccess(level().isClientSide);
    }
    public void setOwner(UUID id) { owner = id; }
    public boolean ownedBy(ServerPlayer player) { return player.getUUID().equals(owner); }
    public int generation() { return generation; }
    public void forgetChat() { chatMemory.clear(); generation++; tell("Ons korte gespreksgeheugen is gewist."); }
    public String status() {
        return workState + (task == null ? "" : " | " + progress + "/" + (task.action() == TaskPlan.Action.CHOP ? task.count() : Blueprint.hut().size()))
                + " | Bijl: " + (getMainHandItem().getItem() instanceof AxeItem ? getMainHandItem().getHoverName().getString() : "geen")
                + " | Eikenplanken: " + countPlanks();
    }
    private void tell(String text) {
        if (owner != null && level() instanceof ServerLevel server) {
            var player = server.getServer().getPlayerList().getPlayer(owner);
            if (player != null) player.sendSystemMessage(Component.literal("Henkhulp: " + text));
        }
    }
    public void stop(String text) {
        entityData.set(FOLLOWING, false);
        walkingToOwner = false;
        task = null; target = null; generation++; getNavigation().stop(); workState = text; syncWork(); tell(text);
    }
    public void assign(TaskPlan next, ServerPlayer player) {
        if (!ownedBy(player)) return;
        if (next.action() == TaskPlan.Action.STATUS) { tell(status()); return; }
        if (next.action() == TaskPlan.Action.STOP) { stop("Ik stop."); return; }
        if (task != null) { tell("Ik ben al bezig. Gebruik eerst /helper stop."); return; }
        entityData.set(FOLLOWING, false);
        walkingToOwner = false;
        getNavigation().stop();
        task = next; progress = 0; idleTicks = 0; target = null; generation++;
        anchor = next.action() == TaskPlan.Action.BUILD
                ? player.blockPosition().relative(player.getDirection(), 4) : blockPosition();
        workState = "Opdracht ontvangen";
        syncWork();
        tell(next.action() == TaskPlan.Action.BUILD ? "Ik bouw een hut vanaf vier blokken voor je. Ontbrekende planken haal ik uit mijn voorraadkist of van de grond." : "Ik ga hout hakken. Zo nodig zoek ik eerst mijn bijl. De opdracht blijft bewaard terwijl ik op materiaal wacht.");
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
        if (held.isEmpty() || (!(held.getItem() instanceof AxeItem) && !held.is(Items.OAK_PLANKS))) {
            openSupplies(player);
            return;
        }
        int received;
        if (held.getItem() instanceof AxeItem) {
            if (!getMainHandItem().isEmpty()) { tell("Ik heb al een bijl. Gebruik eerst /helper collect."); return; }
            setItemSlot(EquipmentSlot.MAINHAND, held.split(1));
            setGuaranteedDrop(EquipmentSlot.MAINHAND);
            received = 1;
        } else if (held.is(Items.OAK_PLANKS)) {
            int before = held.getCount();
            var rest = getInventory().addItem(held.copy());
            held.setCount(rest.getCount());
            received = before - rest.getCount();
        } else { tell("Ik neem een bijl of eikenplanken aan."); return; }
        syncWork();
        tell(received == 0 ? "Mijn rugzak is vol; er is niets overgedragen." : received + " voorwerp(en) echt ontvangen. " + status());
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
        if (!level().isClientSide && tickCount % 10 == 0) syncWork();
        if (isFollowing() && level() instanceof ServerLevel followLevel) {
            followOwner(followLevel);
            return;
        }
        if (task == null || !(level() instanceof ServerLevel server)) return;
        var player = owner == null ? null : server.getServer().getPlayerList().getPlayer(owner);
        if (player == null || player.level() != level() || player.distanceToSqr(this) > 64 * 64) {
            stop("Ik pauzeer: mijn eigenaar is te ver weg."); return;
        }
        if (tickCount % 10 != 0) return;
        if (player.containerMenu instanceof WorkerSuppliesMenu menu && menu.getContainer() instanceof WorkerInventory inventory
                && inventory.isFor(this) && inventory.stillValid(player)) { getNavigation().stop(); return; }
        if (needsSupplies()) { gatherSupplies(server, player); return; }
        if (++idleTicks > 60) { stop("Ik kan mijn doel niet bereiken. Maak ruimte en geef de opdracht opnieuw."); return; }
        workState = task.action() == TaskPlan.Action.BUILD ? "Hut bouwen: " + progress + "/96" : "Hout hakken: " + progress + "/" + task.count();
        if (task.action() == TaskPlan.Action.BUILD) build(server, player); else chop(server, player);
    }
    private void followOwner(ServerLevel server) {
        var player = owner == null ? null : server.getServer().getPlayerList().getPlayer(owner);
        if (player == null || !player.isAlive() || player.isSpectator() || player.level() != level()) {
            pauseFollowing("Wacht op eigenaar in deze wereld");
            return;
        }
        if (distanceToSqr(player) > MAX_FOLLOW_DISTANCE * MAX_FOLLOW_DISTANCE) {
            pauseFollowing("Te ver: kom binnen 64 blokken");
            return;
        }
        if (player.containerMenu instanceof WorkerSuppliesMenu menu && menu.getContainer() instanceof WorkerInventory inventory
                && inventory.isFor(this)) {
            pauseFollowing("Wacht terwijl je mijn rugzak bekijkt");
            return;
        }
        double distance = distanceToSqr(player);
        if (distance <= followStopDistance() * followStopDistance()) {
            pauseFollowing("Bij je: volgmodus blijft aan");
            return;
        }
        if (!walkingToOwner && distance <= followStartDistance() * followStartDistance()) {
            getNavigation().stop();
            workState = "Wacht tot je >" + followStartDistance() + " blokken weg bent";
            return;
        }
        walkingToOwner = true;
        if (tickCount % 10 != 0) return;
        if (lastFollowPosition == null || position().distanceToSqr(lastFollowPosition) > .0625) {
            followStuckTicks = 0;
            lastFollowPosition = position();
        } else followStuckTicks += 10;
        getLookControl().setLookAt(player, 10, getMaxHeadXRot());
        getNavigation().moveTo(player, 1.1);
        workState = followStuckTicks >= 600 ? "Route geblokkeerd: maak een doorgang" : "Ik volg je | afstand " + (int)Math.ceil(Math.sqrt(distance)) + " blokken";
    }
    private void pauseFollowing(String reason) {
        getNavigation().stop();
        walkingToOwner = false;
        followStuckTicks = 0;
        lastFollowPosition = position();
        workState = reason;
    }
    private boolean needsSupplies() {
        if (progress >= (task.action() == TaskPlan.Action.CHOP ? task.count() : Blueprint.hut().size())) return false;
        if (task.action() == TaskPlan.Action.CHOP) {
            if (getMainHandItem().getItem() instanceof AxeItem) return false;
            for (int i = 0; i < getInventory().getContainerSize(); i++) {
                if (getInventory().getItem(i).getItem() instanceof AxeItem) {
                    setItemSlot(EquipmentSlot.MAINHAND, getInventory().removeItem(i, 1));
                    setGuaranteedDrop(EquipmentSlot.MAINHAND);
                    return false;
                }
            }
            return true;
        }
        return task.action() == TaskPlan.Action.BUILD && countPlanks() == 0;
    }
    private boolean wanted(ItemStack stack) {
        return task.action() == TaskPlan.Action.CHOP ? stack.getItem() instanceof AxeItem : stack.is(Items.OAK_PLANKS);
    }
    private ItemStack storeSupply(ItemStack stack) {
        if (stack.getItem() instanceof AxeItem && getMainHandItem().isEmpty()) {
            setItemSlot(EquipmentSlot.MAINHAND, stack.split(1));
            setGuaranteedDrop(EquipmentSlot.MAINHAND);
            return stack;
        }
        return getInventory().addItem(stack);
    }
    private boolean fetchApproach(BlockPos pos) {
        if (distanceToSqr(pos.getX() + .5, pos.getY(), pos.getZ() + .5) <= 2.5 * 2.5) {
            getNavigation().stop();
            var hit = level().clip(new net.minecraft.world.level.ClipContext(getEyePosition(), pos.getCenter(),
                    net.minecraft.world.level.ClipContext.Block.COLLIDER, net.minecraft.world.level.ClipContext.Fluid.NONE, this));
            return hit.getType() == net.minecraft.world.phys.HitResult.Type.MISS || hit.getBlockPos().equals(pos);
        }
        getNavigation().moveTo(pos.getX() + .5, pos.getY(), pos.getZ() + .5, .75);
        return false;
    }
    private void gatherSupplies(ServerLevel server, ServerPlayer player) {
        String needed = task.action() == TaskPlan.Action.CHOP ? "bijl" : "eikenplanken";
        var drops = server.getEntitiesOfClass(ItemEntity.class, getBoundingBox().inflate(16, 4, 16), item ->
                item.isAlive() && !item.hasPickUpDelay() && wanted(item.getItem())
                        && (item.getTarget() == null || item.getTarget().equals(owner)));
        drops.sort(java.util.Comparator.comparingDouble(this::distanceToSqr));
        if (!drops.isEmpty()) {
            var item = drops.getFirst();
            workState = "Ophalen van grond: " + needed;
            if (fetchApproach(item.blockPosition())) {
                var original = item.getItem();
                if (!(original.getItem() instanceof AxeItem) && !getInventory().canAddItem(original)) {
                    workState = "Rugzak vol: haal eerst spullen op";
                    return;
                }
                int before = original.getCount();
                var rest = storeSupply(original.copy());
                int taken = before - rest.getCount();
                if (taken > 0) {
                    take(item, taken);
                    item.setItem(rest);
                    if (rest.isEmpty()) item.discard();
                    swing(net.minecraft.world.InteractionHand.MAIN_HAND);
                    idleTicks = 0;
                }
            } else if (++idleTicks > 60) stop("Ik kan het materiaal op de grond niet bereiken.");
            return;
        }
        if (supplyPos != null && supplyPos.distSqr(blockPosition()) <= 32 * 32 && allowed(server, player, supplyPos)) {
            var entity = server.getBlockEntity(supplyPos);
            if ((entity instanceof ChestBlockEntity || entity instanceof BarrelBlockEntity)
                    && entity instanceof net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity container
                    && container.canOpen(player)) {
                for (int i = 0; i < container.getContainerSize(); i++) {
                    if (!wanted(container.getItem(i))) continue;
                    if (!(container.getItem(i).getItem() instanceof AxeItem) && !getInventory().canAddItem(container.getItem(i))) {
                        workState = "Rugzak vol: haal eerst spullen op";
                        getNavigation().stop();
                        return;
                    }
                    workState = "Naar voorraadkist voor " + needed;
                    if (!fetchApproach(supplyPos)) {
                        if (++idleTicks > 60) stop("Ik kan de voorraadkist niet bereiken.");
                        return;
                    }
                    int amount = task.action() == TaskPlan.Action.CHOP ? 1 : Math.min(64, Blueprint.hut().size() - progress - countPlanks());
                    var removed = container.removeItem(i, Math.max(1, amount));
                    var rest = storeSupply(removed);
                    if (!rest.isEmpty()) {
                        var slot = container.getItem(i);
                        if (slot.isEmpty()) container.setItem(i, rest);
                        else { slot.grow(rest.getCount()); container.setChanged(); }
                    }
                    container.setChanged();
                    swing(net.minecraft.world.InteractionHand.MAIN_HAND);
                    idleTicks = 0;
                    return;
                }
            }
        }
        getNavigation().stop();
        idleTicks = 0;
        workState = "Wacht op " + needed + " | open rugzak of vul kist";
    }
    private boolean approach(BlockPos pos) {
        if (getEyePosition().distanceToSqr(pos.getCenter()) <= 4.5 * 4.5) {
            getNavigation().stop(); getLookControl().setLookAt(pos.getX() + .5, pos.getY() + .5, pos.getZ() + .5); return true;
        }
        getNavigation().moveTo(pos.getX() + .5, getY(), pos.getZ() + .5, .65);
        return false;
    }
    private boolean approachBuild(BlockPos pos) {
        boolean insideFootprint = getX() > anchor.getX() - .4 && getX() < anchor.getX() + 5.4
                && getZ() > anchor.getZ() - .4 && getZ() < anchor.getZ() + 5.4;
        if (!insideFootprint && getEyePosition().distanceToSqr(pos.getCenter()) <= 4.5 * 4.5) {
            getNavigation().stop();
            getLookControl().setLookAt(pos.getX() + .5, pos.getY() + .5, pos.getZ() + .5);
            return true;
        }
        net.minecraft.world.level.pathfinder.Path bestPath = null;
        BlockPos bestStand = null;
        double best = Double.MAX_VALUE;
        for (int x = -1; x <= 5; x++) for (int z = -1; z <= 5; z++) {
            if (x != -1 && x != 5 && z != -1 && z != 5) continue;
            var stand = anchor.offset(x, 0, z);
            var eye = new net.minecraft.world.phys.Vec3(stand.getX() + .5, stand.getY() + getEyeHeight(), stand.getZ() + .5);
            if (eye.distanceToSqr(pos.getCenter()) > 4.5 * 4.5 || !level().getBlockState(stand).getCollisionShape(level(), stand).isEmpty()
                    || !level().getBlockState(stand.above()).getCollisionShape(level(), stand.above()).isEmpty()) continue;
            var path = getNavigation().createPath(stand, 0);
            double score = stand.distSqr(blockPosition());
            if (path != null && path.canReach() && score < best) { bestPath = path; bestStand = stand; best = score; }
        }
        if (bestPath != null) {
            if (distanceToSqr(bestStand.getX() + .5, bestStand.getY(), bestStand.getZ() + .5) < 1) {
                // Path navigation accepts a broad arrival radius; finish the last step
                // precisely so the worker's body stays outside the block being placed.
                getNavigation().stop();
                getMoveControl().setWantedPosition(bestStand.getX() + .5, bestStand.getY(), bestStand.getZ() + .5, .75);
            } else getNavigation().moveTo(bestPath, .75);
        }
        return false;
    }
    private boolean allowed(ServerLevel server, ServerPlayer player, BlockPos pos) {
        return server.hasChunkAt(pos) && server.getWorldBorder().isWithinBounds(pos)
                && !server.isOutsideBuildHeight(pos) && server.mayInteract(player, pos)
                && player.mayBuild();
    }
    private void chop(ServerLevel server, ServerPlayer player) {
        if (progress >= task.count()) { stop("Houtkap klaar. Haal de buit op met /helper collect."); return; }
        if (!(getMainHandItem().getItem() instanceof AxeItem)) { workState = "Wacht op een nieuwe bijl"; return; }
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
        swing(net.minecraft.world.InteractionHand.MAIN_HAND);
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
        if (!approachBuild(pos)) return;
        if (!server.getEntities(this, new AABB(pos)).isEmpty() || getBoundingBox().intersects(new AABB(pos))) return;
        var snapshot = net.neoforged.neoforge.common.util.BlockSnapshot.create(server.dimension(), server, pos);
        if (!server.setBlock(pos, Blocks.OAK_PLANKS.defaultBlockState(), 3)) { stop("Plaatsen is mislukt."); return; }
        if (net.neoforged.neoforge.event.EventHooks.onBlockPlace(this, snapshot, net.minecraft.core.Direction.UP)) {
            snapshot.restore(); stop("Bouwen is hier geblokkeerd."); return;
        }
        consumePlank(); progress++; idleTicks = 0;
        swing(net.minecraft.world.InteractionHand.MAIN_HAND);
        server.playSound(null, pos, net.minecraft.sounds.SoundEvents.WOOD_PLACE, net.minecraft.sounds.SoundSource.BLOCKS, .6F, 1F);
    }
    @Override public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (owner != null) tag.putUUID("HenkOwner", owner);
        if (supplyPos != null) tag.putLong("HenkSupply", supplyPos.asLong());
        tag.putBoolean("HenkFollowing", isFollowing());
        tag.putInt("HenkFollowGap", entityData.get(FOLLOW_GAP));
    }
    @Override public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.hasUUID("HenkOwner")) owner = tag.getUUID("HenkOwner");
        if (tag.contains("HenkSupply")) supplyPos = BlockPos.of(tag.getLong("HenkSupply"));
        entityData.set(FOLLOWING, tag.getBoolean("HenkFollowing"));
        entityData.set(FOLLOW_GAP, Math.clamp(tag.getInt("HenkFollowGap"), 0, 2));
        walkingToOwner = false;
        followStuckTicks = 0;
        // Never resume an old job after a world reload.
        task = null;
    }
}
