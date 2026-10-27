package com.frontierworks.rocketry.client;

import com.frontierworks.rocketry.RocketEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

public class RocketRenderer extends EntityRenderer<RocketEntity> {
    private static final ResourceLocation T1 = new ResourceLocation("fw_rocketry", "textures/entity/rocket.png");
    private static final ResourceLocation T2 = new ResourceLocation("fw_rocketry", "textures/entity/rocket_t2.png");
    private final RocketModel<RocketEntity> model;

    public RocketRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
        this.model = new RocketModel<>(ctx.bakeLayer(RocketModel.LAYER));
        this.shadowRadius = 0.9f;
    }

    @Override
    public void render(RocketEntity rocket, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(180.0f - yaw));
        pose.scale(-1.0f, -1.0f, 1.0f);
        pose.translate(0.0, -1.501, 0.0);
        model.setupAnim(rocket, 0, 0, rocket.tickCount + partialTick, 0, 0);
        VertexConsumer vc = buffers.getBuffer(RenderType.entityCutoutNoCull(getTextureLocation(rocket)));
        model.renderToBuffer(pose, vc, light, OverlayTexture.NO_OVERLAY, 1f, 1f, 1f, 1f);
        pose.popPose();
        super.render(rocket, yaw, partialTick, pose, buffers, light);
    }

    @Override
    public ResourceLocation getTextureLocation(RocketEntity rocket) {
        return rocket.tier() >= 2 ? T2 : T1;
    }
}
