package com.lodestar.quarry.client;

import com.lodestar.core.client.RenderUtil;
import com.lodestar.quarry.AbstractQuarryBE;
import com.lodestar.quarry.LaserQuarryBE;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;

/** The cutting laser from the underside of the laser quarry to the block it is working on, and the area outline. */
public class LaserQuarryRenderer implements BlockEntityRenderer<LaserQuarryBE> {
    public LaserQuarryRenderer(BlockEntityRendererProvider.Context ctx) {}

    @Override
    public void render(LaserQuarryBE be, float pt, PoseStack ps, MultiBufferSource buf, int light, int overlay) {
        if (!be.hasArea()) return;
        Level level = be.getLevel();
        if (level == null) return;
        if (be.showArea() || AreaPreview.surveying()) AreaPreview.quarry(ps, buf, be);
        AbstractQuarryBE.Phase ph = be.phase();
        BlockPos h = be.head();
        if (h == null || !be.isLitClient() || (ph != AbstractQuarryBE.Phase.MINING && ph != AbstractQuarryBE.Phase.RESTORING)) return;
        BlockPos o = be.getBlockPos();
        BlockPos from = be.clientPrevHead != null ? be.clientPrevHead : h;
        float f = Mth.clamp((level.getGameTime() - be.clientHeadTime + pt) / Math.max(1, be.clientMoveTicks), 0f, 1f);
        double tx = Mth.lerp(f, from.getX(), h.getX()) + 0.5 - o.getX();
        double ty = Mth.lerp(f, from.getY(), h.getY()) + 0.5 - o.getY();
        double tz = Mth.lerp(f, from.getZ(), h.getZ()) + 0.5 - o.getZ();
        boolean restoring = ph == AbstractQuarryBE.Phase.RESTORING;
        float pulse = 0.5f + 0.5f * Mth.sin((level.getGameTime() + pt) * 0.8f);
        int outer = restoring ? 0x6040E070 : 0x60FF3020;
        int core = restoring ? 0xFFD0FFD8 : 0xFFFFE6D0;
        RenderUtil.beam(ps, buf, 0.5, 0.0, 0.5, tx, ty, tz, 0.16f + 0.04f * pulse, outer);
        RenderUtil.beam(ps, buf, 0.5, 0.0, 0.5, tx, ty, tz, 0.05f, core);
        float s = 0.18f + 0.08f * pulse;
        RenderUtil.colorBox(ps, buf, (float) tx - s, (float) ty - s, (float) tz - s, (float) tx + s, (float) ty + s, (float) tz + s,
                restoring ? 0x9050FF80 : 0x90FF8030);
    }

    @Override
    public boolean shouldRenderOffScreen(LaserQuarryBE be) {
        return true;
    }

    @Override
    public int getViewDistance() {
        return 192;
    }
}
