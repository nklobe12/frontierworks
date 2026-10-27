package com.frontierworks.blasters.client;

import com.frontierworks.blasters.Content;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.IEventBus;

public final class BlastersClient {
    private BlastersClient() {}

    public static void init(IEventBus bus) {
        bus.addListener((EntityRenderersEvent.RegisterRenderers e) -> e.registerEntityRenderer(Content.BOLT.get(), ctx -> new ThrownItemRenderer<>(ctx, 1.0f, true)));
    }
}
