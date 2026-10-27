package com.lodestar.veins.client;

import com.lodestar.core.client.GuiDraw;
import com.lodestar.veins.OreIndex;
import com.lodestar.veins.SurveyReportItem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Depth chart of a seismic survey: one column per ore, one row per 16-block layer, cells shaded by how much ore
 * the layer holds. Hover a cell for exact counts.
 */
public class SurveyReportScreen extends Screen {
    private static final int CELL_W = 22, CELL_H = 9;
    private final CompoundTag tag;
    private final List<String> ores = new ArrayList<>();
    private int layers;
    private int firstLayer, lastLayer;
    private int max = 1;

    public SurveyReportScreen(CompoundTag tag) {
        super(Component.translatable("item.ls_veins.survey_report"));
        this.tag = tag;
        CompoundTag o = tag.getCompound("Ores");
        ores.addAll(o.getAllKeys());
        ores.sort((a, b) -> Integer.compare(OreIndex.NAMES.indexOf(a), OreIndex.NAMES.indexOf(b)));
        firstLayer = Integer.MAX_VALUE;
        lastLayer = -1;
        for (String k : ores) {
            int[] a = o.getIntArray(k);
            layers = Math.max(layers, a.length);
            for (int i = 0; i < a.length; i++) {
                if (a[i] > 0) {
                    firstLayer = Math.min(firstLayer, i);
                    lastLayer = Math.max(lastLayer, i);
                    max = Math.max(max, a[i]);
                }
            }
        }
        if (lastLayer < 0) {
            firstLayer = 0;
            lastLayer = 0;
        }
    }

    private int panelW() {
        return Math.max(180, 44 + ores.size() * CELL_W + 8);
    }

    private int panelH() {
        return 40 + (lastLayer - firstLayer + 1) * CELL_H + 16;
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        renderBackground(g);
        int w = panelW(), h = panelH();
        int x = (width - w) / 2, y = (height - h) / 2;
        GuiDraw.panel(g, x, y, w, h);
        g.drawString(font, title, x + (w - font.width(title)) / 2, y + 6, GuiDraw.TEXT, false);
        Component at = Component.translatable("tooltip.ls_veins.report.at", tag.getInt("X"), tag.getInt("Z"));
        g.drawString(font, at, x + (w - font.width(at)) / 2, y + 16, 0xFF707070, false);
        int minY = tag.getInt("MinY");
        int gx = x + 40, gy = y + 30;
        CompoundTag o = tag.getCompound("Ores");
        if (ores.isEmpty()) {
            Component c = Component.translatable("tooltip.ls_veins.report.empty");
            g.drawString(font, c, x + (w - font.width(c)) / 2, gy + 10, GuiDraw.TEXT, false);
            return;
        }
        // headers
        for (int i = 0; i < ores.size(); i++) {
            String k = ores.get(i);
            int cx = gx + i * CELL_W;
            g.fill(cx + 2, gy - 6, cx + CELL_W - 2, gy - 2, 0xFF000000 | OreIndex.color(k));
        }
        String hover = null;
        for (int layer = lastLayer; layer >= firstLayer; layer--) {
            int row = lastLayer - layer;
            int ry = gy + row * CELL_H;
            int yLo = minY + layer * 16;
            g.drawString(font, String.valueOf(yLo), x + 6, ry + 1, 0xFF505050, false);
            for (int i = 0; i < ores.size(); i++) {
                String k = ores.get(i);
                int[] a = o.getIntArray(k);
                int v = layer < a.length ? a[layer] : 0;
                int cx = gx + i * CELL_W;
                int base = OreIndex.color(k);
                float f = v <= 0 ? 0 : (float) (Math.log1p(v) / Math.log1p(max));
                int col = v <= 0 ? 0xFF2B2B2B : blend(0x2B2B2B, base, 0.25f + 0.75f * f);
                g.fill(cx + 1, ry, cx + CELL_W - 1, ry + CELL_H - 1, col);
                if (mx >= cx && mx < cx + CELL_W && my >= ry && my < ry + CELL_H) {
                    hover = k + ":" + v + ":" + yLo;
                }
            }
        }
        // totals
        int ty = gy + (lastLayer - firstLayer + 1) * CELL_H + 3;
        for (int i = 0; i < ores.size(); i++) {
            String t = String.valueOf(SurveyReportItem.total(o.getIntArray(ores.get(i))));
            g.drawString(font, t, gx + i * CELL_W + (CELL_W - font.width(t)) / 2, ty, 0xFF404040, false);
        }
        super.render(g, mx, my, pt);
        if (hover != null) {
            String[] p = hover.split(":");
            List<Component> tip = new ArrayList<>();
            tip.add(OreIndex.display(p[0]));
            tip.add(Component.translatable("gui.ls_veins.cell", p[1], p[2], Integer.parseInt(p[2]) + 15).withStyle(s -> s.withColor(0xAAAAAA)));
            g.renderComponentTooltip(font, tip, mx, my);
        } else {
            for (int i = 0; i < ores.size(); i++) {
                int cx = gx + i * CELL_W;
                if (mx >= cx && mx < cx + CELL_W && my >= gy - 8 && my < gy) {
                    g.renderTooltip(font, OreIndex.display(ores.get(i)), mx, my);
                }
            }
        }
    }

    private static int blend(int a, int b, float t) {
        int r = (int) (((a >> 16) & 255) * (1 - t) + ((b >> 16) & 255) * t);
        int gg = (int) (((a >> 8) & 255) * (1 - t) + ((b >> 8) & 255) * t);
        int bl = (int) ((a & 255) * (1 - t) + (b & 255) * t);
        return 0xFF000000 | (r << 16) | (gg << 8) | bl;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
