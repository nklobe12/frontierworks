package com.frontierworks.explosives.client;

import com.frontierworks.explosives.Content;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.IEventBus;

public final class ExplosivesClient {
    private ExplosivesClient() {}

    public static void init(IEventBus bus) {
        bus.addListener((EntityRenderersEvent.RegisterRenderers e) -> e.registerEntityRenderer(Content.THROWN.get(), ThrownItemRenderer::new));
    }
}
