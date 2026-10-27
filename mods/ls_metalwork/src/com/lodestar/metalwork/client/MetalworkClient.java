package com.lodestar.metalwork.client;

import com.lodestar.metalwork.Content;
import com.lodestar.metalwork.Heat;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.IEventBus;

public final class MetalworkClient {
    private MetalworkClient() {}

    public static void init(IEventBus bus) {
        bus.addListener((EntityRenderersEvent.RegisterRenderers e) -> e.registerBlockEntityRenderer(Content.SMITHING_ANVIL_BE.get(), AnvilRenderer::new));
        MinecraftForge.EVENT_BUS.addListener((ItemTooltipEvent e) -> {
            int h = Heat.get(e.getItemStack());
            if (h > 0) e.getToolTip().add(1, Heat.describe(h));
        });
    }
}
