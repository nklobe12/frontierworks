package com.lodestar.quarry.client;

import com.lodestar.quarry.AbstractQuarryBE;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;

/** Outline drawing for quarry areas: the whole dig column and the layer being worked. */
final class AreaPreview {
    private AreaPreview() {}

    private static final ResourceLocation WRENCH = new ResourceLocation("ls_core", "wrench");
    private static final ResourceLocation MARKER = new ResourceLocation("ls_quarry", "survey_marker");

    /** True while the local player holds a wrench or a survey marker. */
    static boolean surveying() {
        LocalPlayer p = Minecraft.getInstance().player;
        if (p == null) return false;
        return holds(p.getMainHandItem()) || holds(p.getOffhandItem());
    }

    private static boolean holds(ItemStack st) {
        if (st.isEmpty()) return false;
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(st.getItem());
        return WRENCH.equals(id) || MARKER.equals(id);
    }

    /** Box from world corners (inclusive block coordinates) relative to the block entity at origin. */
    static void box(PoseStack ps, MultiBufferSource buf, BlockPos origin, int ax, int ay, int az, int bx, int by, int bz, float r, float g, float b, float a) {
        VertexConsumer vc = buf.getBuffer(RenderType.lines());
        AABB box = new AABB(ax - origin.getX(), ay - origin.getY(), az - origin.getZ(), bx + 1 - origin.getX(), by + 1 - origin.getY(), bz + 1 - origin.getZ());
        LevelRenderer.renderLineBox(ps, vc, box.inflate(0.002), r, g, b, a);
    }

    /** Whole dig column (yellow) plus the current layer (cyan) of a quarry. */
    static void quarry(PoseStack ps, MultiBufferSource buf, AbstractQuarryBE be) {
        BlockPos o = be.getBlockPos();
        box(ps, buf, o, be.x0(), be.floorY(), be.z0(), be.x1(), be.topY(), be.z1(), 1f, 0.82f, 0.2f, 1f);
        if (be.phase() == AbstractQuarryBE.Phase.MINING || be.phase() == AbstractQuarryBE.Phase.RESTORING) {
            box(ps, buf, o, be.x0(), be.curY(), be.z0(), be.x1(), be.curY(), be.z1(), 0.3f, 0.9f, 1f, 1f);
        }
    }
}
