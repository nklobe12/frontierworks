package com.lodestar.petroleum.client;

import com.lodestar.core.client.MachineScreen;
import com.lodestar.core.client.RenderUtil;
import com.lodestar.petroleum.Content;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

/** Client setup for Petroleum: the pumpjack's walking beam renderer and the distillation column screen. */
public final class PetroleumClient {
    private PetroleumClient() {}

    public static void init(IEventBus bus) {
        for (var id : PumpjackRenderer.MODELS) RenderUtil.extraModel(id);
        bus.addListener((EntityRenderersEvent.RegisterRenderers e) -> e.registerBlockEntityRenderer(Content.PUMPJACK_BE.get(), PumpjackRenderer::new));
        bus.addListener((FMLClientSetupEvent e) -> e.enqueueWork(() ->
                MachineScreen.register(Content.DISTILLATION_COLUMN_BE.get(), DistillationScreen::new)));
    }
}
