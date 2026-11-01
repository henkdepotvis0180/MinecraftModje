package nl.henk.helpers;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.ItemStack;

/** Uses the vanilla nine-slot inventory protocol, including cursor/shift-click sync. */
final class WorkerSuppliesMenu extends ChestMenu {
    WorkerSuppliesMenu(int id, Inventory playerInventory, Worker worker) {
        super(MenuType.GENERIC_9x1, id, playerInventory, new WorkerInventory(worker), 1);
        var toolSlot = new Slot(getContainer(), 0, 8, 18) {
            @Override public boolean mayPlace(ItemStack stack) { return stack.getItem() instanceof AxeItem; }
            @Override public int getMaxStackSize() { return 1; }
        };
        toolSlot.index = 0;
        slots.set(0, toolSlot);
    }
}
