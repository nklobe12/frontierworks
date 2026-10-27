package com.frontierworks.storage.client;

import com.frontierworks.storage.Content;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.IEventBus;

public final class StorageClient {
    private StorageClient() {}

    public static void init(IEventBus bus) {
        bus.addListener(StorageClient::renderers);
    }

    private static void renderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(Content.BULK_BARREL_BE.get(), BarrelRenderer::new);
        event.registerBlockEntityRenderer(Content.REINFORCED_BULK_BARREL_BE.get(), BarrelRenderer::new);
        event.registerBlockEntityRenderer(Content.ELITE_BULK_BARREL_BE.get(), BarrelRenderer::new);
    }
}
