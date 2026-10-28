package com.lodestar.aeolus.client;

import com.lodestar.aeolus.TidalGeneratorBE;
import com.lodestar.aeolus.Tides;
import com.lodestar.core.client.GuiDraw;
import com.lodestar.core.client.MachineScreen;
import com.lodestar.core.machine.MachineMenu;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.Level;

import java.util.List;

/** Tidal generator screen with a chart of the output through the current day and a marker at the present time. */
public class TidalScreen extends MachineScreen {
    private static final int CX = 30, CY = 17, CW = 96, CH = 28;

    public TidalScreen(MachineMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
    }

    @Override
    protected void drawExtraBg(GuiGraphics g, int x, int y, int mouseX, int mouseY) {
        Level level = menu.be.getLevel();
        if (level == null) return;
        GuiDraw.inset(g, x + CX, y + CY, CW, CH);
        long day = level.getDayTime() - Math.floorMod(level.getDayTime(), 24000L);
        double moon = Tides.moon(level.getMoonPhase());
        double water = menu.extra(1) == TidalGeneratorBE.OK ? Math.max(0.05, menu.extra(2) / 100.0) : 0.15;
        for (int i = 0; i < CW; i++) {
            long t = day + i * 24000L / CW;
            double v = Tides.current(t) * moon * water / 1.25;
            int h = (int) Math.round(v * (CH - 2));
            boolean rising = Tides.rising(t);
            int col = menu.extra(1) == TidalGeneratorBE.OK ? (rising ? 0xFF2A9D8F : 0xFF3A7FB0) : 0xFF555555;
            if (h > 0) g.fill(x + CX + i, y + CY + CH - h, x + CX + i + 1, y + CY + CH, col);
            int th = (int) Math.round((Tides.height(t) * 0.5 + 0.5) * (CH - 3));
            g.fill(x + CX + i, y + CY + CH - 2 - th, x + CX + i + 1, y + CY + CH - 1 - th, 0xFFE8E8E8);
        }
        int now = (int) (Math.floorMod(level.getDayTime(), 24000L) * CW / 24000L);
        g.fill(x + CX + now, y + CY - 1, x + CX + now + 1, y + CY + CH + 1, 0xFFFFD040);
    }

    @Override
    protected void addExtraTooltip(List<Component> tip, int rx, int ry) {
        Level level = menu.be.getLevel();
        if (level == null || !GuiDraw.in(rx, ry, CX, CY, CW, CH)) return;
        long t = level.getDayTime();
        tip.add(Component.translatable("gui.ls_aeolus.chart"));
        double c = Tides.current(t);
        tip.add(Component.translatable(c < 0.15 ? "gui.ls_aeolus.tide.slack" : Tides.rising(t) ? "gui.ls_aeolus.tide.flood" : "gui.ls_aeolus.tide.ebb")
                .withStyle(ChatFormatting.GRAY));
        int phase = level.getMoonPhase();
        tip.add(Component.translatable("gui.ls_aeolus.moon", Component.translatable("gui.ls_aeolus.phase." + phase),
                String.format("%.2f", Tides.moon(phase))).withStyle(ChatFormatting.GRAY));
        tip.add(Component.translatable(Tides.moonKind(phase)).withStyle(ChatFormatting.DARK_GRAY));
    }
}
