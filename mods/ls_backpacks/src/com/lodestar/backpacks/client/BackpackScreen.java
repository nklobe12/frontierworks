package com.lodestar.backpacks.client;

import com.lodestar.backpacks.BackpackMenu;
import com.lodestar.backpacks.Contents;
import com.lodestar.backpacks.Upgrades;
import com.lodestar.core.client.ExtraAreas;
import com.lodestar.core.client.GuiDraw;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.fluids.FluidStack;

import java.util.List;

/** Storage grid plus a side panel with upgrade slots and the crafting / smelting / tank page. */
public class BackpackScreen extends AbstractContainerScreen<BackpackMenu> implements ExtraAreas {
    private static final int SIDE_W = 100;

    public BackpackScreen(BackpackMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        imageWidth = 176;
        imageHeight = 18 + menu.tier.rows * 18 + 14 + 76 + 6;
        inventoryLabelY = 18 + menu.tier.rows * 18 + 3;
    }

    @Override
    public List<Rect2i> extraAreas() {
        return List.of(new Rect2i(leftPos + imageWidth, topPos, SIDE_W + 4, sideHeight()));
    }

    private int sideHeight() {
        return Math.max(20 + menu.tier.upgrades * 18 + 6, menu.page == BackpackMenu.PAGE_NONE ? 0 : 108);
    }

    @Override
    protected void renderBg(GuiGraphics g, float pt, int mx, int my) {
        GuiDraw.panel(g, leftPos, topPos, imageWidth, imageHeight);
        int sx = leftPos + BackpackMenu.SIDE_X - 4;
        GuiDraw.panel(g, sx, topPos, SIDE_W, sideHeight());
        for (Slot s : menu.slots) if (s.isActive()) GuiDraw.slot(g, leftPos + s.x, topPos + s.y);
        // page tabs
        int[] pages = {BackpackMenu.PAGE_CRAFT, BackpackMenu.PAGE_SMELT, BackpackMenu.PAGE_TANK};
        String[] labels = {"C", "S", "T"};
        for (int i = 0; i < 3; i++) {
            if (!menu.pageAvailable(pages[i])) continue;
            int bx = leftPos + BackpackMenu.PAGE_X + i * 16, by = topPos + 4;
            GuiDraw.button(g, bx, by, 14, 11, GuiDraw.in(mx, my, bx, by, 14, 11) || menu.page == pages[i], true);
            g.drawString(font, labels[i], bx + 4, by + 2, menu.page == pages[i] ? 0xFF1F5FBF : GuiDraw.TEXT, false);
        }
        int px = leftPos + BackpackMenu.PAGE_X, py = topPos;
        if (menu.page == BackpackMenu.PAGE_CRAFT) {
            GuiDraw.arrow(g, px + 16, py + 74, 0);
        } else if (menu.page == BackpackMenu.PAGE_SMELT) {
            float burn = menu.burnMax() <= 0 ? 0 : (float) menu.burn() / menu.burnMax();
            GuiDraw.flame(g, px + 1, py + 37, burn);
            GuiDraw.arrow(g, px + 20, py + 36, (float) menu.cook() / Upgrades.COOK_TIME);
        } else if (menu.page == BackpackMenu.PAGE_TANK) {
            Fluid f = menu.tankFluid();
            GuiDraw.fluid(g, px, py + 18, 18, 64, f, menu.tankAmount(), Contents.TANK);
            g.drawString(font, "↓", px + 30, py + 40, GuiDraw.TEXT, false);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mx, int my) {
        g.drawString(font, title, 8, 6, GuiDraw.TEXT, false);
        g.drawString(font, playerInventoryTitle, 8, inventoryLabelY, GuiDraw.TEXT, false);
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        renderBackground(g);
        super.render(g, mx, my, pt);
        renderTooltip(g, mx, my);
        int px = leftPos + BackpackMenu.PAGE_X, py = topPos;
        if (menu.page == BackpackMenu.PAGE_TANK && GuiDraw.in(mx, my, px, py + 18, 18, 64)) {
            Fluid f = menu.tankFluid();
            Component name = f == null ? Component.translatable("gui.ls_backpacks.empty") : new FluidStack(f, 1).getDisplayName();
            g.renderComponentTooltip(font, List.of(name, Component.literal(String.format("%,d / %,d mB", menu.tankAmount(), Contents.TANK))), mx, my);
        }
        if (hoveredSlot == null && menu.tier.upgrades > 0 && GuiDraw.in(mx, my, leftPos + BackpackMenu.SIDE_X + 4, topPos + 18, 16, menu.tier.upgrades * 18)) {
            g.renderTooltip(font, Component.translatable("gui.ls_backpacks.upgrade_slot"), mx, my);
        }
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        int[] pages = {BackpackMenu.PAGE_CRAFT, BackpackMenu.PAGE_SMELT, BackpackMenu.PAGE_TANK};
        for (int i = 0; i < 3; i++) {
            if (!menu.pageAvailable(pages[i])) continue;
            int bx = leftPos + BackpackMenu.PAGE_X + i * 16, by = topPos + 4;
            if (GuiDraw.in((int) mx, (int) my, bx, by, 14, 11)) {
                menu.page = pages[i];
                Minecraft.getInstance().gameMode.handleInventoryButtonClick(menu.containerId, pages[i]);
                return true;
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        if (menu.page != BackpackMenu.PAGE_NONE && !menu.pageAvailable(menu.page)) menu.page = menu.firstPage();
        if (menu.page == BackpackMenu.PAGE_NONE) menu.page = menu.firstPage();
    }
}
