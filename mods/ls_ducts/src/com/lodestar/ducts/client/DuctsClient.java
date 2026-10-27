package com.lodestar.ducts.client;

import com.lodestar.core.client.RenderUtil;
import com.lodestar.core.net.CoreNet;
import com.lodestar.ducts.DuctTable;
import com.lodestar.ducts.DuctsSetup;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

public final class DuctsClient {
    private DuctsClient() {}

    public static void init(IEventBus bus) {
        for (String m : DuctRenderer.MODELS) RenderUtil.extraModel(new ResourceLocation("ls_ducts", "block/" + m));
        bus.addListener((FMLClientSetupEvent e) -> e.enqueueWork(() -> MenuScreens.register(DuctsSetup.FILTER_MENU.get(), FilterScreen::new)));
        bus.addListener((EntityRenderersEvent.RegisterRenderers e) -> {
            for (var t : DuctTable.types()) e.registerBlockEntityRenderer(t.get(), DuctRenderer::new);
            e.registerEntityRenderer(DuctsSetup.TRANSIT_POD.get(), PodRenderer::new);
        });
        CoreNet.registerClient("ls_ducts:stations", tag -> Minecraft.getInstance().setScreen(new StationScreen(tag)));
    }
}
