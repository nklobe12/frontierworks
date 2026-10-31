package com.lodestar.petroleum.client;

import com.lodestar.core.block.LSEntityBlock;
import com.lodestar.core.client.RenderUtil;
import com.lodestar.petroleum.PumpjackBE;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

/**
 * Draws the moving parts of a formed pumpjack: the crank and counterweights turning over the motor, pitman arms, the
 * walking beam rocking on the samson post, the horsehead and the polished rod going down into the wellhead.
 */
public class PumpjackRenderer implements BlockEntityRenderer<PumpjackBE> {
    static final ResourceLocation BEAM = new ResourceLocation("ls_petroleum", "block/pumpjack_beam_segment");
    static final ResourceLocation HEAD = new ResourceLocation("ls_petroleum", "block/pumpjack_horsehead");
    static final ResourceLocation CRANK = new ResourceLocation("ls_petroleum", "block/pumpjack_crank");
    static final ResourceLocation SADDLE = new ResourceLocation("ls_petroleum", "block/pumpjack_saddle");
    static final List<ResourceLocation> MODELS = List.of(BEAM, HEAD, CRANK, SADDLE);

    private static final float AMPLITUDE = (float) Math.toRadians(14);
    private static final float PIVOT_Y = 3.15f, PIVOT_Z = 2.5f, REAR = 1.6f, FRONT = 1.6f;
    private static final float AXLE_Y = 1.55f, AXLE_Z = 0.5f, CRANK_R = 0.41f;

    public PumpjackRenderer(BlockEntityRendererProvider.Context ctx) {}

    @Override
    public void render(PumpjackBE be, float pt, PoseStack ps, MultiBufferSource buf, int packedLight, int overlay) {
        if (!be.formed || be.getLevel() == null) return;
        int light = LevelRenderer.getLightColor(be.getLevel(), be.getBlockPos().above(2));
        Direction facing = LSEntityBlock.facingOf(be.getBlockState());
        float phase = be.prevPhase + (be.phase - be.prevPhase) * pt;
        float phi = (float) (phase * Math.PI * 2);
        float theta = (float) (-AMPLITUDE * Math.cos(phi));
        ps.pushPose();
        ps.translate(0.5, 0, 0.5);
        ps.mulPose(Axis.YP.rotationDegrees(-facing.toYRot()));
        ps.translate(-0.5, 0, -0.5);

        // bearing saddle on the post
        ps.pushPose();
        ps.translate(0, 3.0, 2.0);
        RenderUtil.model(ps, buf, SADDLE, light, overlay);
        ps.popPose();

        // walking beam and horsehead
        float len = REAR + FRONT;
        ps.pushPose();
        ps.translate(0.5, PIVOT_Y, PIVOT_Z);
        ps.mulPose(Axis.XP.rotation(-theta));
        ps.translate(-0.5, -0.47, -REAR);
        int full = (int) len;
        for (int i = 0; i < full; i++) {
            ps.pushPose();
            ps.translate(0, 0, i);
            RenderUtil.model(ps, buf, BEAM, light, overlay);
            ps.popPose();
        }
        ps.pushPose();
        ps.translate(0, 0, full);
        ps.scale(1, 1, len - full);
        RenderUtil.model(ps, buf, BEAM, light, overlay);
        ps.popPose();
        ps.pushPose();
        ps.translate(0, -0.35, len - 0.15);
        RenderUtil.model(ps, buf, HEAD, light, overlay);
        ps.popPose();
        ps.popPose();

        // crank and counterweights over the motor
        ps.pushPose();
        ps.translate(0.5, AXLE_Y, AXLE_Z);
        ps.mulPose(Axis.XP.rotation(phi));
        ps.translate(-0.5, -0.5, -0.5);
        RenderUtil.model(ps, buf, CRANK, light, overlay);
        ps.popPose();

        // pitman arms from the crank pins to the back of the beam
        float pinY = AXLE_Y + CRANK_R * (float) Math.cos(phi), pinZ = AXLE_Z + CRANK_R * (float) Math.sin(phi);
        float[] rear = rotX(-0.47f + 0.47f, -(REAR - 0.1f), -theta);
        float rearY = PIVOT_Y + rear[0], rearZ = PIVOT_Z + rear[1];
        RenderUtil.beam(ps, buf, 0.125, pinY, pinZ, 0.3, rearY, rearZ, 0.07f, 0xFF3A3E44);
        RenderUtil.beam(ps, buf, 0.875, pinY, pinZ, 0.7, rearY, rearZ, 0.07f, 0xFF3A3E44);

        // polished rod hanging from the horsehead into the wellhead
        float[] tip = rotX(-0.72f, FRONT + 0.3f, -theta);
        float tipY = PIVOT_Y + tip[0];
        RenderUtil.beam(ps, buf, 0.5, tipY, 4.5, 0.5, 0.85, 4.5, 0.05f, 0xFF9AA0A8);
        ps.popPose();
    }

    /** Rotates (y, z) about the x axis by angle a. */
    private static float[] rotX(float y, float z, float a) {
        float c = (float) Math.cos(a), s = (float) Math.sin(a);
        return new float[]{y * c - z * s, y * s + z * c};
    }

    @Override
    public boolean shouldRenderOffScreen(PumpjackBE be) {
        return true;
    }

    @Override
    public int getViewDistance() {
        return 96;
    }
}
