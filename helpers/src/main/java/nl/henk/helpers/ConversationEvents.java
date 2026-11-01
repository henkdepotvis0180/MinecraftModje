package nl.henk.helpers;

import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

@EventBusSubscriber(modid = HenkHelpers.ID, value = Dist.CLIENT)
public final class ConversationEvents {
    private ConversationEvents() {}

    @SubscribeEvent
    public static void interact(PlayerInteractEvent.EntityInteractSpecific event) {
        if (!event.getLevel().isClientSide || event.getHand() != InteractionHand.MAIN_HAND
                || !(event.getTarget() instanceof Worker worker)) return;
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
        Minecraft.getInstance().setScreen(new WorkerChatScreen(worker.getUUID(), worker.getDisplayName()));
    }
}
