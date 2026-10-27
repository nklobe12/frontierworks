package com.frontierworks.storage.client;

import com.frontierworks.storage.BarrelBE;
import com.frontierworks.storage.BarrelBlock;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/** Draws the stored item and its count on the barrel's front face. */
public class BarrelRenderer implements BlockEntityRenderer<BarrelBE> {
    private final Font font;

    public BarrelRenderer(BlockEntityRendererProvider.Context ctx) {
        this.font = ctx.getFont();
    }

    @Override
    public void render(BarrelBE be, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        ItemStack type = be.getStoredType();
        if (type.isEmpty() || be.getLevel() == null) return;
        Direction facing = be.getBlockState().hasProperty(BarrelBlock.FACING) ? be.getBlockState().getValue(BarrelBlock.FACING) : Direction.NORTH;
        int faceLight = net.minecraft.client.renderer.LevelRenderer.getLightColor(be.getLevel(), be.getBlockPos().relative(facing));
        pose.pushPose();
        pose.translate(0.5, 0.5, 0.5);
        pose.mulPose(Axis.YP.rotationDegrees(-facing.toYRot()));
        pose.translate(0, 0.08, 0.505);
        pose.pushPose();
        pose.scale(0.45f, 0.45f, 0.01f);
        Minecraft.getInstance().getItemRenderer().renderStatic(type, ItemDisplayContext.GUI, faceLight, OverlayTexture.NO_OVERLAY, pose, buffers,
                be.getLevel(), 0);
        pose.popPose();
        String text = format(be.getCount());
        pose.translate(0, -0.3, 0.001);
        pose.scale(0.0125f, -0.0125f, 0.0125f);
        float w = font.width(text);
        font.drawInBatch(text, -w / 2f, 0, be.isLocked() ? 0xFFFFD050 : 0xFFFFFFFF, false, pose.last().pose(), buffers, Font.DisplayMode.NORMAL, 0, faceLight);
        pose.popPose();
    }

    private static String format(int n) {
        if (n >= 1_000_000) return String.format("%.1fM", n / 1_000_000.0);
        if (n >= 10_000) return String.format("%.1fk", n / 1000.0);
        return Integer.toString(n);
    }
}
