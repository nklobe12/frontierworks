package com.frontierworks.spacesuit.client;

import com.frontierworks.spacesuit.OxygenEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.IEventBus;

public final class OxygenHud {
    private OxygenHud() {}

    public static void init(IEventBus bus) {
        bus.addListener((RegisterGuiOverlaysEvent e) -> e.registerAbove(VanillaGuiOverlay.AIR_LEVEL.id(), "oxygen", OxygenHud::render));
    }

    private static void render(ForgeGui gui, GuiGraphics g, float partialTick, int width, int height) {
        Minecraft mc = Minecraft.getInstance();
        Player p = mc.player;
        if (p == null || mc.options.hideGui || !OxygenEvents.airless(p)) return;
        ItemStack chest = p.getItemBySlot(EquipmentSlot.CHEST);
        int o = OxygenEvents.isSuitChest(chest) && chest.hasTag() ? chest.getTag().getInt("Oxygen") : 0;
        int pct = (int) Math.round(100.0 * o / OxygenEvents.SUIT_MAX);
        String text = I18n.get("hud.fw_spacesuit.oxygen", pct);
        int color = pct > 25 ? 0x7fd8ff : pct > 5 ? 0xffc040 : 0xff4040;
        int x = width / 2 + 10;
        int y = height - 49 - (p.getArmorValue() > 0 ? 10 : 0);
        g.drawString(mc.font, text, x, y, color, true);
        int bar = (int) (80 * Math.min(1.0, o / (double) OxygenEvents.SUIT_MAX));
        g.fill(x + 40, y + 2, x + 120, y + 6, 0xff202028);
        g.fill(x + 40, y + 2, x + 40 + bar, y + 6, 0xff000000 | color);
    }
}
