package com.frontierworks.grapple.client;

import com.frontierworks.grapple.Content;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.IEventBus;

public final class GrappleClient {
    private GrappleClient() {}

    public static void init(IEventBus bus) {
        bus.addListener(GrappleClient::renderers);
    }

    private static void renderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(Content.GRAPPLE_HOOK_ENTITY.get(), ThrownItemRenderer::new);
    }
}
