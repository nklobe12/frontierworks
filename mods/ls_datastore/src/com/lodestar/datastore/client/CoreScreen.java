package com.lodestar.datastore.client;

import com.lodestar.core.client.GuiDraw;
import com.lodestar.datastore.CoreBE;
import com.lodestar.datastore.DataGrid;
import com.lodestar.datastore.menu.CoreMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Network status: energy, upkeep, devices, disk space and crafting jobs. */
public class CoreScreen extends AbstractContainerScreen<CoreMenu> {
    public CoreScreen(CoreMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        imageWidth = 176;
        imageHeight = 124;
    }

    @Override
    protected void renderBg(GuiGraphics g, float pt, int mx, int my) {
        GuiDraw.panel(g, leftPos, topPos, imageWidth, imageHeight);
        if (!(minecraft.level.getBlockEntity(menu.pos) instanceof CoreBE c)) return;
        GuiDraw.energyBar(g, leftPos + 10, topPos + 20, 14, 94, c.cEnergy, CoreBE.CAPACITY);
        int x = leftPos + 32, y = topPos + 20;
        int col = c.cStatus == DataGrid.ONLINE ? 0xFF1E7B3E : 0xFF9B2020;
        g.drawString(font, Component.translatable("gui.ls_datastore.status." + c.cStatus), x, y, col, false);
        g.drawString(font, Component.translatable("gui.ls_datastore.energy", String.format("%,d", c.cEnergy), String.format("%,d", CoreBE.CAPACITY)), x, y + 14, GuiDraw.TEXT, false);
        g.drawString(font, Component.translatable("gui.ls_datastore.upkeep", c.cUpkeep), x, y + 26, GuiDraw.TEXT, false);
        g.drawString(font, Component.translatable("gui.ls_datastore.devices", c.cDevices), x, y + 38, GuiDraw.TEXT, false);
        g.drawString(font, Component.translatable("gui.ls_datastore.disks", c.cDisks), x + 72, y + 38, GuiDraw.TEXT, false);
        g.drawString(font, Component.translatable("gui.ls_datastore.bytes", TermClient.fmt(c.cBytesUsed), TermClient.fmt(c.cBytesTotal)), x, y + 52, GuiDraw.TEXT, false);
        GuiDraw.hbar(g, x, y + 62, 130, 5, c.cBytesTotal <= 0 ? 0 : (float) c.cBytesUsed / c.cBytesTotal, 0xFF3FD0C9);
        g.drawString(font, Component.translatable("gui.ls_datastore.types", c.cTypesUsed, c.cTypesTotal), x, y + 72, GuiDraw.TEXT, false);
        g.drawString(font, Component.translatable("gui.ls_datastore.jobs", c.cJobs), x, y + 84, GuiDraw.TEXT, false);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mx, int my) {
        g.drawString(font, title, 8, 6, GuiDraw.TEXT, false);
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        renderBackground(g);
        super.render(g, mx, my, pt);
        if (minecraft.level.getBlockEntity(menu.pos) instanceof CoreBE c && GuiDraw.in(mx, my, leftPos + 10, topPos + 20, 14, 94)) {
            g.renderTooltip(font, Component.literal(String.format("%,d / %,d FE", c.cEnergy, CoreBE.CAPACITY)), mx, my);
        }
        renderTooltip(g, mx, my);
    }
}
