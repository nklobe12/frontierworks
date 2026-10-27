package com.lodestar.ducts.client;

import com.lodestar.core.client.GuiDraw;
import com.lodestar.ducts.FilterMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

/** Ghost-slot filter with a whitelist / blacklist switch. */
public class FilterScreen extends AbstractContainerScreen<FilterMenu> {
    public FilterScreen(FilterMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        imageWidth = 176;
        imageHeight = 166;
        inventoryLabelY = 73;
    }

    private int bx() {
        return leftPos + 126;
    }

    private int by() {
        return topPos + 34;
    }

    @Override
    protected void renderBg(GuiGraphics g, float pt, int mx, int my) {
        GuiDraw.panel(g, leftPos, topPos, imageWidth, imageHeight);
        for (Slot s : menu.slots) GuiDraw.slot(g, leftPos + s.x, topPos + s.y);
        boolean hov = GuiDraw.in(mx, my, bx(), by(), 40, 16);
        GuiDraw.button(g, bx(), by(), 40, 16, hov, true);
        Component label = Component.translatable(menu.whitelist() ? "gui.ls_ducts.whitelist" : "gui.ls_ducts.blacklist");
        g.drawString(font, label, bx() + 20 - font.width(label) / 2, by() + 4, menu.whitelist() ? 0xFF1E6B2E : 0xFF8B1E1E, false);
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        renderBackground(g);
        super.render(g, mx, my, pt);
        renderTooltip(g, mx, my);
        if (GuiDraw.in(mx, my, bx(), by(), 40, 16)) g.renderTooltip(font, Component.translatable("gui.ls_ducts.filter_mode"), mx, my);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (GuiDraw.in((int) mx, (int) my, bx(), by(), 40, 16)) {
            Minecraft.getInstance().gameMode.handleInventoryButtonClick(menu.containerId, 0);
            return true;
        }
        return super.mouseClicked(mx, my, button);
    }
}
