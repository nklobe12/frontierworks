package com.lodestar.kinetics.client;

import com.lodestar.core.client.RenderUtil;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/** Transform helpers shared by the Kinetics renderers. Models are built along +Y and turned onto their axis here. */
final class KRender {
    private KRender() {}

    static ResourceLocation model(String name) {
        return new ResourceLocation("ls_kinetics", "block/" + name);
    }

    /** Rotation by deg about the positive direction of a world axis. */
    static void spin(PoseStack ps, Direction.Axis a, float deg) {
        switch (a) {
            case X -> ps.mulPose(Axis.XP.rotationDegrees(deg));
            case Y -> ps.mulPose(Axis.YP.rotationDegrees(deg));
            case Z -> ps.mulPose(Axis.ZP.rotationDegrees(deg));
        }
    }

    /** Turns a model built along +Y so that +Y points along the positive direction of axis a. */
    static void orientAxis(PoseStack ps, Direction.Axis a) {
        if (a == Direction.Axis.X) ps.mulPose(Axis.ZP.rotationDegrees(-90));
        else if (a == Direction.Axis.Z) ps.mulPose(Axis.XP.rotationDegrees(90));
    }

    /** Turns a model built facing up so that it faces d. */
    static void orientUp(PoseStack ps, Direction d) {
        switch (d) {
            case UP -> {}
            case DOWN -> ps.mulPose(Axis.XP.rotationDegrees(180));
            case NORTH -> ps.mulPose(Axis.XP.rotationDegrees(-90));
            case SOUTH -> ps.mulPose(Axis.XP.rotationDegrees(90));
            case EAST -> ps.mulPose(Axis.ZP.rotationDegrees(-90));
            case WEST -> ps.mulPose(Axis.ZP.rotationDegrees(90));
        }
    }

    /** Draws a +Y model spinning along an axis through the block centre. */
    static void rotor(PoseStack ps, MultiBufferSource buf, String name, Direction.Axis a, float deg, int light, int overlay) {
        ps.pushPose();
        ps.translate(0.5, 0.5, 0.5);
        spin(ps, a, deg);
        orientAxis(ps, a);
        ps.translate(-0.5, -0.5, -0.5);
        RenderUtil.model(ps, buf, model(name), light, overlay);
        ps.popPose();
    }

    /** Draws a facing-up model turned to face d and spinning about d's axis by deg (about the positive axis). */
    static void facingRotor(PoseStack ps, MultiBufferSource buf, String name, Direction d, float deg, int light, int overlay) {
        ps.pushPose();
        ps.translate(0.5, 0.5, 0.5);
        spin(ps, d.getAxis(), deg);
        orientUp(ps, d);
        ps.translate(-0.5, -0.5, -0.5);
        RenderUtil.model(ps, buf, model(name), light, overlay);
        ps.popPose();
    }

    /** A flat double-sided coloured quad (belts). */
    static void quad(PoseStack ps, MultiBufferSource buf, Vec3 a, Vec3 b, Vec3 c, Vec3 d, int argb) {
        VertexConsumer vc = buf.getBuffer(RenderType.debugQuads());
        Matrix4f m = ps.last().pose();
        float al = ((argb >> 24) & 0xFF) / 255f, r = ((argb >> 16) & 0xFF) / 255f, g = ((argb >> 8) & 0xFF) / 255f, bl = (argb & 0xFF) / 255f;
        Vec3[] q = {a, b, c, d};
        for (Vec3 v : q) vc.vertex(m, (float) v.x, (float) v.y, (float) v.z).color(r, g, bl, al).endVertex();
        for (int i = 3; i >= 0; i--) vc.vertex(m, (float) q[i].x, (float) q[i].y, (float) q[i].z).color(r * 0.8f, g * 0.8f, bl * 0.8f, al).endVertex();
    }
}
