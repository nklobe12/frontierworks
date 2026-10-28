package com.lodestar.quarry.client;

import com.lodestar.core.client.RenderUtil;
import com.lodestar.quarry.MarkerBE;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;

/**
 * Survey marker beams: a linked marker draws laser lines to its partners and along the four edges of the frame;
 * with a wrench or marker in hand the whole column that will be dug is outlined down to bedrock.
 */
public class MarkerRenderer implements BlockEntityRenderer<MarkerBE> {
    private static final int BEAM = 0xB04A8CFF;
    private static final double H = 0.75;

    public MarkerRenderer(BlockEntityRendererProvider.Context ctx) {}

    @Override
    public void render(MarkerBE be, float pt, PoseStack ps, MultiBufferSource buf, int light, int overlay) {
        BlockPos o = be.getBlockPos();
        BlockPos px = be.partnerX(), pz = be.partnerZ();
        BlockPos a = be.areaMin(), b = be.areaMax();
        if (a != null && b != null) {
            if (px != null || pz != null) {
                double ax = a.getX() + 0.5 - o.getX(), az = a.getZ() + 0.5 - o.getZ(), bx = b.getX() + 0.5 - o.getX(), bz = b.getZ() + 0.5 - o.getZ();
                RenderUtil.beam(ps, buf, ax, H, az, bx, H, az, 0.06f, BEAM);
                RenderUtil.beam(ps, buf, ax, H, bz, bx, H, bz, 0.06f, BEAM);
                RenderUtil.beam(ps, buf, ax, H, az, ax, H, bz, 0.06f, BEAM);
                RenderUtil.beam(ps, buf, bx, H, az, bx, H, bz, 0.06f, BEAM);
            }
            if (AreaPreview.surveying() && be.getLevel() != null && (px != null || pz != null)) {
                AreaPreview.box(ps, buf, o, a.getX() + 1, be.getLevel().getMinBuildHeight(), a.getZ() + 1, b.getX() - 1, o.getY(), b.getZ() - 1,
                        1f, 0.82f, 0.2f, 1f);
                AreaPreview.box(ps, buf, o, a.getX(), o.getY(), a.getZ(), b.getX(), o.getY() + 4, b.getZ(), 0.3f, 0.55f, 1f, 1f);
            }
            return;
        }
        for (BlockPos p : new BlockPos[]{px, pz}) {
            if (p == null) continue;
            RenderUtil.beam(ps, buf, 0.5, H, 0.5, p.getX() + 0.5 - o.getX(), H, p.getZ() + 0.5 - o.getZ(), 0.06f, BEAM);
        }
    }

    @Override
    public boolean shouldRenderOffScreen(MarkerBE be) {
        return true;
    }

    @Override
    public int getViewDistance() {
        return 128;
    }
}
