package nl.henk.helpers;

import net.minecraft.world.Container;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** A live view: slot 0 is the actual equipped tool; slots 1-8 are the saved bag. */
final class WorkerInventory implements Container {
    private final Worker worker;
    WorkerInventory(Worker worker) { this.worker = worker; }
    boolean isFor(Worker candidate) { return worker == candidate; }
    @Override public int getContainerSize() { return 9; }
    @Override public boolean isEmpty() {
        for (int i = 0; i < 9; i++) if (!getItem(i).isEmpty()) return false;
        return true;
    }
    @Override public ItemStack getItem(int slot) {
        return slot == 0 ? worker.getMainHandItem() : worker.getInventory().getItem(slot - 1);
    }
    @Override public ItemStack removeItem(int slot, int amount) {
        ItemStack result = getItem(slot).split(amount);
        if (getItem(slot).isEmpty()) setItem(slot, ItemStack.EMPTY);
        setChanged();
        return result;
    }
    @Override public ItemStack removeItemNoUpdate(int slot) {
        var result = getItem(slot);
        setItem(slot, ItemStack.EMPTY);
        return result;
    }
    @Override public void setItem(int slot, ItemStack stack) {
        if (slot == 0) worker.setItemSlot(EquipmentSlot.MAINHAND, stack);
        else worker.getInventory().setItem(slot - 1, stack);
        setChanged();
    }
    @Override public void setChanged() { worker.getInventory().setChanged(); }
    @Override public boolean stillValid(Player player) {
        return worker.isAlive() && worker.level() == player.level() && worker.distanceToSqr(player) <= 64
                && player instanceof net.minecraft.server.level.ServerPlayer sp && worker.ownedBy(sp);
    }
    @Override public void clearContent() {
        for (int i = 0; i < 9; i++) setItem(i, ItemStack.EMPTY);
    }
}
