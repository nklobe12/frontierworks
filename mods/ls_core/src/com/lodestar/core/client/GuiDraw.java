package com.lodestar.core.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;

/** Procedural vanilla-style GUI drawing so machines need no GUI textures. */
public final class GuiDraw {
    private GuiDraw() {}

    public static final int BG = 0xFFC6C6C6;
    public static final int DARK = 0xFF373737;
    public static final int LIGHT = 0xFFFFFFFF;
    public static final int SLOT = 0xFF8B8B8B;
    public static final int TEXT = 0xFF404040;

    public static void panel(GuiGraphics g, int x, int y, int w, int h) {
        g.fill(x + 1, y, x + w - 1, y + 1, 0xFF000000);
        g.fill(x + 1, y + h - 1, x + w - 1, y + h, 0xFF000000);
        g.fill(x, y + 1, x + 1, y + h - 1, 0xFF000000);
        g.fill(x + w - 1, y + 1, x + w, y + h - 1, 0xFF000000);
        g.fill(x + 1, y + 1, x + w - 1, y + h - 1, BG);
        g.fill(x + 1, y + 1, x + w - 2, y + 3, LIGHT);
        g.fill(x + 1, y + 1, x + 3, y + h - 2, LIGHT);
        g.fill(x + 3, y + h - 3, x + w - 1, y + h - 1, 0xFF555555);
        g.fill(x + w - 3, y + 3, x + w - 1, y + h - 1, 0xFF555555);
    }

    /** Slot frame around an item position (x,y = item top-left). */
    public static void slot(GuiGraphics g, int x, int y) {
        g.fill(x - 1, y - 1, x + 17, y, DARK);
        g.fill(x - 1, y - 1, x, y + 17, DARK);
        g.fill(x, y + 16, x + 17, y + 17, LIGHT);
        g.fill(x + 16, y, x + 17, y + 17, LIGHT);
        g.fill(x, y, x + 16, y + 16, SLOT);
    }

    public static void bigSlot(GuiGraphics g, int x, int y) {
        g.fill(x - 5, y - 5, x + 21, y - 4, DARK);
        g.fill(x - 5, y - 5, x - 4, y + 21, DARK);
        g.fill(x - 4, y + 20, x + 21, y + 21, LIGHT);
        g.fill(x + 20, y - 4, x + 21, y + 21, LIGHT);
        g.fill(x - 4, y - 4, x + 20, y + 20, SLOT);
    }

    public static void inset(GuiGraphics g, int x, int y, int w, int h) {
        g.fill(x - 1, y - 1, x + w + 1, y, DARK);
        g.fill(x - 1, y - 1, x, y + h + 1, DARK);
        g.fill(x, y + h, x + w + 1, y + h + 1, LIGHT);
        g.fill(x + w, y, x + w + 1, y + h + 1, LIGHT);
        g.fill(x, y, x + w, y + h, 0xFF2B2B2B);
    }

    public static void energyBar(GuiGraphics g, int x, int y, int w, int h, int stored, int max) {
        inset(g, x, y, w, h);
        if (max <= 0 || stored <= 0) return;
        int filled = (int) Math.ceil((double) h * Math.min(stored, max) / max);
        for (int i = 0; i < filled; i++) {
            int yy = y + h - 1 - i;
            float f = (float) i / Math.max(1, h - 1);
            int r = 0xB0 + (int) (0x4F * f);
            int gg = 0x10 + (int) (0x50 * f);
            int color = 0xFF000000 | (Math.min(255, r) << 16) | (gg << 8) | 0x18;
            g.fill(x + 1, yy, x + w - 1, yy + 1, color);
        }
    }

    public static void fluid(GuiGraphics g, int x, int y, int w, int h, Fluid fluid, int amount, int capacity) {
        inset(g, x, y, w, h);
        if (fluid == null || amount <= 0 || capacity <= 0) return;
        int filled = Math.max(1, (int) ((long) h * Math.min(amount, capacity) / capacity));
        IClientFluidTypeExtensions ext = IClientFluidTypeExtensions.of(fluid);
        TextureAtlasSprite sprite = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(ext.getStillTexture());
        int tint = ext.getTintColor();
        float a = ((tint >> 24) & 0xFF) / 255f;
        if (a <= 0.01f) a = 1f;
        float r = ((tint >> 16) & 0xFF) / 255f;
        float gr = ((tint >> 8) & 0xFF) / 255f;
        float b = (tint & 0xFF) / 255f;
        int top = y + h - filled;
        for (int yy = y + h; yy > top; yy -= 16) {
            int seg = Math.min(16, yy - top);
            for (int xx = x; xx < x + w; xx += 16) {
                int segW = Math.min(16, x + w - xx);
                g.blit(xx, yy - seg, 0, segW, seg, sprite, r, gr, b, a);
            }
        }
        // gauge marks
        for (int m = 1; m < 4; m++) {
            int my = y + h - h * m / 4;
            g.fill(x + w - 4, my, x + w, my + 1, 0x80FFFFFF);
        }
    }

