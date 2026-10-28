package com.lodestar.codex.client;

import com.lodestar.core.multiblock.MultiblockPattern;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * A multiblock pattern laid out for display: every block with its local position, drawn in 3D inside a GUI rectangle
 * with a turntable camera. The structure's front (the controller's facing) points towards the viewer at yaw 0.
 */
public final class StructurePreview {
    record Placed(int x, int y, int z, BlockState state) {}

    public final MultiblockPattern pattern;
    public final List<ItemStack> materials;
    private final List<Placed> blocks = new ArrayList<>();

    private StructurePreview(MultiblockPattern pattern, Block controller) {
        this.pattern = pattern;
        this.materials = pattern.materialStacks(controller);
        for (int y = 0; y < pattern.height; y++)
            for (int z = 0; z < pattern.depth; z++)
                for (int x = 0; x < pattern.width; x++) {
                    Block b = pattern.displayAt(x, y, z, controller);
                    if (b == null) continue;
                    BlockState st = face(b.defaultBlockState());
                    if (st.getRenderShape() == RenderShape.INVISIBLE) continue;
                    blocks.add(new Placed(x, y, z, st));
                }
    }

    /** Faces directional blocks (the controller especially) to the structure's front. */
    private static BlockState face(BlockState st) {
        if (st.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) return st.setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.SOUTH);
        if (st.hasProperty(BlockStateProperties.FACING)) return st.setValue(BlockStateProperties.FACING, Direction.SOUTH);
        return st;
    }

    @Nullable
    public static StructurePreview of(String id) {
        MultiblockPattern.Preview p = MultiblockPattern.preview(id);
        if (p == null) return null;
        try {
            return new StructurePreview(p.pattern().get(), p.controller().get());
        } catch (Exception e) {
            return null;
        }
    }

    public int layers() {
        return pattern.height;
    }

    /**
     * Draws the structure centred in (x, y, w, h). maxLayer limits the drawn layers (0-based, inclusive; -1 = all).
     */
    public void render(GuiGraphics g, int x, int y, int w, int h, float yaw, float pitch, float zoom, int maxLayer) {
        float diag = (float) Math.sqrt(pattern.width * pattern.width + pattern.height * pattern.height + pattern.depth * pattern.depth);
        float scale = Math.min(w, h) / Math.max(1f, diag) * zoom;
        BlockRenderDispatcher br = Minecraft.getInstance().getBlockRenderer();
        g.enableScissor(x, y, x + w, y + h);
        PoseStack ps = g.pose();
        ps.pushPose();
        ps.translate(x + w / 2f, y + h / 2f, 160);
        ps.scale(scale, -scale, scale);
        ps.mulPose(Axis.XP.rotationDegrees(pitch));
        ps.mulPose(Axis.YP.rotationDegrees(yaw));
        ps.translate(-pattern.width / 2f, -pattern.height / 2f, -pattern.depth / 2f);
        for (Placed p : blocks) {
            if (maxLayer >= 0 && p.y > maxLayer) continue;
            ps.pushPose();
            ps.translate(p.x, p.y, p.z);
            try {
                br.renderSingleBlock(p.state, ps, g.bufferSource(), LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
            } catch (Exception ignored) {
                // a block whose model cannot draw outside a world; leave the gap
            }
            ps.popPose();
        }
        g.flush();
        ps.popPose();
        g.disableScissor();
    }
}
