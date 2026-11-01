package nl.henk.helpers;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(HenkHelpers.ID)
@PrefixGameTestTemplate(false)
public final class WorkerGameTests {
    private static void floor(GameTestHelper h) {
        for (int x = 0; x < 16; x++) for (int z = 0; z < 16; z++) h.setBlock(x, 0, z, Blocks.STONE);
    }
    private static ServerPlayer owner(GameTestHelper h) {
        var p = h.makeMockServerPlayerInLevel();
        var pos = h.absolutePos(new BlockPos(1, 1, 1));
        p.teleportTo(pos.getX() + .5, pos.getY(), pos.getZ() + .5);
        return p;
    }
    private static Worker worker(GameTestHelper h, ServerPlayer p) {
        var npc = h.spawn(HenkHelpers.WORKER.get(), new BlockPos(3, 1, 3));
        npc.setOwner(p.getUUID());
        return npc;
    }
    private static int count(Worker npc, net.minecraft.world.item.Item item) {
        return npc.getInventory().countItem(item);
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void realInventoryTransfersAndSave(GameTestHelper h) {
        floor(h);
        var p = owner(h);
        var npc = worker(h, p);
        p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_AXE));
        npc.receive(p);
        h.assertTrue(p.getMainHandItem().isEmpty(), "The axe must leave the player hand");
        h.assertTrue(npc.getMainHandItem().is(Items.IRON_AXE), "The actual hand must hold the axe");
        p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.OAK_PLANKS, 64));
        npc.receive(p);
        h.assertTrue(p.getMainHandItem().isEmpty() && count(npc, Items.OAK_PLANKS) == 64, "Real plank transfer");
        var saved = new CompoundTag();
        npc.saveWithoutId(saved);
        var restored = HenkHelpers.WORKER.get().create(h.getLevel());
        restored.load(saved);
        h.assertTrue(restored.ownedBy(p) && restored.getMainHandItem().is(Items.IRON_AXE)
                && count(restored, Items.OAK_PLANKS) == 64, "Owner, equipment and bag must survive save/load");
        npc.collect(p);
        h.assertTrue(npc.getMainHandItem().isEmpty() && npc.getInventory().isEmpty(), "Collection empties real inventory");
        h.assertTrue(p.getInventory().countItem(Items.IRON_AXE) == 1 && p.getInventory().countItem(Items.OAK_PLANKS) == 64,
                "Collection conserves all items");
        h.getLevel().getServer().getPlayerList().remove(p);
        h.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void menuShiftClickAndFullBag(GameTestHelper h) {
        floor(h);
        var p = owner(h);
        var npc = worker(h, p);
        p.getInventory().setItem(0, new ItemStack(Items.STONE_AXE));
        p.getInventory().setItem(1, new ItemStack(Items.OAK_PLANKS, 64));
        var menu = new WorkerSuppliesMenu(1, p.getInventory(), npc);
        menu.quickMoveStack(p, 36);
        menu.quickMoveStack(p, 37);
        h.assertTrue(npc.getMainHandItem().is(Items.STONE_AXE), "Shift-click must equip the axe");
        h.assertTrue(count(npc, Items.OAK_PLANKS) == 64 && p.getInventory().getItem(1).isEmpty(), "Menu transfer must be real");
        h.assertTrue(!menu.getSlot(0).mayPlace(new ItemStack(Items.DIAMOND)), "Tool slot rejects other items");
        for (int i = 0; i < 8; i++) npc.getInventory().setItem(i, new ItemStack(Items.OAK_PLANKS, 64));
        p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.OAK_PLANKS, 32));
        npc.receive(p);
        h.assertTrue(p.getMainHandItem().getCount() == 32 && count(npc, Items.OAK_PLANKS) == 512,
                "A full bag must neither destroy nor duplicate planks");
        h.getLevel().getServer().getPlayerList().remove(p);
        h.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 350)
    public static void waitsThenFetchesAxeFromChestAndChops(GameTestHelper h) {
        floor(h);
        var p = owner(h);
        var npc = worker(h, p);
        h.setBlock(5, 1, 3, Blocks.CHEST);
        h.setBlock(8, 1, 3, Blocks.OAK_LOG);
        var chest = (ChestBlockEntity) h.getBlockEntity(new BlockPos(5, 1, 3));
        npc.bindSupplies(p);
        npc.assign(new TaskPlan(TaskPlan.Action.CHOP, 1), p);
        h.runAfterDelay(30, () -> {
            h.assertTrue(npc.getMainHandItem().isEmpty(), "No invented axe while waiting");
            h.assertTrue(h.getBlockState(new BlockPos(8, 1, 3)).is(Blocks.OAK_LOG), "No work without a real tool");
            chest.setItem(0, new ItemStack(Items.IRON_AXE));
        });
        h.succeedWhen(() -> {
            h.assertTrue(h.getBlockState(new BlockPos(8, 1, 3)).isAir(), "Worker must resume and chop the actual block");
            h.assertTrue(npc.getMainHandItem().is(Items.IRON_AXE) && chest.getItem(0).isEmpty(), "Axe is taken from the chest");
            h.assertTrue(npc.getMainHandItem().getDamageValue() == 1, "Axe must wear out");
            h.assertTrue(count(npc, Items.OAK_LOG) == 1, "Log must be in the real bag");
            h.getLevel().getServer().getPlayerList().remove(p);
        });
    }

    @GameTest(template = "empty", timeoutTicks = 350)
    public static void walksToDroppedToolAndWorks(GameTestHelper h) {
        floor(h);
        var p = owner(h);
        var npc = worker(h, p);
        var pos = h.absolutePos(new BlockPos(10, 1, 3));
        var item = new ItemEntity(h.getLevel(), pos.getX() + .5, pos.getY() + .2, pos.getZ() + .5,
                new ItemStack(Items.STONE_AXE));
        item.setNoPickUpDelay();
        h.getLevel().addFreshEntity(item);
        h.setBlock(12, 1, 3, Blocks.OAK_LOG);
        npc.assign(new TaskPlan(TaskPlan.Action.CHOP, 1), p);
        h.succeedWhen(() -> {
            h.assertTrue(count(npc, Items.OAK_LOG) == 1, "Must walk, pick up tool and chop");
            h.assertTrue(!item.isAlive() && npc.getMainHandItem().is(Items.STONE_AXE), "Dropped tool becomes equipped");
            h.getLevel().getServer().getPlayerList().remove(p);
        });
    }

    @GameTest(template = "empty", timeoutTicks = 2600)
    public static void fetchesPlanksAndCompletesRealHut(GameTestHelper h) {
        floor(h);
        var p = owner(h);
        p.setYRot(0);
        var npc = worker(h, p);
        h.setBlock(5, 1, 3, Blocks.CHEST);
        var chest = (ChestBlockEntity) h.getBlockEntity(new BlockPos(5, 1, 3));
        chest.setItem(0, new ItemStack(Items.OAK_PLANKS, 64));
        chest.setItem(1, new ItemStack(Items.OAK_PLANKS, 32));
        npc.bindSupplies(p);
        var anchor = p.blockPosition().relative(p.getDirection(), 4);
        npc.assign(new TaskPlan(TaskPlan.Action.BUILD, 0), p);
        h.succeedWhen(() -> {
            int placed = 0;
            for (var cell : Blueprint.hut()) if (h.getLevel().getBlockState(anchor.offset(cell.x(), cell.y(), cell.z())).is(Blocks.OAK_PLANKS)) placed++;
            h.assertTrue(placed == 96, "Actual hut blocks: " + placed + "/96; " + npc.status() + " at " + npc.position());
            h.assertTrue(chest.isEmpty() && count(npc, Items.OAK_PLANKS) == 0, "Exactly 96 real planks consumed");
            h.assertTrue(npc.displayStatus().contains("klaar"), "Task must finish without waiting for more planks");
            h.getLevel().getServer().getPlayerList().remove(p);
        });
    }

    @GameTest(template = "empty", timeoutTicks = 120)
    public static void stopCancelsWaitingAndOwnerIsRequired(GameTestHelper h) {
        floor(h);
        var p = owner(h);
        var npc = worker(h, p);
        var stranger = h.makeMockServerPlayerInLevel();
        stranger.teleportTo(p.getX(), p.getY(), p.getZ());
        var inventory = new WorkerInventory(npc);
        h.assertTrue(!inventory.stillValid(stranger) && inventory.stillValid(p), "Only the owner can use the live inventory");
        npc.assign(new TaskPlan(TaskPlan.Action.CHOP, 1), p);
        npc.stop("Test stop");
        h.setBlock(5, 1, 3, Blocks.OAK_LOG);
        var item = new ItemEntity(h.getLevel(), npc.getX(), npc.getY(), npc.getZ(), new ItemStack(Items.IRON_AXE));
        item.setNoPickUpDelay();
        h.getLevel().addFreshEntity(item);
        h.runAfterDelay(60, () -> {
            h.assertTrue(npc.getMainHandItem().isEmpty() && item.isAlive(), "Stopped worker must not collect tools");
            h.assertTrue(h.getBlockState(new BlockPos(5, 1, 3)).is(Blocks.OAK_LOG), "Stopped worker must not resume work");
            h.getLevel().getServer().getPlayerList().remove(p);
            h.getLevel().getServer().getPlayerList().remove(stranger);
            h.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 600)
    public static void followsOnlyOnRequestAndKeepsGap(GameTestHelper h) {
        floor(h);
        var p = owner(h);
        var npc = worker(h, p);
        var start = npc.position();
        h.runAfterDelay(20, () -> {
            h.assertTrue(!npc.isFollowing() && npc.position().distanceToSqr(start) < .01, "Following must be opt-in");
            h.getLevel().getServer().getCommands().performPrefixedCommand(p.createCommandSourceStack(), "helper talk " + npc.getUUID() + " volg mij");
            h.assertTrue(npc.isFollowing(), "Natural follow request must work without Ollama");
            var far = h.absolutePos(new BlockPos(12, 1, 3));
            p.teleportTo(far.getX() + .5, far.getY(), far.getZ() + .5);
        });
        h.runAfterDelay(180, () -> {
            h.assertTrue(npc.position().distanceToSqr(start) > 16 && npc.distanceToSqr(p) <= 4.1, "Must actually walk and stop within two blocks");
            var settled = npc.position();
            p.teleportTo(settled.x - 3, settled.y, settled.z);
            h.runAfterDelay(30, () -> {
                h.assertTrue(npc.position().distanceToSqr(settled) < .05, "At three blocks, the 4/2 gap must prevent restarting");
                npc.setFollowing(p, false);
                p.teleportTo(settled.x - 8, settled.y, settled.z);
                h.runAfterDelay(40, () -> {
                    h.assertTrue(!npc.isFollowing() && npc.position().distanceToSqr(settled) < .05, "Stay must stop following");
                    h.getLevel().getServer().getPlayerList().remove(p);
                    h.succeed();
                });
            });
        });
    }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void followRangeOwnershipAndSave(GameTestHelper h) {
        floor(h);
        var p = owner(h);
        var npc = worker(h, p);
        var stranger = h.makeMockServerPlayerInLevel();
        stranger.teleportTo(p.getX(), p.getY(), p.getZ());
        npc.setFollowing(stranger, true);
        npc.cycleFollowGap(stranger);
        h.assertTrue(!npc.isFollowing() && npc.followStartDistance() == 4, "Strangers cannot alter follow mode or gap");
        npc.setFollowing(p, true);
        npc.cycleFollowGap(p);
        h.assertTrue(npc.followStartDistance() == 6 && npc.followStopDistance() == 3, "Second distance preset");
        npc.cycleFollowGap(p);
        var tag = new CompoundTag();
        npc.saveWithoutId(tag);
        var restored = HenkHelpers.WORKER.get().create(h.getLevel());
        restored.load(tag);
        h.assertTrue(restored.isFollowing() && restored.followStartDistance() == 10 && restored.followStopDistance() == 4,
                "Explicit follow choice and gap must survive save/load");
        var start = npc.position();
        p.teleportTo(start.x + 80, start.y, start.z);
        h.runAfterDelay(20, () -> {
            h.assertTrue(npc.position().distanceToSqr(start) < .01 && npc.displayStatus().contains("64"), "Beyond 64: pause, never teleport");
            h.getLevel().getServer().getPlayerList().remove(p);
            h.runAfterDelay(20, () -> {
                h.assertTrue(npc.position().distanceToSqr(start) < .01 && npc.displayStatus().contains("eigenaar"), "Offline owner pauses following");
                h.getLevel().getServer().getPlayerList().remove(stranger);
                h.succeed();
            });
        });
    }

    @GameTest(template = "empty", timeoutTicks = 1000)
    public static void blockedFollowerWaitsAndResumesWhenOpened(GameTestHelper h) {
        floor(h);
        var p = owner(h);
        var npc = worker(h, p);
        for (int x = 2; x <= 4; x++) for (int z = 2; z <= 4; z++)
            if (x != 3 || z != 3) for (int y = 1; y <= 3; y++) h.setBlock(x, y, z, Blocks.STONE);
        npc.setFollowing(p, true);
        var far = h.absolutePos(new BlockPos(11, 1, 3));
        p.teleportTo(far.getX() + .5, far.getY(), far.getZ() + .5);
        var start = npc.position();
        h.runAfterDelay(650, () -> {
            h.assertTrue(npc.position().distanceToSqr(start) < .1 && npc.displayStatus().contains("geblokkeerd"), "Blocked follower must wait and report, not teleport");
            for (int y = 1; y <= 3; y++) h.setBlock(4, y, 3, Blocks.AIR);
            h.succeedWhen(() -> {
                h.assertTrue(npc.distanceToSqr(p) <= 4.1, "Follower must resume after a route opens");
                h.getLevel().getServer().getPlayerList().remove(p);
            });
        });
    }
}
