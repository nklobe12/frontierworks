package com.lodestar.datastore.client;

import com.lodestar.core.block.LSEntityBlock;
import com.lodestar.core.client.RenderUtil;
import com.lodestar.datastore.DriveBayBE;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;

/** Draws the disks in a drive bay's front slots, coloured by tier, with a fill light. */
public class DriveBayRenderer implements BlockEntityRenderer<DriveBayBE> {
    private static final int[] TIER = {0xFFD9844F, 0xFFECD97A, 0xFF9FD0DE, 0xFF3C6FD6};

    public DriveBayRenderer(BlockEntityRendererProvider.Context ctx) {}

    @Override
    public void render(DriveBayBE be, float pt, PoseStack ps, MultiBufferSource buf, int light, int overlay) {
        Direction facing = LSEntityBlock.facingOf(be.getBlockState());
        ps.pushPose();
        ps.translate(0.5, 0.5, 0.5);
        switch (facing) {
            case SOUTH -> ps.mulPose(Axis.YP.rotationDegrees(180));
            case EAST -> ps.mulPose(Axis.YP.rotationDegrees(-90));
            case WEST -> ps.mulPose(Axis.YP.rotationDegrees(90));
            default -> {}
        }
        ps.translate(-0.5, -0.5, -0.5);
        for (int i = 0; i < DriveBayBE.SLOTS; i++) {
            int v = be.visual[i];
            if (v == 0) continue;
            int tier = Math.max(0, Math.min(3, (v & 7) - 1));
            int fill = (v >> 4) & 3;
            boolean online = (v & 256) != 0;
            int col = i % 2, row = i / 2;
            float u0 = 2 + col * 6, v0 = 2 + row * 3;
            // texture u maps to block x = 16 - u on the north face; disk body u0..u0+5, light u0+5..u0+6
            float x0 = (16 - (u0 + 5)) / 16f, x1 = (16 - u0) / 16f, y0 = (16 - (v0 + 2)) / 16f, y1 = (16 - v0) / 16f;
            RenderUtil.colorBox(ps, buf, x0 + 0.01f, y0 + 0.01f, -0.012f, x1, y1 - 0.01f, 0.004f, 0xFF262B33);
            RenderUtil.colorBox(ps, buf, x0 + 0.03f, y0 + 0.03f, -0.016f, x1 - 0.03f, y1 - 0.03f, -0.011f, TIER[tier]);
            int led = !online ? 0xFF3A3F46 : fill == 2 ? 0xFFE04040 : fill == 1 ? 0xFFE0C040 : 0xFF40E070;
            float lx = (16 - (u0 + 6)) / 16f;
            RenderUtil.colorBox(ps, buf, lx + 0.005f, y0 + 0.02f, -0.014f, lx + 1 / 16f - 0.005f, y1 - 0.02f, 0.004f, led);
        }
        ps.popPose();
    }
}
