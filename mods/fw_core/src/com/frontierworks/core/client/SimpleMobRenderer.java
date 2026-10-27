package com.frontierworks.core.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Mob;

/** Mob renderer for a model + single texture, with an optional uniform scale. */
public class SimpleMobRenderer<T extends Mob, M extends EntityModel<T>> extends MobRenderer<T, M> {
    private final ResourceLocation texture;
    private final float scale;

    public SimpleMobRenderer(EntityRendererProvider.Context ctx, M model, float shadow, ResourceLocation texture, float scale) {
        super(ctx, model, shadow * scale);
        this.texture = texture;
        this.scale = scale;
    }

    @Override
    public ResourceLocation getTextureLocation(T entity) {
        return texture;
    }

    @Override
    protected void scale(T entity, PoseStack pose, float partialTick) {
        if (scale != 1.0f) pose.scale(scale, scale, scale);
    }

    /** Humanoid (zombie-shaped) mob with a custom 64x64 skin. */
    public static class Humanoid<T extends Mob> extends HumanoidMobRenderer<T, HumanoidModel<T>> {
        private final ResourceLocation texture;
        private final float scale;

        public Humanoid(EntityRendererProvider.Context ctx, ResourceLocation texture, float scale) {
            super(ctx, new HumanoidModel<>(ctx.bakeLayer(ModelLayers.ZOMBIE)), 0.5f * scale);
            this.texture = texture;
            this.scale = scale;
        }

        @Override
        public ResourceLocation getTextureLocation(T entity) {
            return texture;
        }

        @Override
        protected void scale(T entity, PoseStack pose, float partialTick) {
            if (scale != 1.0f) pose.scale(scale, scale, scale);
        }
    }
}
