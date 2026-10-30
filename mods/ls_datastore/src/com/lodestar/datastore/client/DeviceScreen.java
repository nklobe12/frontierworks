package com.lodestar.datastore.client;

import com.lodestar.core.client.GuiDraw;
import com.lodestar.datastore.FabricatorBE;
import com.lodestar.datastore.menu.DeviceMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

/** Screen for drive bays, fabricators and ports. */
public class DeviceScreen extends AbstractContainerScreen<DeviceMenu> {
    public DeviceScreen(DeviceMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        imageWidth = 176;
        imageHeight = 166;
        inventoryLabelY = 73;
    }

    // priority control (drive bays and links)
    private int px() {
        return leftPos + (menu.kind == DeviceMenu.Kind.DRIVE ? 128 : 104);
    }

    private int py() {
        return topPos + (menu.kind == DeviceMenu.Kind.DRIVE ? 6 : 50);
    }

    private int mx0() {
        return leftPos + 104;
    }

    private int my0() {
        return topPos + 20;
    }

    @Override
    protected void renderBg(GuiGraphics g, float pt, int mx, int my) {
        GuiDraw.panel(g, leftPos, topPos, imageWidth, imageHeight);
        for (Slot s : menu.slots) GuiDraw.slot(g, leftPos + s.x, topPos + s.y);
        if (menu.kind == DeviceMenu.Kind.FABRICATOR && menu.be instanceof FabricatorBE f) {
            int left = menu.data(DeviceMenu.D_LEFT), total = menu.data(DeviceMenu.D_TOTAL);
            float prog = total <= 0 ? 0 : 1f - (float) left / total;
            GuiDraw.arrow(g, leftPos + 88, topPos + 35, prog);
            GuiDraw.bigSlot(g, leftPos + 120, topPos + 35);
            if (!f.making.isEmpty()) g.renderItem(f.making, leftPos + 120, topPos + 35);
        }
        if (menu.kind == DeviceMenu.Kind.PORT) {
            boolean hov = GuiDraw.in(mx, my, mx0(), my0(), 38, 14);
            GuiDraw.button(g, mx0(), my0(), 38, 14, hov, true);
            Component label = Component.translatable(menu.allow() ? "gui.ls_datastore.allow" : "gui.ls_datastore.deny");
            g.drawString(font, label, mx0() + 19 - font.width(label) / 2, my0() + 3, menu.allow() ? 0xFF1E6B2E : 0xFF8B1E1E, false);
        }
        if (menu.showsPriority()) {
            GuiDraw.button(g, px(), py(), 12, 12, GuiDraw.in(mx, my, px(), py(), 12, 12), true);
            GuiDraw.button(g, px() + 30, py(), 12, 12, GuiDraw.in(mx, my, px() + 30, py(), 12, 12), true);
            g.drawString(font, "-", px() + 4, py() + 2, GuiDraw.TEXT, false);
            g.drawString(font, "+", px() + 33, py() + 2, GuiDraw.TEXT, false);
            String p = Integer.toString(menu.priority());
            g.drawString(font, p, px() + 21 - font.width(p) / 2, py() + 2, 0xFF2060A0, false);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mx, int my) {
        g.drawString(font, font.plainSubstrByWidth(title.getString(), menu.kind == DeviceMenu.Kind.DRIVE ? 118 : 160), 8, 6, GuiDraw.TEXT, false);
        g.drawString(font, playerInventoryTitle, 8, inventoryLabelY, GuiDraw.TEXT, false);
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        renderBackground(g);
        super.render(g, mx, my, pt);
        if (menu.showsPriority() && GuiDraw.in(mx, my, px(), py(), 42, 12)) {
            g.renderComponentTooltip(font, java.util.List.of(Component.translatable("gui.ls_datastore.priority", menu.priority()),
                    Component.translatable("gui.ls_datastore.priority.tip")), mx, my);
        } else if (menu.kind == DeviceMenu.Kind.PORT && GuiDraw.in(mx, my, mx0(), my0(), 38, 14)) {
            g.renderTooltip(font, Component.translatable("gui.ls_datastore.filter_mode"), mx, my);
        } else if (menu.kind == DeviceMenu.Kind.FABRICATOR && menu.be instanceof FabricatorBE f && GuiDraw.in(mx, my, leftPos + 116, topPos + 31, 24, 24)) {
            g.renderTooltip(font, f.making.isEmpty() ? Component.translatable("gui.ls_datastore.idle")
                    : Component.translatable("gui.ls_datastore.working", f.making.getHoverName()), mx, my);
        } else {
            renderTooltip(g, mx, my);
        }
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        int x = (int) mx, y = (int) my;
        if (menu.showsPriority()) {
            if (GuiDraw.in(x, y, px(), py(), 12, 12)) return click(0);
            if (GuiDraw.in(x, y, px() + 30, py(), 12, 12)) return click(1);
        }
        if (menu.kind == DeviceMenu.Kind.PORT && GuiDraw.in(x, y, mx0(), my0(), 38, 14)) return click(2);
        return super.mouseClicked(mx, my, button);
    }

    private boolean click(int id) {
        minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
        return true;
    }
}
