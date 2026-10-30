package com.lodestar.datastore.client;

import com.lodestar.core.client.GuiDraw;
import com.lodestar.datastore.ItemKey;
import com.lodestar.datastore.menu.TerminalMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Asks how many of a craftable item to make, then asks the server for a plan. */
public class AmountScreen extends Screen {
    private static final int W = 176, H = 92;
    private final TerminalScreen parent;
    private final ItemKey key;
    private EditBox box;

    public AmountScreen(TerminalScreen parent, ItemKey key) {
        super(Component.translatable("gui.ls_datastore.amount"));
        this.parent = parent;
        this.key = key;
    }

    private int x0() {
        return (width - W) / 2;
    }

    private int y0() {
        return (height - H) / 2;
    }

    @Override
    protected void init() {
        int x = x0(), y = y0();
        box = new EditBox(font, x + 40, y + 24, 96, 14, Component.translatable("gui.ls_datastore.amount"));
        box.setFilter(s -> s.isEmpty() || (s.matches("\\d{1,6}")));
        box.setValue(box.getValue().isEmpty() ? "1" : box.getValue());
        addRenderableWidget(box);
        setInitialFocus(box);
        int[] steps = {-64, -10, -1, 1, 10, 64};
        for (int i = 0; i < steps.length; i++) {
            int d = steps[i];
            addRenderableWidget(Button.builder(Component.literal((d > 0 ? "+" : "") + d), b -> add(d)).bounds(x + 8 + i * 27, y + 44, 25, 16).build());
        }
        addRenderableWidget(Button.builder(Component.translatable("gui.ls_datastore.back"), b -> onClose()).bounds(x + 8, y + 66, 76, 18).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.ls_datastore.plan"), b -> plan()).bounds(x + 92, y + 66, 76, 18).build());
    }

    private long value() {
        try {
            return Math.max(1, Long.parseLong(box.getValue()));
        } catch (NumberFormatException e) {
            return 1;
        }
    }

    private void add(int d) {
        long v = value() == 1 && d > 1 ? d : value() + d;
        box.setValue(Long.toString(Math.max(1, Math.min(100_000, v))));
    }

    private void plan() {
        long n = value();
        TermClient.plan = null;
        TermClient.send(TerminalMenu.A_PLAN, key, 0, n);
        minecraft.setScreen(new PlanScreen(parent, key, n));
    }

    @Override
    public boolean keyPressed(int k, int s, int m) {
        if (k == 257 || k == 335) {
            plan();
            return true;
        }
        return super.keyPressed(k, s, m);
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        renderBackground(g);
        int x = x0(), y = y0();
        GuiDraw.panel(g, x, y, W, H);
        g.drawString(font, title, x + 8, y + 7, GuiDraw.TEXT, false);
        GuiDraw.slot(g, x + 16, y + 23);
        g.renderItem(key.proto(), x + 16, y + 23);
        super.render(g, mx, my, pt);
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
