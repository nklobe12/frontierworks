package com.lodestar.core.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import net.minecraftforge.fluids.FluidStack;
import org.joml.Matrix4f;

/** Helpers for block entity renderers: items, fluid boxes, colored boxes, beams and floating text. */
public final class RenderUtil {
    private RenderUtil() {}

    /** Renders an item centered at the current pose origin, scaled. */
    public static void item(PoseStack ps, MultiBufferSource buf, ItemStack stack, float scale, int light, Level level, int seed) {
        if (stack.isEmpty()) return;
        ps.pushPose();
        ps.scale(scale, scale, scale);
        Minecraft.getInstance().getItemRenderer().renderStatic(stack, ItemDisplayContext.FIXED, light, OverlayTexture.NO_OVERLAY, ps, buf, level, seed);
        ps.popPose();
    }

    /** Item lying flat on a surface (e.g. on a casting table), at (x, y, z) in block space. */
    public static void flatItem(PoseStack ps, MultiBufferSource buf, ItemStack stack, double x, double y, double z, float scale, int light, Level level) {
        ps.pushPose();
        ps.translate(x, y, z);
        ps.mulPose(Axis.XP.rotationDegrees(90));
        item(ps, buf, stack, scale, light, level, 0);
        ps.popPose();
    }

    /** Fluid box from (x0,y0,z0) to (x1,y1,z1) in block space (0..1), using the fluid's still texture and tint. */
    public static void fluidBox(PoseStack ps, MultiBufferSource buf, FluidStack fluid, float x0, float y0, float z0, float x1, float y1, float z1, int light) {
        if (fluid.isEmpty() || y1 <= y0) return;
        IClientFluidTypeExtensions ext = IClientFluidTypeExtensions.of(fluid.getFluid());
        TextureAtlasSprite sprite = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(ext.getStillTexture(fluid));
        int tint = ext.getTintColor(fluid);
        int lum = fluid.getFluid().getFluidType().getLightLevel(fluid);
        int l = lum > 0 ? LightTexture.pack(Math.max(lum, LightTexture.block(light)), LightTexture.sky(light)) : light;
        VertexConsumer vc = buf.getBuffer(RenderType.translucent());
        box(ps.last().pose(), vc, sprite, x0, y0, z0, x1, y1, z1, tint, l);
    }

    private static void box(Matrix4f m, VertexConsumer vc, TextureAtlasSprite s, float x0, float y0, float z0, float x1, float y1, float z1, int argb, int light) {
        float a = ((argb >> 24) & 0xFF) / 255f, r = ((argb >> 16) & 0xFF) / 255f, g = ((argb >> 8) & 0xFF) / 255f, b = (argb & 0xFF) / 255f;
        if (a <= 0.01f) a = 1f;
        float u0 = s.getU(x0 * 16), u1 = s.getU(x1 * 16), v0 = s.getV(z0 * 16), v1 = s.getV(z1 * 16);
        // top
        quad(m, vc, x0, y1, z0, x0, y1, z1, x1, y1, z1, x1, y1, z0, u0, v0, u1, v1, r, g, b, a, light, 0, 1, 0);
        // bottom
        quad(m, vc, x0, y0, z1, x0, y0, z0, x1, y0, z0, x1, y0, z1, u0, v0, u1, v1, r, g, b, a, light, 0, -1, 0);
        float vs0 = s.getV((1 - y1) * 16), vs1 = s.getV((1 - y0) * 16);
        float ux0 = s.getU(x0 * 16), ux1 = s.getU(x1 * 16), uz0 = s.getU(z0 * 16), uz1 = s.getU(z1 * 16);
        // north (z0)
        quad(m, vc, x1, y1, z0, x1, y0, z0, x0, y0, z0, x0, y1, z0, ux0, vs0, ux1, vs1, r, g, b, a, light, 0, 0, -1);
        // south (z1)
        quad(m, vc, x0, y1, z1, x0, y0, z1, x1, y0, z1, x1, y1, z1, ux0, vs0, ux1, vs1, r, g, b, a, light, 0, 0, 1);
        // west (x0)
        quad(m, vc, x0, y1, z0, x0, y0, z0, x0, y0, z1, x0, y1, z1, uz0, vs0, uz1, vs1, r, g, b, a, light, -1, 0, 0);
        // east (x1)
        quad(m, vc, x1, y1, z1, x1, y0, z1, x1, y0, z0, x1, y1, z0, uz0, vs0, uz1, vs1, r, g, b, a, light, 1, 0, 0);
    }

