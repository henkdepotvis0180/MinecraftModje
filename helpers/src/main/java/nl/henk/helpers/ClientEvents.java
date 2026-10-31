package nl.henk.helpers;

import net.minecraft.client.renderer.entity.VillagerRenderer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

@EventBusSubscriber(modid = HenkHelpers.ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientEvents {
    private ClientEvents() {}
    @SubscribeEvent
    public static void renderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(HenkHelpers.WORKER.get(), VillagerRenderer::new);
    }
}
