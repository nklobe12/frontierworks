package com.lodestar.aeolus.client;

import com.lodestar.aeolus.Content;
import com.lodestar.core.client.MachineScreen;
import com.lodestar.core.client.RenderUtil;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

/** Client setup for Wind & Water: renderers, the extra models they draw, and the turbine and tidal screens. */
public final class AeolusClient {
    private AeolusClient() {}

    public static void init(IEventBus bus) {
        AeolusRenderers.models().forEach(RenderUtil::extraModel);
        bus.addListener((EntityRenderersEvent.RegisterRenderers e) -> {
            e.registerBlockEntityRenderer(Content.WIND_TURBINE_BE.get(), AeolusRenderers.Turbine::new);
            e.registerBlockEntityRenderer(Content.WATER_WHEEL_BE.get(), AeolusRenderers.Wheel::new);
            e.registerBlockEntityRenderer(Content.TIDAL_GENERATOR_BE.get(), AeolusRenderers.Tidal::new);
            e.registerBlockEntityRenderer(Content.ANEMOMETER_BE.get(), AeolusRenderers.Anemometer::new);
        });
        bus.addListener((FMLClientSetupEvent e) -> e.enqueueWork(() -> {
            MachineScreen.register(Content.WIND_TURBINE_BE.get(), TurbineScreen::new);
            MachineScreen.register(Content.TIDAL_GENERATOR_BE.get(), TidalScreen::new);
        }));
    }
}
