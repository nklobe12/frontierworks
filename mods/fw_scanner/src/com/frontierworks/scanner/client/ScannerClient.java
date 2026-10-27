package com.frontierworks.scanner.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.Tags;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.ForgeRegistries;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Client side of the ore scanner: finds ores around the player and outlines them through walls. */
public final class ScannerClient {
    private static final List<long[]> HITS = new ArrayList<>(); // {pos, rgb}
    private static long expires;

    private ScannerClient() {}

    public static void init(IEventBus bus) {
        MinecraftForge.EVENT_BUS.addListener(ScannerClient::render);
    }

    public static void scan(int radius) {
        Minecraft mc = Minecraft.getInstance();
        Level level = mc.level;
        if (level == null || mc.player == null) return;
        HITS.clear();
        Map<String, Integer> counts = new HashMap<>();
        BlockPos c = mc.player.blockPosition();
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    p.set(c.getX() + dx, c.getY() + dy, c.getZ() + dz);
                    BlockState st = level.getBlockState(p);
                    if (st.is(Tags.Blocks.ORES)) {
                        int rgb = st.getMapColor(level, p).col;
                        HITS.add(new long[]{p.asLong(), rgb == 0 ? 0xFFFFFF : rgb});
                        ResourceLocation id = ForgeRegistries.BLOCKS.getKey(st.getBlock());
                        String name = st.getBlock().getName().getString();
                        counts.merge(name, 1, Integer::sum);
                        if (HITS.size() > 4000) break;
                    }
                }
            }
        }
        expires = level.getGameTime() + 300;
        if (counts.isEmpty()) {
            mc.player.displayClientMessage(Component.translatable("msg.fw_scanner.none"), true);
        } else {
            StringBuilder sb = new StringBuilder();
            counts.entrySet().stream().sorted((a, b) -> b.getValue() - a.getValue()).limit(4)
                    .forEach(e -> sb.append(e.getKey()).append(" x").append(e.getValue()).append("  "));
            mc.player.displayClientMessage(Component.literal(sb.toString().trim()), true);
        }
    }

    private static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS || HITS.isEmpty()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.level.getGameTime() > expires) {
            HITS.clear();
            return;
        }
        Vec3 cam = event.getCamera().getPosition();
        PoseStack pose = event.getPoseStack();
        pose.pushPose();
        pose.translate(-cam.x, -cam.y, -cam.z);
        Matrix4f m = pose.last().pose();
        RenderSystem.disableDepthTest();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        BufferBuilder bb = Tesselator.getInstance().getBuilder();
        bb.begin(VertexFormat.Mode.DEBUG_LINES, DefaultVertexFormat.POSITION_COLOR);
        for (long[] h : HITS) {
            BlockPos p = BlockPos.of(h[0]);
            int rgb = (int) h[1];
            float r = ((rgb >> 16) & 0xFF) / 255f, g = ((rgb >> 8) & 0xFF) / 255f, b = (rgb & 0xFF) / 255f;
            box(bb, m, p.getX() + 0.05f, p.getY() + 0.05f, p.getZ() + 0.05f, p.getX() + 0.95f, p.getY() + 0.95f, p.getZ() + 0.95f, r, g, b);
        }
        BufferUploader.drawWithShader(bb.end());
        RenderSystem.enableDepthTest();
        pose.popPose();
    }

    private static void line(BufferBuilder bb, Matrix4f m, float x1, float y1, float z1, float x2, float y2, float z2, float r, float g, float b) {
        bb.vertex(m, x1, y1, z1).color(r, g, b, 0.9f).endVertex();
        bb.vertex(m, x2, y2, z2).color(r, g, b, 0.9f).endVertex();
    }

    private static void box(BufferBuilder bb, Matrix4f m, float x0, float y0, float z0, float x1, float y1, float z1, float r, float g, float b) {
        line(bb, m, x0, y0, z0, x1, y0, z0, r, g, b);
        line(bb, m, x0, y0, z0, x0, y1, z0, r, g, b);
        line(bb, m, x0, y0, z0, x0, y0, z1, r, g, b);
        line(bb, m, x1, y1, z1, x0, y1, z1, r, g, b);
        line(bb, m, x1, y1, z1, x1, y0, z1, r, g, b);
        line(bb, m, x1, y1, z1, x1, y1, z0, r, g, b);
        line(bb, m, x1, y0, z0, x1, y1, z0, r, g, b);
        line(bb, m, x1, y0, z0, x1, y0, z1, r, g, b);
        line(bb, m, x0, y1, z0, x1, y1, z0, r, g, b);
        line(bb, m, x0, y1, z0, x0, y1, z1, r, g, b);
        line(bb, m, x0, y0, z1, x1, y0, z1, r, g, b);
        line(bb, m, x0, y0, z1, x0, y1, z1, r, g, b);
    }
}
