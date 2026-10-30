package com.lodestar.datastore.client;

import com.lodestar.core.client.GuiDraw;
import com.lodestar.datastore.ItemKey;
import com.lodestar.datastore.menu.TerminalMenu;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/** Shows the server's crafting plan: what is taken from storage, crafted on the way, and missing. */
public class PlanScreen extends Screen {
    private static final int W = 230, H = 196, ROWS = 7;
    private final TerminalScreen parent;
    private final ItemKey key;
    private final long count;
    private Button start;
    private int scroll;

    private record Row(ItemKey key, long used, long crafted, long missing) {}

    public PlanScreen(TerminalScreen parent, ItemKey key, long count) {
        super(Component.translatable("gui.ls_datastore.plan"));
        this.parent = parent;
        this.key = key;
        this.count = count;
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
        addRenderableWidget(Button.builder(Component.translatable("gui.ls_datastore.back"), b -> minecraft.setScreen(new AmountScreen(parent, key)))
                .bounds(x + 8, y + H - 26, 100, 18).build());
        start = addRenderableWidget(Button.builder(Component.translatable("gui.ls_datastore.start"), b -> {
            TermClient.send(TerminalMenu.A_START, key, 0, count);
            minecraft.setScreen(parent);
        }).bounds(x + W - 108, y + H - 26, 100, 18).build());
        start.active = false;
    }

    private List<Row> rows(CompoundTag plan) {
        List<Row> l = new ArrayList<>();
        for (Tag t : plan.getList("Rows", Tag.TAG_COMPOUND)) {
            CompoundTag c = (CompoundTag) t;
            ItemKey k = ItemKey.load(c.getCompound("K"));
            if (k != null) l.add(new Row(k, c.getLong("U"), c.getLong("C"), c.getLong("M")));
        }
        return l;
    }

    private boolean current(CompoundTag plan) {
        ItemKey k = ItemKey.load(plan.getCompound("T"));
        return k != null && k.equals(key) && plan.getLong("N") == count;
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        renderBackground(g);
        int x = x0(), y = y0();
        GuiDraw.panel(g, x, y, W, H);
        g.drawString(font, Component.translatable("gui.ls_datastore.plan_title", count, key.proto().getHoverName()), x + 8, y + 7, GuiDraw.TEXT, false);
        CompoundTag plan = TermClient.plan;
        if (plan == null || !current(plan)) {
            start.active = false;
            g.drawString(font, Component.translatable("gui.ls_datastore.planning"), x + 8, y + 40, 0xFF606060, false);
            super.render(g, mx, my, pt);
            return;
        }
        boolean ok = plan.getBoolean("Ok");
        start.active = ok;
        Component status = ok ? Component.translatable("gui.ls_datastore.plan_ok", plan.getLong("Crafts")).withStyle(ChatFormatting.DARK_GREEN)
                : Component.translatable("gui.ls_datastore.plan_missing").withStyle(ChatFormatting.DARK_RED);
        g.drawString(font, status, x + 8, y + 19, 0xFF404040, false);
        int hx = x + 8, hy = y + 32;
        g.drawString(font, Component.translatable("gui.ls_datastore.col.item"), hx + 20, hy, 0xFF505050, false);
        g.drawString(font, Component.translatable("gui.ls_datastore.col.used"), hx + 118, hy, 0xFF505050, false);
        g.drawString(font, Component.translatable("gui.ls_datastore.col.craft"), hx + 152, hy, 0xFF505050, false);
        g.drawString(font, Component.translatable("gui.ls_datastore.col.missing"), hx + 186, hy, 0xFF505050, false);
        List<Row> rows = rows(plan);
        scroll = Math.max(0, Math.min(scroll, rows.size() - ROWS));
        GuiDraw.inset(g, x + 7, y + 42, W - 14, ROWS * 18 + 2);
        Row hovered = null;
        for (int i = 0; i < ROWS && scroll + i < rows.size(); i++) {
            Row r = rows.get(scroll + i);
            int ry = y + 43 + i * 18;
            if (r.missing > 0) g.fill(x + 8, ry, x + W - 8, ry + 18, 0x60A02020);
            g.renderItem(r.key.proto(), hx + 1, ry + 1);
            g.drawString(font, font.plainSubstrByWidth(r.key.proto().getHoverName().getString(), 94), hx + 20, ry + 5, 0xFFE0E0E0, false);
            if (r.used > 0) g.drawString(font, TermClient.fmt(r.used), hx + 118, ry + 5, 0xFFB0E0FF, false);
            if (r.crafted > 0) g.drawString(font, TermClient.fmt(r.crafted), hx + 152, ry + 5, 0xFFF0D070, false);
            if (r.missing > 0) g.drawString(font, TermClient.fmt(r.missing), hx + 186, ry + 5, 0xFFFF7070, false);
            if (GuiDraw.in(mx, my, hx, ry, W - 16, 18)) hovered = r;
        }
        super.render(g, mx, my, pt);
        if (hovered != null) g.renderTooltip(font, hovered.key.proto(), mx, my);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        scroll -= (int) Math.signum(delta);
        return true;
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