    /** Right-pointing progress arrow, 24x17. */
    public static void arrow(GuiGraphics g, int x, int y, float progress) {
        int back = 0xFF8B8B8B;
        int fill = 0xFFFFFFFF;
        int filledW = Math.round(24 * Math.max(0, Math.min(1, progress)));
        for (int i = 0; i < 24; i++) {
            int top, bottom;
            if (i < 15) {
                top = y + 6;
                bottom = y + 11;
            } else {
                int k = i - 15;
                top = y + k;
                bottom = y + 17 - k;
            }
            g.fill(x + i, top, x + i + 1, bottom, i < filledW ? fill : back);
        }
    }

    /** Vertical flame meter 14x14. */
    public static void flame(GuiGraphics g, int x, int y, float level) {
        g.fill(x, y, x + 14, y + 14, 0xFF6B6B6B);
        int h = Math.round(14 * Math.max(0, Math.min(1, level)));
        for (int i = 0; i < h; i++) {
            int yy = y + 13 - i;
            int color = i < 5 ? 0xFFFF4A00 : i < 10 ? 0xFFFF9A00 : 0xFFFFE04A;
            int inset = Math.max(0, (i - 6) / 2);
            g.fill(x + 2 + inset, yy, x + 12 - inset, yy + 1, color);
        }
    }

    /** A tab or side panel sticking out of a GUI: raised background with a colored accent along the top. */
    public static void tab(GuiGraphics g, int x, int y, int w, int h, int accent) {
        panel(g, x, y, w, h);
        g.fill(x + 3, y + 3, x + w - 3, y + 4, accent);
    }

    /** Flat button: raised when idle, darker when hovered, gray when disabled. */
    public static void button(GuiGraphics g, int x, int y, int w, int h, boolean hovered, boolean enabled) {
        int face = !enabled ? 0xFF8A8A8A : hovered ? 0xFFB8C8E8 : 0xFFD8D8D8;
        g.fill(x, y, x + w, y + h, 0xFF000000);
        g.fill(x + 1, y + 1, x + w - 1, y + h - 1, face);
        g.fill(x + 1, y + 1, x + w - 1, y + 2, enabled ? LIGHT : 0xFFA0A0A0);
        g.fill(x + 1, y + h - 2, x + w - 1, y + h - 1, 0xFF6A6A6A);
    }

    /** Vertical scroll bar: track plus a thumb at position (0..1) sized by visible fraction. */
    public static void scrollbar(GuiGraphics g, int x, int y, int w, int h, float pos, float visible) {
        inset(g, x, y, w, h);
        int th = Math.max(8, Math.round(h * Math.min(1f, visible)));
        int ty = y + Math.round((h - th) * Math.max(0f, Math.min(1f, pos)));
        g.fill(x, ty, x + w, ty + th, 0xFFC6C6C6);
        g.fill(x, ty, x + w, ty + 1, LIGHT);
        g.fill(x, ty + th - 1, x + w, ty + th, 0xFF555555);
    }

    /** Horizontal bar (progress, health, heat...) filled left to right with a color. */
    public static void hbar(GuiGraphics g, int x, int y, int w, int h, float fraction, int color) {
        inset(g, x, y, w, h);
        int fw = Math.round(w * Math.max(0f, Math.min(1f, fraction)));
        if (fw > 0) g.fill(x, y, x + fw, y + h, color);
    }

    /** Vertical bar filled bottom to top. */
    public static void vbar(GuiGraphics g, int x, int y, int w, int h, float fraction, int color) {
        inset(g, x, y, w, h);
        int fh = Math.round(h * Math.max(0f, Math.min(1f, fraction)));
        if (fh > 0) g.fill(x, y + h - fh, x + w, y + h, color);
    }

    /** Word-wraps text into width and draws it; returns the height used. */
    public static int wrapped(GuiGraphics g, net.minecraft.client.gui.Font font, net.minecraft.network.chat.Component text, int x, int y, int width, int color) {
        int yy = y;
        for (net.minecraft.util.FormattedCharSequence line : font.split(text, width)) {
            g.drawString(font, line, x, yy, color, false);
            yy += font.lineHeight + 1;
        }
        return yy - y;
    }

    public static boolean in(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }
}
