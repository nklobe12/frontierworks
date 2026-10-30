package com.lodestar.datastore.client;

import com.lodestar.core.client.GuiDraw;
import com.lodestar.datastore.ItemKey;
import com.lodestar.datastore.menu.TerminalMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;

/** The network's running crafting jobs with progress and a cancel button each. */
public class JobsScreen extends Screen {
    private static final int W = 220, H = 172, ROWS = 6;
    private final TerminalScreen parent;
    private int timer;

    public JobsScreen(TerminalScreen parent) {
        super(Component.translatable("gui.ls_datastore.show_jobs"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        TermClient.send(TerminalMenu.A_JOBS, null, 0, 0);
    }

    private int x0() {
        return (width - W) / 2;
    }

    private int y0() {
        return (height - H) / 2;
    }

    @Override
    public void tick() {
        if (++timer % 20 == 0) TermClient.send(TerminalMenu.A_JOBS, null, 0, 0);
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        renderBackground(g);
        int x = x0(), y = y0();
        GuiDraw.panel(g, x, y, W, H);
        g.drawString(font, title, x + 8, y + 7, GuiDraw.TEXT, false);
        GuiDraw.inset(g, x + 7, y + 20, W - 14, ROWS * 24 + 2);
        if (TermClient.jobs.isEmpty()) g.drawString(font, Component.translatable("gui.ls_datastore.no_jobs"), x + 12, y + 26, 0xFFA0A0A0, false);
        for (int i = 0; i < ROWS && i < TermClient.jobs.size(); i++) {
            CompoundTag j = TermClient.jobs.getCompound(i);
            ItemKey k = ItemKey.load(j.getCompound("T"));
            if (k == null) continue;
            int ry = y + 21 + i * 24;
            g.renderItem(k.proto(), x + 10, ry + 4);
            g.drawString(font, Component.translatable("gui.ls_datastore.job", j.getLong("N"), k.proto().getHoverName()), x + 30, ry + 2, 0xFFE0E0E0, false);
            long done = j.getLong("Done"), total = Math.max(1, j.getLong("Total"));
            GuiDraw.hbar(g, x + 30, ry + 13, 100, 6, (float) done / total, 0xFF3FD0C9);
            g.drawString(font, Component.translatable("gui.ls_datastore.job." + j.getInt("State")), x + 134, ry + 12, 0xFFA0A0A0, false);
            boolean hov = GuiDraw.in(mx, my, x + W - 24, ry + 4, 14, 14);
            GuiDraw.button(g, x + W - 24, ry + 4, 14, 14, hov, true);
            g.drawString(font, "x", x + W - 20, ry + 7, 0xFF802020, false);
            if (hov) g.renderTooltip(font, Component.translatable("gui.ls_datastore.cancel"), mx, my);
        }
        super.render(g, mx, my, pt);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        int x = x0(), y = y0();
        for (int i = 0; i < ROWS && i < TermClient.jobs.size(); i++) {
            int ry = y + 21 + i * 24;
            if (GuiDraw.in((int) mx, (int) my, x + W - 24, ry + 4, 14, 14)) {
                TermClient.cancel(TermClient.jobs.getCompound(i).getInt("Id"));
                return true;
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
