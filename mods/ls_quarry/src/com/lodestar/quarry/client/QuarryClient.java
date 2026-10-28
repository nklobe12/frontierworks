package com.lodestar.quarry.client;

import com.lodestar.core.client.MachineScreen;
import com.lodestar.core.client.RenderUtil;
import com.lodestar.quarry.Content;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

public final class QuarryClient {
    private QuarryClient() {}

    public static final ResourceLocation RAIL = new ResourceLocation("ls_quarry", "block/gantry_rail");
    public static final ResourceLocation HEAD = new ResourceLocation("ls_quarry", "block/drill_head");
    public static final ResourceLocation SHAFT = new ResourceLocation("ls_quarry", "block/drill_shaft");
    public static final ResourceLocation BIT = new ResourceLocation("ls_quarry", "block/drill_bit");

    public static void init(IEventBus bus) {
        for (ResourceLocation id : new ResourceLocation[]{RAIL, HEAD, SHAFT, BIT}) RenderUtil.extraModel(id);
        bus.addListener((EntityRenderersEvent.RegisterRenderers e) -> {
            e.registerBlockEntityRenderer(Content.QUARRY_BE.get(), QuarryRenderer::new);
            e.registerBlockEntityRenderer(Content.LASER_QUARRY_BE.get(), LaserQuarryRenderer::new);
            e.registerBlockEntityRenderer(Content.SURVEY_MARKER_BE.get(), MarkerRenderer::new);
        });
        bus.addListener((FMLClientSetupEvent e) -> e.enqueueWork(() -> {
            MachineScreen.register(Content.QUARRY_BE.get(), QuarryScreen::new);
            MachineScreen.register(Content.LASER_QUARRY_BE.get(), QuarryScreen::new);
        }));
    }
}
