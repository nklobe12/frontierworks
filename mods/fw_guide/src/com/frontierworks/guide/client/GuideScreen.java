package com.frontierworks.guide.client;

import com.frontierworks.core.util.FW;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/** Three-column guide: categories, mods in the category, and the selected mod's page with its items. */
public class GuideScreen extends Screen {
    private static final int W = 380, H = 230;
    private static final int MODS_PER_PAGE = 10, ITEMS_PER_PAGE = 40;
    private final GuideData data;
    private int cat;
    private int modPage;
    private int mod = -1;
    private int itemPage;
    private int left, top;

    public GuideScreen(GuideData data) {
        super(Component.translatable("guide.fw_guide.title"));
        this.data = data;
    }

    /** Opens directly on a mod's page (used by the showcase). */
    public GuideScreen(GuideData data, String modid) {
        this(data);
        for (int c = 0; c < data.categories.size(); c++) {
            List<GuideData.ModPage> mods = data.categories.get(c).mods();
            for (int i = 0; i < mods.size(); i++) {
                if (mods.get(i).id().equals(modid)) {
                    cat = c;
                    mod = i;
                    modPage = i / MODS_PER_PAGE;
                }
            }
        }
    }

    @Override
    protected void init() {
        left = (width - W) / 2;
        top = (height - H) / 2;
        rebuild();
    }

    private void rebuild() {
        clearWidgets();
        for (int i = 0; i < data.categories.size(); i++) {
            final int idx = i;
            GuideData.Category c = data.categories.get(i);
            Button b = Button.builder(Component.literal(c.title()), btn -> {
                cat = idx;
                mod = -1;
                modPage = 0;
                itemPage = 0;
                rebuild();
            }).bounds(left + 6, top + 22 + i * 24, 92, 20).build();
            b.active = i != cat;
            addRenderableWidget(b);
        }
        if (data.categories.isEmpty()) return;
        List<GuideData.ModPage> mods = data.categories.get(cat).mods();
        int start = modPage * MODS_PER_PAGE;
        for (int i = start; i < Math.min(mods.size(), start + MODS_PER_PAGE); i++) {
            final int idx = i;
            Button b = Button.builder(Component.literal(mods.get(i).name()), btn -> {
                mod = idx;
                itemPage = 0;
                rebuild();
            }).bounds(left + 104, top + 22 + (i - start) * 18, 112, 16).build();
            b.active = i != mod;
            addRenderableWidget(b);
        }
        int pages = (mods.size() + MODS_PER_PAGE - 1) / MODS_PER_PAGE;
        if (pages > 1) {
            addRenderableWidget(Button.builder(Component.literal("<"), b -> { modPage = (modPage + pages - 1) % pages; rebuild(); })
                    .bounds(left + 104, top + H - 24, 20, 16).build());
            addRenderableWidget(Button.builder(Component.literal(">"), b -> { modPage = (modPage + 1) % pages; rebuild(); })
                    .bounds(left + 196, top + H - 24, 20, 16).build());
        }
        if (mod >= 0 && mod < mods.size()) {
            int items = mods.get(mod).items().size();
            int ip = (items + ITEMS_PER_PAGE - 1) / ITEMS_PER_PAGE;
            if (ip > 1) {
                addRenderableWidget(Button.builder(Component.literal("<"), b -> { itemPage = (itemPage + ip - 1) % ip; rebuild(); })
                        .bounds(left + W - 52, top + H - 24, 20, 16).build());
                addRenderableWidget(Button.builder(Component.literal(">"), b -> { itemPage = (itemPage + 1) % ip; rebuild(); })
                        .bounds(left + W - 28, top + H - 24, 20, 16).build());
            }
        }
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        renderBackground(g);
        g.fill(left - 2, top - 2, left + W + 2, top + H + 2, 0xff2a2018);
        g.fill(left, top, left + W, top + H, 0xfff2e8d0);
        g.fill(left + 100, top + 18, left + 101, top + H - 6, 0xffc8b890);
        g.fill(left + 220, top + 18, left + 221, top + H - 6, 0xffc8b890);
        g.drawString(font, title, left + 8, top + 6, 0x4a3420, false);
        super.render(g, mx, my, pt);
        if (data.categories.isEmpty()) return;
        GuideData.Category c = data.categories.get(cat);
        int x = left + 226, y = top + 22, w = W - 232;
        if (mod < 0 || mod >= c.mods().size()) {
            g.drawString(font, Component.literal(c.title()), x, y, 0x4a3420, false);
            y += 14;
            for (FormattedCharSequence line : font.split(Component.literal(c.intro()), w)) {
                g.drawString(font, line, x, y, 0x5a4a38, false);
                y += 10;
            }
            y += 6;
            g.drawString(font, Component.translatable("guide.fw_guide.mods", c.mods().size()), x, y, 0x8a6a48, false);
            y += 12;
            for (FormattedCharSequence line : font.split(Component.translatable("guide.fw_guide.pick"), w)) {
                g.drawString(font, line, x, y, 0x8a7a68, false);
                y += 10;
            }
            ItemStack icon = FW.stack(c.icon(), 1);
            if (!icon.isEmpty()) g.renderItem(icon, left + W - 24, top + 4);
            return;
        }
        GuideData.ModPage page = c.mods().get(mod);
        g.drawString(font, Component.literal(page.name()), x, y, 0x4a3420, false);
        y += 12;
        List<FormattedCharSequence> lines = font.split(Component.literal(page.desc()), w);
        for (int i = 0; i < Math.min(lines.size(), 9); i++) {
            g.drawString(font, lines.get(i), x, y, 0x5a4a38, false);
            y += 10;
        }
        y += 4;
        g.drawString(font, Component.translatable("guide.fw_guide.items", page.items().size()), x, y, 0x8a6a48, false);
        y += 11;
        List<ItemStack> all = new ArrayList<>();
        for (String id : page.items()) {
            ItemStack s = FW.stack(id, 1);
            if (!s.isEmpty()) all.add(s);
        }
        int start = itemPage * ITEMS_PER_PAGE;
        List<ItemStack> stacks = all.subList(Math.min(start, all.size()), Math.min(all.size(), start + ITEMS_PER_PAGE));
        int perRow = Math.max(1, w / 18);
        ItemStack hovered = ItemStack.EMPTY;
        for (int i = 0; i < stacks.size(); i++) {
            int ix = x + (i % perRow) * 18, iy = y + (i / perRow) * 18;
            if (iy + 16 > top + H - 26) break;
            ItemStack s = stacks.get(i);
            if (s.isEmpty()) continue;
            g.renderItem(s, ix, iy);
            if (mx >= ix && mx < ix + 16 && my >= iy && my < iy + 16) hovered = s;
        }
        if (!hovered.isEmpty()) g.renderTooltip(font, hovered, mx, my);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
