package com.lodestar.aeolus.client;

import com.lodestar.aeolus.Wind;
import com.lodestar.core.block.LSEntityBlock;
import com.lodestar.core.client.GuiDraw;
import com.lodestar.core.client.MachineScreen;
import com.lodestar.core.machine.MachineMenu;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;

/** Wind turbine screen: the generic machine screen plus a wind dial showing where the wind comes from against the rotor axis. */
public class TurbineScreen extends MachineScreen {
    private static final int DX = 26, DY = 28, R = 11;

    public TurbineScreen(MachineMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
    }

    private static void dot(GuiGraphics g, double x, double y, int color) {
        int ix = (int) Math.round(x), iy = (int) Math.round(y);
        g.fill(ix, iy, ix + 1, iy + 1, color);
    }

    @Override
    protected void drawExtraBg(GuiGraphics g, int x, int y, int mouseX, int mouseY) {
        int cx = x + DX, cy = y + DY;
        g.fill(cx - R - 1, cy - R - 1, cx + R + 2, cy + R + 2, 0xFF2B2B2B);
        for (int i = 0; i < 64; i++) {
            double a = i * Math.PI * 2 / 64;
            dot(g, cx + Math.sin(a) * R, cy - Math.cos(a) * R, 0xFF8A8A8A);
        }
        g.drawString(font, "N", cx - 2, cy - R + 1, 0xFF6E8BB0, false);
        // the turbine axis
        Direction f = LSEntityBlock.facingOf(menu.be.getBlockState());
        double ax = Wind.bearingOf(f);
        for (int t = -R + 3; t <= R - 3; t++) dot(g, cx + Math.sin(ax) * t, cy - Math.cos(ax) * t, 0xFF5A5A5A);
        // wind arrow, pointing the way it blows
        double b = Math.toRadians(menu.extra(6));
        double local = menu.extra(3) / 100.0;
        int col = local > 1.5 ? 0xFFE05050 : local > 0.9 ? 0xFFE0C050 : 0xFF70D0E0;
        for (int t = -R + 2; t <= R - 2; t++) dot(g, cx + Math.sin(b) * -t, cy - Math.cos(b) * -t, col);
        double hx = cx - Math.sin(b) * (R - 2), hy = cy + Math.cos(b) * (R - 2);
        for (int k = 1; k <= 3; k++) {
            dot(g, hx + Math.sin(b + 0.6) * k, hy - Math.cos(b + 0.6) * k, col);
            dot(g, hx + Math.sin(b - 0.6) * k, hy - Math.cos(b - 0.6) * k, col);
        }
    }

    @Override
    protected void addExtraTooltip(List<Component> tip, int rx, int ry) {
        if (GuiDraw.in(rx, ry, DX - R, DY - R, R * 2 + 1, R * 2 + 1)) {
            Direction f = LSEntityBlock.facingOf(menu.be.getBlockState());
            tip.add(Component.translatable("gui.ls_aeolus.dial", Wind.compassLong(Math.toRadians(menu.extra(6))), Wind.compassLong(Wind.bearingOf(f))));
            tip.add(Component.literal("x" + String.format("%.2f", menu.extra(7) / 100.0)).withStyle(ChatFormatting.GRAY));
            tip.add(Component.translatable("gui.ls_aeolus.dial.hint").withStyle(ChatFormatting.DARK_GRAY));
        }
    }
}
