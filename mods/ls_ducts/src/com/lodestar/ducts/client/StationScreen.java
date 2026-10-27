package com.lodestar.ducts.client;

import com.lodestar.core.client.GuiDraw;
import com.lodestar.core.net.CoreNet;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/** Pick where the pod should take you. */
public class StationScreen extends Screen {
    private static final int W = 200, ROW = 20, ROWS = 7;
    private final CompoundTag origin;
    private final String here;
    private final List<CompoundTag> dests = new ArrayList<>();
    private int scroll;

    public StationScreen(CompoundTag tag) {
        super(Component.translatable("gui.ls_ducts.station"));
        origin = tag.getCompound("Origin");
        here = tag.getString("Here");
        for (Tag t : tag.getList("List", Tag.TAG_COMPOUND)) dests.add((CompoundTag) t);
    }

    private int left() {
        return (width - W) / 2;
    }

    private int top() {
        return (height - (40 + ROWS * ROW)) / 2;
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        renderBackground(g);
        int x = left(), y = top(), h = 40 + ROWS * ROW;
        GuiDraw.panel(g, x, y, W, h);
        g.drawString(font, here.isEmpty() ? title.getString() : here, x + 8, y + 7, GuiDraw.TEXT, false);
        g.drawString(font, Component.translatable("gui.ls_ducts.choose"), x + 8, y + 19, 0xFF707070, false);
        for (int i = 0; i < ROWS && i + scroll < dests.size(); i++) {
            CompoundTag d = dests.get(i + scroll);
            int ry = y + 32 + i * ROW;
            boolean hov = GuiDraw.in(mx, my, x + 6, ry, W - 20, ROW - 2);
            GuiDraw.button(g, x + 6, ry, W - 20, ROW - 2, hov, true);
            g.drawString(font, font.plainSubstrByWidth(d.getString("Name"), W - 70), x + 11, ry + 5, GuiDraw.TEXT, false);
            String len = Component.translatable("gui.ls_ducts.blocks", d.getInt("Len")).getString();
            g.drawString(font, len, x + W - 18 - font.width(len), ry + 5, 0xFF606060, false);
        }
        if (dests.size() > ROWS) GuiDraw.scrollbar(g, x + W - 11, y + 32, 6, ROWS * ROW - 2, (float) scroll / (dests.size() - ROWS), (float) ROWS / dests.size());
        super.render(g, mx, my, pt);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        int x = left(), y = top();
        for (int i = 0; i < ROWS && i + scroll < dests.size(); i++) {
            int ry = y + 32 + i * ROW;
            if (GuiDraw.in((int) mx, (int) my, x + 6, ry, W - 20, ROW - 2)) {
                CompoundTag t = new CompoundTag();
                t.put("Origin", origin);
                t.put("Dest", dests.get(i + scroll).getCompound("Pos"));
                CoreNet.sendTag("ls_ducts:travel", t);
                onClose();
                return true;
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        scroll = Math.max(0, Math.min(Math.max(0, dests.size() - ROWS), scroll - (int) Math.signum(delta)));
        return true;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
