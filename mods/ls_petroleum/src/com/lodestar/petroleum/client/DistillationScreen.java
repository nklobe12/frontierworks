package com.lodestar.petroleum.client;

import com.lodestar.core.client.MachineScreen;
import com.lodestar.core.machine.MachineLayout;
import com.lodestar.core.machine.MachineMenu;
import com.lodestar.petroleum.DistillationBE;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Machine screen for the distillation column: marks each tray's tank with its fraction colour and greys out missing trays. */
public class DistillationScreen extends MachineScreen {
    private static final int[] BAND = {0xFF5A3A1C, 0xFFC8901E, 0xFF7FB2CC, 0xFFE3D36E, 0xFFC9D6B4};

    public DistillationScreen(MachineMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
    }

    @Override
    protected void drawExtraBg(GuiGraphics g, int x, int y, int mouseX, int mouseY) {
        int trays = menu.extra(0);
        for (int i = 0; i < DistillationBE.MAX_TRAYS; i++) {
            MachineLayout.TankDef td = layout.tanks.get(1 + i);
            int tx = x + td.x, ty = y + td.y;
            g.fill(tx, ty + td.h + 1, tx + td.w, ty + td.h + 3, BAND[i]);
            if (i >= trays) g.fill(tx, ty, tx + td.w, ty + td.h, 0x90404040);
        }
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        int rx = mouseX - leftPos, ry = mouseY - topPos;
        for (int i = 0; i < DistillationBE.MAX_TRAYS; i++) {
            MachineLayout.TankDef td = layout.tanks.get(1 + i);
            if (rx >= td.x && rx < td.x + td.w && ry >= td.y + td.h + 1 && ry < td.y + td.h + 4) {
                g.renderTooltip(font, Component.translatable("gui.ls_petroleum.tray" + (i + 1)), mouseX, mouseY);
            }
        }
    }
}
