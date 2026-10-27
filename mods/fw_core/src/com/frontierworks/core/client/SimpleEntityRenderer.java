package com.frontierworks.core.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;

/** Renders a non-living entity (vehicles, rockets) with an EntityModel, facing its yaw. */
public class SimpleEntityRenderer<T extends Entity> extends EntityRenderer<T> {
    private final EntityModel<T> model;
    private final ResourceLocation texture;
    private final float scale;

    public SimpleEntityRenderer(EntityRendererProvider.Context ctx, EntityModel<T> model, ResourceLocation texture, float scale, float shadow) {
        super(ctx);
        this.model = model;
        this.texture = texture;
        this.scale = scale;
        this.shadowRadius = shadow;
    }

    @Override
    public void render(T entity, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(180.0f - yaw));
        pose.scale(-scale, -scale, scale);
        pose.translate(0.0, -1.501, 0.0);
        model.setupAnim(entity, 0, 0, entity.tickCount + partialTick, 0, 0);
        VertexConsumer vc = buffers.getBuffer(RenderType.entityCutoutNoCull(texture));
        model.renderToBuffer(pose, vc, light, OverlayTexture.NO_OVERLAY, 1f, 1f, 1f, 1f);
        pose.popPose();
        super.render(entity, yaw, partialTick, pose, buffers, light);
    }

    @Override
    public ResourceLocation getTextureLocation(T entity) {
        return texture;
    }
}
