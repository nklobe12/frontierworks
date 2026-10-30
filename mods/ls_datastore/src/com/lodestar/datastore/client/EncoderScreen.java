package com.lodestar.datastore.client;

import com.lodestar.core.client.GuiDraw;
import com.lodestar.datastore.craft.GridContainer;
import com.lodestar.datastore.menu.EncoderMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeType;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Pattern encoder: ghost recipe grid, live output preview, substitutes switch, Encode and clear buttons. */
public class EncoderScreen extends AbstractContainerScreen<EncoderMenu> {
    private static final int EX = 98, EY = 58, SX = 96, SY = 17;

    public EncoderScreen(EncoderMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        imageWidth = 176;
        imageHeight = 166;
        inventoryLabelY = 73;
    }

    private ItemStack preview() {
        List<ItemStack> grid = new ArrayList<>();
        for (int i = 0; i < 9; i++) grid.add(menu.slots.get(i).getItem());
        GridContainer c = new GridContainer(grid);
        if (c.isEmpty()) return ItemStack.EMPTY;
        Optional<CraftingRecipe> r = minecraft.level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, c, minecraft.level);
        return r.map(rc -> rc.assemble(c, minecraft.level.registryAccess())).orElse(ItemStack.EMPTY);
    }

    @Override
    protected void renderBg(GuiGraphics g, float pt, int mx, int my) {
        GuiDraw.panel(g, leftPos, topPos, imageWidth, imageHeight);
        for (Slot s : menu.slots) GuiDraw.slot(g, leftPos + s.x, topPos + s.y);
        GuiDraw.arrow(g, leftPos + 90, topPos + 35, 0);
        GuiDraw.bigSlot(g, leftPos + 122, topPos + 35);
        ItemStack out = preview();
        if (!out.isEmpty()) g.renderItem(out, leftPos + 122, topPos + 35);
        g.renderItemDecorations(font, out, leftPos + 122, topPos + 35);
        boolean hovE = GuiDraw.in(mx, my, leftPos + EX, topPos + EY, 44, 14);
        GuiDraw.button(g, leftPos + EX, topPos + EY, 44, 14, hovE, !out.isEmpty() && menu.slots.get(EncoderMenu.CARD_IN).hasItem());
        Component enc = Component.translatable("gui.ls_datastore.encode");
        g.drawString(font, enc, leftPos + EX + 22 - font.width(enc) / 2, topPos + EY + 3, GuiDraw.TEXT, false);
        boolean hovS = GuiDraw.in(mx, my, leftPos + SX, topPos + SY, 50, 12);
        GuiDraw.button(g, leftPos + SX, topPos + SY, 50, 12, hovS, true);
        Component sub = Component.translatable("gui.ls_datastore.substitute",
                Component.translatable(menu.substitute() ? "gui.ls_datastore.on" : "gui.ls_datastore.off"));
        g.pose().pushPose();
        g.pose().translate(leftPos + SX + 25, topPos + SY + 3, 0);
        g.pose().scale(0.75f, 0.75f, 1f);
        g.drawString(font, sub, -font.width(sub) / 2, 0, menu.substitute() ? 0xFF1E6B2E : 0xFF6B1E1E, false);
        g.pose().popPose();
        boolean hovC = GuiDraw.in(mx, my, leftPos + 8, topPos + 17, 14, 14);
        GuiDraw.button(g, leftPos + 8, topPos + 17, 14, 14, hovC, true);
        g.drawString(font, "x", leftPos + 12, topPos + 20, 0xFF802020, false);
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
        if (GuiDraw.in(mx, my, leftPos + SX, topPos + SY, 50, 12)) {
            g.renderTooltip(font, Component.translatable("gui.ls_datastore.substitute.tip"), mx, my);
        } else if (GuiDraw.in(mx, my, leftPos + 118, topPos + 31, 24, 24)) {
            ItemStack out = preview();
            if (out.isEmpty()) g.renderTooltip(font, Component.translatable("gui.ls_datastore.no_recipe"), mx, my);
            else g.renderTooltip(font, out, mx, my);
        } else if (GuiDraw.in(mx, my, leftPos + 8, topPos + 17, 14, 14)) {
            g.renderTooltip(font, Component.translatable("gui.ls_datastore.clear"), mx, my);
        } else {
            renderTooltip(g, mx, my);
        }
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        int x = (int) mx, y = (int) my;
        if (GuiDraw.in(x, y, leftPos + EX, topPos + EY, 44, 14)) return click(0);
        if (GuiDraw.in(x, y, leftPos + SX, topPos + SY, 50, 12)) return click(1);
        if (GuiDraw.in(x, y, leftPos + 8, topPos + 17, 14, 14)) return click(2);
        return super.mouseClicked(mx, my, button);
    }

    private boolean click(int id) {
        minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
        return true;
    }
}
