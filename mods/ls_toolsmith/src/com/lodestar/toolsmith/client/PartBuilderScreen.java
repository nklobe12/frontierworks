package com.lodestar.toolsmith.client;

import com.lodestar.core.client.GuiDraw;
import com.lodestar.toolsmith.Materials;
import com.lodestar.toolsmith.PartItem;
import com.lodestar.toolsmith.PartType;
import com.lodestar.toolsmith.ToolMaterial;
import com.lodestar.toolsmith.station.PartBuilderMenu;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/** Part builder: a grid of part shapes to choose from, the pattern and material slots, and the result. */
public class PartBuilderScreen extends AbstractContainerScreen<PartBuilderMenu> {
    private static final int GX = 8, GY = 18, COLS = 5;

    public PartBuilderScreen(PartBuilderMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
    }

    private ToolMaterial shownMaterial() {
        ToolMaterial m = PartBuilderMenu.materialOf(menu.slots.get(1).getItem());
        if (m != null) return m;
        ToolMaterial wood = Materials.get("wood");
        return wood != null ? wood : Materials.FALLBACK;
    }

    @Override
    protected void renderBg(GuiGraphics g, float pt, int mx, int my) {
        GuiDraw.panel(g, leftPos, topPos, imageWidth, imageHeight);
        ToolMaterial m = shownMaterial();
        PartType[] parts = PartType.values();
        for (int i = 0; i < parts.length; i++) {
            int x = leftPos + GX + (i % COLS) * 19, y = topPos + GY + (i / COLS) * 19;
            boolean sel = i == menu.selected();
            boolean hover = mx >= x && mx < x + 18 && my >= y && my < y + 18;
            GuiDraw.button(g, x, y, 18, 18, hover, true);
            if (sel) g.fill(x + 1, y + 1, x + 17, y + 17, 0xFF7FB2FF);
            g.renderItem(PartItem.of(parts[i], m), x + 1, y + 1);
        }
        GuiDraw.slot(g, leftPos + 112, topPos + 18);
        GuiDraw.slot(g, leftPos + 112, topPos + 50);
        GuiDraw.bigSlot(g, leftPos + 150, topPos + 34);
        GuiDraw.arrow(g, leftPos + 120, topPos + 34, 0);
        if (menu.slots.get(0).getItem().isEmpty()) {
            g.fill(leftPos + 114, topPos + 20, leftPos + 126, topPos + 32, 0x40FFFFFF);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mx, int my) {
        g.drawString(font, title, 8, 6, GuiDraw.TEXT, false);
        g.drawString(font, playerInventoryTitle, 8, inventoryLabelY, GuiDraw.TEXT, false);
        PartType p = PartType.values()[menu.selected()];
        g.drawString(font, Component.translatable("gui.ls_toolsmith.cost", p.cost), 104, 70, 0xFF606060, false);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        PartType[] parts = PartType.values();
        for (int i = 0; i < parts.length; i++) {
            int x = leftPos + GX + (i % COLS) * 19, y = topPos + GY + (i / COLS) * 19;
            if (mx >= x && mx < x + 18 && my >= y && my < y + 18 && minecraft != null && minecraft.gameMode != null) {
                minecraft.gameMode.handleInventoryButtonClick(menu.containerId, i);
                return true;
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        renderBackground(g);
        super.render(g, mx, my, pt);
        renderTooltip(g, mx, my);
        PartType[] parts = PartType.values();
        for (int i = 0; i < parts.length; i++) {
            int x = leftPos + GX + (i % COLS) * 19, y = topPos + GY + (i / COLS) * 19;
            if (mx >= x && mx < x + 18 && my >= y && my < y + 18) {
                List<Component> tip = new ArrayList<>();
                tip.add(parts[i].displayName());
                tip.add(Component.translatable("gui.ls_toolsmith.cost", parts[i].cost).withStyle(ChatFormatting.GRAY));
                g.renderComponentTooltip(font, tip, mx, my);
            }
        }
        if (mx >= leftPos + 112 && mx < leftPos + 128 && my >= topPos + 18 && my < topPos + 34 && menu.slots.get(0).getItem().isEmpty()) {
            g.renderTooltip(font, Component.translatable("gui.ls_toolsmith.needs_pattern"), mx, my);
        }
    }
}
