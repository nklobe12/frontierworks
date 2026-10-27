package com.lodestar.toolsmith.client;

import com.lodestar.core.client.GuiDraw;
import com.lodestar.toolsmith.Materials;
import com.lodestar.toolsmith.PartItem;
import com.lodestar.toolsmith.PartType;
import com.lodestar.toolsmith.ToolMaterial;
import com.lodestar.toolsmith.ToolType;
import com.lodestar.toolsmith.station.ToolStationMenu;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import java.util.ArrayList;
import java.util.List;

/**
 * Tool station / forge screen: five slots in a cross, the result, a strip of tool recipes to hover for the parts
 * they need, and any reason the current combination doesn't work.
 */
public class ToolStationScreen extends AbstractContainerScreen<ToolStationMenu> {
    public ToolStationScreen(ToolStationMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
    }

    private List<ToolType> shownTools() {
        List<ToolType> l = new ArrayList<>();
        for (ToolType t : ToolType.values()) if (!t.large || menu.isForge()) l.add(t);
        return l;
    }

    @Override
    protected void renderBg(GuiGraphics g, float pt, int mx, int my) {
        GuiDraw.panel(g, leftPos, topPos, imageWidth, imageHeight);
        for (int[] p : ToolStationMenu.POS) GuiDraw.slot(g, leftPos + p[0], topPos + p[1]);
        GuiDraw.arrow(g, leftPos + 98, topPos + 34, 0);
        GuiDraw.bigSlot(g, leftPos + 134, topPos + 35);
        // tool recipe strip on the right edge
        List<ToolType> tools = shownTools();
        ToolMaterial iron = Materials.get("iron");
        for (int i = 0; i < tools.size(); i++) {
            int x = leftPos + imageWidth + 2, y = topPos + 4 + i * 18;
            GuiDraw.button(g, x, y, 18, 18, false, true);
            g.renderItem(PartItem.of(tools.get(i).parts.get(0), iron == null ? Materials.FALLBACK : iron), x + 1, y + 1);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mx, int my) {
        g.drawString(font, title, 8, 6, GuiDraw.TEXT, false);
        g.drawString(font, playerInventoryTitle, 8, inventoryLabelY, GuiDraw.TEXT, false);
        if (menu.error != null) {
            GuiDraw.wrapped(g, font, menu.error.copy().withStyle(ChatFormatting.DARK_RED), 96, 56, 76, 0xFFAA2222);
        }
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        renderBackground(g);
        super.render(g, mx, my, pt);
        renderTooltip(g, mx, my);
        List<ToolType> tools = shownTools();
        for (int i = 0; i < tools.size(); i++) {
            int x = leftPos + imageWidth + 2, y = topPos + 4 + i * 18;
            if (mx >= x && mx < x + 18 && my >= y && my < y + 18) {
                List<Component> tip = new ArrayList<>();
                tip.add(Component.translatable("tool.ls_toolsmith." + tools.get(i).id).withStyle(ChatFormatting.WHITE));
                for (PartType p : tools.get(i).parts) tip.add(Component.literal("  ").append(p.displayName()).withStyle(ChatFormatting.GRAY));
                g.renderComponentTooltip(font, tip, mx, my);
            }
        }
    }
}