    private static void quad(Matrix4f m, VertexConsumer vc, float x0, float y0, float z0, float x1, float y1, float z1, float x2, float y2, float z2,
                             float x3, float y3, float z3, float u0, float v0, float u1, float v1, float r, float g, float b, float a, int light,
                             float nx, float ny, float nz) {
        vc.vertex(m, x0, y0, z0).color(r, g, b, a).uv(u0, v0).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(nx, ny, nz).endVertex();
        vc.vertex(m, x1, y1, z1).color(r, g, b, a).uv(u0, v1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(nx, ny, nz).endVertex();
        vc.vertex(m, x2, y2, z2).color(r, g, b, a).uv(u1, v1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(nx, ny, nz).endVertex();
        vc.vertex(m, x3, y3, z3).color(r, g, b, a).uv(u1, v0).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(nx, ny, nz).endVertex();
    }

    /** Solid colored box (no texture), e.g. for beams, markers and simple moving parts. */
    public static void colorBox(PoseStack ps, MultiBufferSource buf, float x0, float y0, float z0, float x1, float y1, float z1, int argb) {
        VertexConsumer vc = buf.getBuffer(RenderType.debugQuads());
        Matrix4f m = ps.last().pose();
        float a = ((argb >> 24) & 0xFF) / 255f, r = ((argb >> 16) & 0xFF) / 255f, g = ((argb >> 8) & 0xFF) / 255f, b = (argb & 0xFF) / 255f;
        float[][] f = {
                {x0, y1, z0, x0, y1, z1, x1, y1, z1, x1, y1, z0}, {x0, y0, z1, x0, y0, z0, x1, y0, z0, x1, y0, z1},
                {x1, y1, z0, x1, y0, z0, x0, y0, z0, x0, y1, z0}, {x0, y1, z1, x0, y0, z1, x1, y0, z1, x1, y1, z1},
                {x0, y1, z0, x0, y0, z0, x0, y0, z1, x0, y1, z1}, {x1, y1, z1, x1, y0, z1, x1, y0, z0, x1, y1, z0}};
        for (float[] q : f) {
            for (int i = 0; i < 4; i++) vc.vertex(m, q[i * 3], q[i * 3 + 1], q[i * 3 + 2]).color(r, g, b, a).endVertex();
        }
    }

    /** Line beam between two points (block-space coordinates relative to the pose origin) as a thin box. */
    public static void beam(PoseStack ps, MultiBufferSource buf, double x0, double y0, double z0, double x1, double y1, double z1, float thickness, int argb) {
        double dx = x1 - x0, dy = y1 - y0, dz = z1 - z0;
        float len = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (len < 1e-4) return;
        ps.pushPose();
        ps.translate(x0, y0, z0);
        float yaw = (float) Math.atan2(dx, dz);
        float pitch = (float) Math.atan2(dy, Math.sqrt(dx * dx + dz * dz));
        ps.mulPose(Axis.YP.rotation(yaw));
        ps.mulPose(Axis.XP.rotation(-pitch));
        float t = thickness / 2;
        colorBox(ps, buf, -t, -t, 0, t, t, len, argb);
        ps.popPose();
    }

    /** Text floating at the pose origin, facing the camera. */
    public static void billboardText(PoseStack ps, MultiBufferSource buf, Component text, int color, int light) {
        Minecraft mc = Minecraft.getInstance();
        ps.pushPose();
        ps.mulPose(mc.getEntityRenderDispatcher().cameraOrientation());
        ps.scale(-0.025f, -0.025f, 0.025f);
        float w = mc.font.width(text) / 2f;
        mc.font.drawInBatch(text, -w, 0, color, false, ps.last().pose(), buf, net.minecraft.client.gui.Font.DisplayMode.NORMAL, 0x40000000, light);
        ps.popPose();
    }
}
