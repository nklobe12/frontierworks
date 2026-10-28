package com.lodestar.quarry.client;

import com.lodestar.core.client.RenderUtil;
import com.lodestar.quarry.AbstractQuarryBE;
import com.lodestar.quarry.QuarryBE;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;

/** Draws the drill gantry riding the top of the frame (rail, carriage, shaft and spinning bit) and the area outline. */
public class QuarryRenderer implements BlockEntityRenderer<QuarryBE> {
    public QuarryRenderer(BlockEntityRendererProvider.Context ctx) {}

    @Override
    public void render(QuarryBE be, float pt, PoseStack ps, MultiBufferSource buf, int light, int overlay) {
        if (!be.hasArea()) return;
        Level level = be.getLevel();
        if (level == null) return;
        if (be.showArea() || AreaPreview.surveying()) AreaPreview.quarry(ps, buf, be);
        AbstractQuarryBE.Phase ph = be.phase();
        if (ph != AbstractQuarryBE.Phase.MINING && ph != AbstractQuarryBE.Phase.RESTORING && ph != AbstractQuarryBE.Phase.DONE) return;
        BlockPos o = be.getBlockPos();
        int yTop = be.frameTop();
        double hx, hy, hz;
        BlockPos h = be.head();
        if (h == null || ph == AbstractQuarryBE.Phase.DONE) {
            hx = be.x0();
            hz = be.z0();
            hy = yTop - 1;
        } else {
            BlockPos from = be.clientPrevHead != null ? be.clientPrevHead : h;
            float f = Mth.clamp((level.getGameTime() - be.clientHeadTime + pt) / Math.max(1, be.clientMoveTicks), 0f, 1f);
            hx = Mth.lerp(f, from.getX(), h.getX());
            hy = Mth.lerp(f, from.getY(), h.getY());
            hz = Mth.lerp(f, from.getZ(), h.getZ());
            hy = Math.min(hy, yTop - 1);
        }
        int rz = (int) Math.round(hz);
        // rail spanning the frame along X at the carriage's Z
        for (int x = be.x0() - 1; x <= be.x1() + 1; x++) {
            ps.pushPose();
            ps.translate(x - o.getX(), yTop - o.getY(), hz - o.getZ());
            RenderUtil.model(ps, buf, QuarryClient.RAIL, LevelRenderer.getLightColor(level, new BlockPos(x, yTop, rz)), OverlayTexture.NO_OVERLAY);
            ps.popPose();
        }
        int headLight = LevelRenderer.getLightColor(level, new BlockPos((int) Math.round(hx), yTop, rz));
        ps.pushPose();
        ps.translate(hx - o.getX(), yTop - o.getY(), hz - o.getZ());
        RenderUtil.model(ps, buf, QuarryClient.HEAD, headLight, OverlayTexture.NO_OVERLAY);
        ps.popPose();
        // shaft down to the bit
        for (double y = yTop - 1; y > hy + 0.5; y -= 1) {
            ps.pushPose();
            ps.translate(hx - o.getX(), y - o.getY(), hz - o.getZ());
            RenderUtil.model(ps, buf, QuarryClient.SHAFT, LevelRenderer.getLightColor(level, new BlockPos((int) Math.round(hx), (int) Math.floor(y), rz)),
                    OverlayTexture.NO_OVERLAY);
            ps.popPose();
        }
        ps.pushPose();
        ps.translate(hx - o.getX() + 0.5, hy - o.getY(), hz - o.getZ() + 0.5);
        if (be.isLitClient()) ps.mulPose(Axis.YP.rotationDegrees((level.getGameTime() + pt) * 24f % 360f));
        ps.translate(-0.5, 0, -0.5);
        RenderUtil.model(ps, buf, QuarryClient.BIT, LevelRenderer.getLightColor(level, new BlockPos((int) Math.round(hx), (int) Math.round(hy), rz)),
                OverlayTexture.NO_OVERLAY);
        ps.popPose();
    }

    @Override
    public boolean shouldRenderOffScreen(QuarryBE be) {
        return true;
    }

    @Override
    public int getViewDistance() {
        return 192;
    }
}
