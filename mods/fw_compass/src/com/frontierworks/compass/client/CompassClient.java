package com.frontierworks.compass.client;

import com.frontierworks.compass.Content;
import com.frontierworks.compass.ExplorersCompassItem;
import net.minecraft.client.renderer.item.CompassItemPropertyFunction;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

public final class CompassClient {
    private CompassClient() {}

    public static void init(IEventBus bus) {
        bus.addListener(CompassClient::setup);
    }

    private static void setup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> ItemProperties.register(Content.EXPLORERS_COMPASS.get(), new ResourceLocation("angle"),
                new CompassItemPropertyFunction((level, stack, entity) -> ExplorersCompassItem.target(stack))));
    }
}
