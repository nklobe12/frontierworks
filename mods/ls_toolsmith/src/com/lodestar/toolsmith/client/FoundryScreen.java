package com.lodestar.toolsmith.client;

import com.lodestar.core.client.GuiDraw;
import com.lodestar.toolsmith.foundry.FoundryControllerBE;
import com.lodestar.toolsmith.foundry.FoundryMenu;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraftforge.fluids.FluidStack;

import java.util.ArrayList;
import java.util.List;

/** Foundry screen: melting slots with progress bars on the left, the stacked pool of molten fluids on the right. */
public class FoundryScreen extends AbstractContainerScreen<FoundryMenu> {
    private static final int GX = 122, GY = 18, GW = 46, GH = 106;

    public FoundryScreen(FoundryMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        imageWidth = 176;
        imageHeight = 222;
        inventoryLabelY = FoundryMenu.INV_Y - 11;
    }

    @Override
    protected void renderBg(GuiGraphics g, float pt, int mx, int my) {
        GuiDraw.panel(g, leftPos, topPos, imageWidth, imageHeight);
        FoundryControllerBE be = menu.be;
        for (int i = 0; i < FoundryControllerBE.SLOTS; i++) {
            Slot s = menu.slots.get(i);
            int x = leftPos + s.x, y = topPos + s.y;
            if (i < be.usableSlots) {
                GuiDraw.slot(g, x, y);
                int need = be.needed[i];
                if (s.hasItem() && need > 0) {
                    int w = Math.min(16, be.progress[i] * 16 / need);
                    g.fill(x, y + 15, x + w, y + 16, 0xFFFF7A1A);
                }
            } else {
                g.fill(x - 1, y - 1, x + 17, y + 17, 0xFF9A9A9A);
            }
        }
        // fluid pool
        int x = leftPos + GX, y = topPos + GY;
        GuiDraw.inset(g, x, y, GW, GH);
        int cap = Math.max(1, be.capacity);
        int bottom = y + GH;
        for (FluidStack f : be.fluids) {
            int h = Math.max(1, (int) ((long) GH * f.getAmount() / cap));
            GuiDraw.fluid(g, x, bottom - h, GW, h, f.getFluid(), 1, 1);
            bottom -= h;
            if (bottom <= y) break;
        }
        if (!be.formed) {
            Component c = Component.translatable("gui.ls_toolsmith.foundry.incomplete");
            g.drawWordWrap(font, c, x + 2, y + 30, GW - 4, 0xFFFF5555);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mx, int my) {
        g.drawString(font, title, 8, 6, GuiDraw.TEXT, false);
        g.drawString(font, playerInventoryTitle, 8, inventoryLabelY, GuiDraw.TEXT, false);
    }

    private int fluidAt(double mx, double my) {
        FoundryControllerBE be = menu.be;
        int x = leftPos + GX, y = topPos + GY;
        if (mx < x || mx >= x + GW || my < y || my >= y + GH) return -1;
        int cap = Math.max(1, be.capacity);
        int bottom = y + GH;
        for (int i = 0; i < be.fluids.size(); i++) {
            int h = Math.max(1, (int) ((long) GH * be.fluids.get(i).getAmount() / cap));
            if (my >= bottom - h && my < bottom) return i;
            bottom -= h;
        }
        return -2;
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        int f = fluidAt(mx, my);
        if (f > 0 && minecraft != null && minecraft.gameMode != null) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, f);
            return true;
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        renderBackground(g);
        super.render(g, mx, my, pt);
        renderTooltip(g, mx, my);
        int f = fluidAt(mx, my);
        FoundryControllerBE be = menu.be;
        if (f != -1) {
            List<Component> tip = new ArrayList<>();
            if (f >= 0) {
                FluidStack fs = be.fluids.get(f);
                tip.add(fs.getDisplayName());
                tip.add(Component.literal(fs.getAmount() + " mB (" + fs.getAmount() / 144 + " ingots)").withStyle(ChatFormatting.GRAY));
                if (f > 0) tip.add(Component.translatable("gui.ls_toolsmith.foundry.select").withStyle(ChatFormatting.DARK_GRAY));
                else tip.add(Component.translatable("gui.ls_toolsmith.foundry.selected").withStyle(ChatFormatting.DARK_GRAY));
            }
            int used = 0;
            for (FluidStack s : be.fluids) used += s.getAmount();
            tip.add(Component.translatable("gui.ls_toolsmith.foundry.capacity", used, be.capacity).withStyle(ChatFormatting.GRAY));
            g.renderComponentTooltip(font, tip, mx, my);
        }
    }
}
