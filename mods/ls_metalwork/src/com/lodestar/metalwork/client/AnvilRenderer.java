package com.lodestar.metalwork.client;

import com.lodestar.core.client.RenderUtil;
import com.lodestar.metalwork.AnvilBE;
import com.lodestar.metalwork.Heat;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/** Draws the workpiece on the anvil glowing with its heat, and the chosen shape with strike progress above it. */
public class AnvilRenderer implements BlockEntityRenderer<AnvilBE> {
    public AnvilRenderer(BlockEntityRendererProvider.Context ctx) {}

    @Override
    public void render(AnvilBE be, float pt, PoseStack ps, MultiBufferSource buf, int light, int overlay) {
        ItemStack work = be.work();
        if (work.isEmpty()) return;
        int heat = Heat.get(work);
        int glow = Heat.glowColor(heat);
        int l = glow != 0 ? LightTexture.FULL_BRIGHT : light;
        RenderUtil.flatItem(ps, buf, work, 0.5, 1.01, 0.5, 0.55f, l, be.getLevel());
        if (glow != 0) {
            int alpha = Math.min(150, 40 + (heat - 400) / 8);
            RenderUtil.colorBox(ps, buf, 0.32f, 1.012f, 0.32f, 0.68f, 1.03f, 0.68f, (alpha << 24) | glow);
        }
        if (be.required() > 0) {
            ps.pushPose();
            ps.translate(0.5, 1.45, 0.5);
            Component label = AnvilBE.shapeName(be.shape()).copy().append(" " + (be.progress() / 2) + "/" + (be.required() / 2));
            RenderUtil.billboardText(ps, buf, label, 0xFFFFFFFF, LightTexture.FULL_BRIGHT);
            ps.popPose();
        }
    }
}
