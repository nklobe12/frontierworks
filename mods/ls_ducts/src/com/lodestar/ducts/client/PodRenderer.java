package com.lodestar.ducts.client;

import com.lodestar.core.client.RenderUtil;
import com.lodestar.ducts.transit.TransitPod;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;

/** A faint glass bubble around the rider. */
public class PodRenderer extends EntityRenderer<TransitPod> {
    public PodRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
    }

    @Override
    public void render(TransitPod pod, float yaw, float pt, PoseStack ps, MultiBufferSource buf, int light) {
        RenderUtil.colorBox(ps, buf, -0.42f, -0.02f, -0.42f, 0.42f, 0.82f, 0.42f, 0x2C7FD8FF);
        super.render(pod, yaw, pt, ps, buf, light);
    }

    @Override
    public ResourceLocation getTextureLocation(TransitPod pod) {
        return TextureAtlas.LOCATION_BLOCKS;
    }
}
