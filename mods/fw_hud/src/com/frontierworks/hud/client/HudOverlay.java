package com.frontierworks.hud.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.biome.Biome;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/** Explorer's HUD: coordinates, facing, biome, day/time and light level. Toggle with F7 (cycles position). */
public final class HudOverlay {
    public static final KeyMapping TOGGLE = new KeyMapping("key.fw_hud.toggle", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_F7, "key.categories.frontierworks");
    private static int mode = 1; // 0 off, 1 top-left, 2 top-right

    private HudOverlay() {}

    public static void init(IEventBus bus) {
        bus.addListener(HudOverlay::keys);
        bus.addListener(HudOverlay::overlays);
        MinecraftForge.EVENT_BUS.addListener(HudOverlay::tick);
    }

    private static void keys(RegisterKeyMappingsEvent e) {
        e.register(TOGGLE);
    }

    private static void overlays(RegisterGuiOverlaysEvent e) {
        e.registerAbove(VanillaGuiOverlay.DEBUG_TEXT.id(), "explorer_hud", HudOverlay::render);
    }

    private static void tick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        while (TOGGLE.consumeClick()) mode = (mode + 1) % 3;
    }

    private static void render(ForgeGui gui, GuiGraphics g, float partialTick, int width, int height) {
        Minecraft mc = Minecraft.getInstance();
        Player p = mc.player;
        if (mode == 0 || p == null || mc.level == null || mc.options.renderDebug || mc.options.hideGui) return;
        BlockPos pos = p.blockPosition();
        List<String> lines = new ArrayList<>();
        lines.add(String.format("XYZ %d / %d / %d", pos.getX(), pos.getY(), pos.getZ()));
        Direction d = p.getDirection();
        lines.add("Facing " + d.getName() + (d.getAxis() == Direction.Axis.X ? (d.getStepX() > 0 ? " (+X)" : " (-X)") : (d.getStepZ() > 0 ? " (+Z)" : " (-Z)")));
        Holder<Biome> biome = mc.level.getBiome(pos);
        String biomeName = biome.unwrapKey().map(k -> {
            ResourceLocation id = k.location();
            String key = "biome." + id.getNamespace() + "." + id.getPath();
            return net.minecraft.client.resources.language.I18n.exists(key) ? net.minecraft.client.resources.language.I18n.get(key) : id.getPath();
        }).orElse("?");
        lines.add("Biome " + biomeName);
        long time = mc.level.getDayTime();
        long day = time / 24000L + 1;
        int hours = (int) ((time / 1000 + 6) % 24);
        int minutes = (int) ((time % 1000) * 60 / 1000);
        lines.add(String.format("Day %d  %02d:%02d", day, hours, minutes));
        int light = mc.level.getBrightness(LightLayer.BLOCK, pos);
        lines.add("Light " + light + (light < 8 ? "  (mobs spawn)" : ""));
        int x = mode == 1 ? 4 : width - 4;
        int y = 4;
        for (String s : lines) {
            int w = mc.font.width(s);
            int lx = mode == 1 ? x : x - w;
            g.fill(lx - 2, y - 1, lx + w + 2, y + 9, 0x80000000);
            g.drawString(mc.font, s, lx, y, 0xFFE0E0E0, false);
            y += 10;
        }
    }
}
