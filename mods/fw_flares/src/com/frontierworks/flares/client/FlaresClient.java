package com.frontierworks.flares.client;

import com.frontierworks.flares.Content;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.IEventBus;

public final class FlaresClient {
    private FlaresClient() {}

    public static void init(IEventBus bus) {
        bus.addListener((EntityRenderersEvent.RegisterRenderers e) -> e.registerEntityRenderer(Content.FLARE_ENTITY.get(), ThrownItemRenderer::new));
    }
}
