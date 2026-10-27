package com.lodestar.transmutation.client;

import com.lodestar.core.client.GuiDraw;
import com.lodestar.core.net.CoreNet;
import com.lodestar.transmutation.AetherValues;
import com.lodestar.transmutation.ExchangeMenu;
import com.lodestar.transmutation.Knowledge;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Exchange screen: stored aether, the dissolve and cell slots, and a searchable grid of every item you know. */
public class ExchangeScreen extends AbstractContainerScreen<ExchangeMenu> {
    private static final int GX = 70, GY = 34, COLS = 8, ROWS = 5;
    private EditBox search;
    private int page;
    private List<Item> shown = new ArrayList<>();
    private String lastQuery = null;
    private int lastKnown = -1;

    public ExchangeScreen(ExchangeMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        imageWidth = ExchangeMenu.W;
        imageHeight = ExchangeMenu.H;
        inventoryLabelY = 129;
        inventoryLabelX = (ExchangeMenu.W - 162) / 2;
    }

    @Override
    protected void init() {
        super.init();
        search = new EditBox(font, leftPos + GX, topPos + 18, COLS * 18, 12, Component.empty());
        search.setHint(Component.translatable("gui.ls_transmutation.search"));
        addRenderableWidget(search);
    }

    private void refresh() {
        String q = search.getValue().toLowerCase(Locale.ROOT);
        List<Item> known = Knowledge.known(minecraft.player);
        if (q.equals(lastQuery) && known.size() == lastKnown) return;
        lastQuery = q;
        lastKnown = known.size();
        List<Item> l = new ArrayList<>();
        for (Item it : known) {
            if (AetherValues.get(it) <= 0) continue;
            if (!q.isEmpty() && !new ItemStack(it).getHoverName().getString().toLowerCase(Locale.ROOT).contains(q)) continue;
            l.add(it);
        }
        l.sort((a, b) -> Long.compare(AetherValues.get(b), AetherValues.get(a)));
        shown = l;
        int pages = Math.max(1, (shown.size() + COLS * ROWS - 1) / (COLS * ROWS));
        page = Math.min(page, pages - 1);
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        refresh();
    }

    @Override
    protected void renderBg(GuiGraphics g, float pt, int mx, int my) {
        refresh();
        GuiDraw.panel(g, leftPos, topPos, imageWidth, imageHeight);
        GuiDraw.slot(g, leftPos + 18, topPos + 34);
        GuiDraw.slot(g, leftPos + 18, topPos + 82);
        for (int i = 0; i < 2; i++) {
            int bx = leftPos + 6 + i * 26, by = topPos + 102;
            GuiDraw.button(g, bx, by, 24, 12, GuiDraw.in(mx, my, bx, by, 24, 12), menu.slots.get(1).hasItem());
            Component c = Component.translatable(i == 0 ? "gui.ls_transmutation.fill" : "gui.ls_transmutation.empty");
            g.drawString(font, c, bx + 12 - font.width(c) / 2, by + 2, 0xFF303030, false);
        }
        GuiDraw.inset(g, leftPos + GX - 1, topPos + GY - 1, COLS * 18 + 1, ROWS * 18 + 1);
        int start = page * COLS * ROWS;
        for (int i = 0; i < COLS * ROWS && start + i < shown.size(); i++) {
            int x = leftPos + GX + (i % COLS) * 18, y = topPos + GY + (i / COLS) * 18;
            Item it = shown.get(start + i);
            boolean afford = Knowledge.aether(minecraft.player) >= AetherValues.get(it);
            if (GuiDraw.in(mx, my, x, y, 18, 18)) g.fill(x, y, x + 18, y + 18, 0x40FFFFFF);
            g.renderItem(new ItemStack(it), x + 1, y + 1);
            if (!afford) g.fill(x + 1, y + 1, x + 17, y + 17, 0x90202020);
        }
        int pages = Math.max(1, (shown.size() + COLS * ROWS - 1) / (COLS * ROWS));
        GuiDraw.button(g, leftPos + GX, topPos + 126 - 2, 14, 10, false, page > 0);
        GuiDraw.button(g, leftPos + GX + COLS * 18 - 14, topPos + 126 - 2, 14, 10, false, page < pages - 1);
        g.drawString(font, "<", leftPos + GX + 4, topPos + 125, 0xFF303030, false);
        g.drawString(font, ">", leftPos + GX + COLS * 18 - 10, topPos + 125, 0xFF303030, false);
        String pg = (page + 1) + "/" + pages;
        g.drawString(font, pg, leftPos + GX + COLS * 9 - font.width(pg) / 2, topPos + 125, 0xFF505050, false);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mx, int my) {
        g.drawString(font, title, 8, 6, GuiDraw.TEXT, false);
        Component av = Component.translatable("gui.ls_transmutation.aether", AetherValues.format(Knowledge.aether(minecraft.player)));
        g.drawString(font, av, imageWidth - 8 - font.width(av), 6, 0xFF2A6FA8, false);
        g.drawString(font, Component.translatable("gui.ls_transmutation.dissolve"), 6, 24, 0xFF505050, false);
        g.drawString(font, Component.translatable("gui.ls_transmutation.cell"), 6, 72, 0xFF505050, false);
        g.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, GuiDraw.TEXT, false);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        int start = page * COLS * ROWS;
        for (int i = 0; i < COLS * ROWS && start + i < shown.size(); i++) {
            int x = leftPos + GX + (i % COLS) * 18, y = topPos + GY + (i / COLS) * 18;
            if (GuiDraw.in((int) mx, (int) my, x, y, 18, 18)) {
                CompoundTag tag = new CompoundTag();
                tag.putString("Item", BuiltInRegistries.ITEM.getKey(shown.get(start + i)).toString());
                tag.putInt("Count", Screen.hasShiftDown() ? 64 : button == 1 ? 8 : 1);
                CoreNet.sendTag("ls_transmutation:withdraw", tag);
                return true;
            }
        }
        int pages = Math.max(1, (shown.size() + COLS * ROWS - 1) / (COLS * ROWS));
        if (GuiDraw.in((int) mx, (int) my, leftPos + GX, topPos + 124, 14, 10) && page > 0) {
            page--;
            return true;
        }
        if (GuiDraw.in((int) mx, (int) my, leftPos + GX + COLS * 18 - 14, topPos + 124, 14, 10) && page < pages - 1) {
            page++;
            return true;
        }
        for (int b = 0; b < 2; b++) {
            if (GuiDraw.in((int) mx, (int) my, leftPos + 6 + b * 26, topPos + 102, 24, 12) && minecraft.gameMode != null) {
                minecraft.gameMode.handleInventoryButtonClick(menu.containerId, b);
                return true;
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        int pages = Math.max(1, (shown.size() + COLS * ROWS - 1) / (COLS * ROWS));
        page = Math.max(0, Math.min(pages - 1, page - (int) Math.signum(delta)));
        return true;
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (search.isFocused()) {
            if (key == 256) return super.keyPressed(key, scan, mods);
            search.keyPressed(key, scan, mods);
            return true;
        }
        return super.keyPressed(key, scan, mods);
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        renderBackground(g);
        super.render(g, mx, my, pt);
        renderTooltip(g, mx, my);
        int start = page * COLS * ROWS;
        for (int i = 0; i < COLS * ROWS && start + i < shown.size(); i++) {
            int x = leftPos + GX + (i % COLS) * 18, y = topPos + GY + (i / COLS) * 18;
            if (GuiDraw.in(mx, my, x, y, 18, 18)) {
                Item it = shown.get(start + i);
                List<Component> tip = new ArrayList<>(getTooltipFromItem(minecraft, new ItemStack(it)));
                long v = AetherValues.get(it);
                tip.add(Component.translatable("gui.ls_transmutation.value", AetherValues.format(v)).withStyle(ChatFormatting.AQUA));
                long can = v > 0 ? Knowledge.aether(minecraft.player) / v : 0;
                tip.add(Component.translatable("gui.ls_transmutation.afford", can).withStyle(ChatFormatting.GRAY));
                tip.add(Component.translatable("gui.ls_transmutation.buy_hint").withStyle(ChatFormatting.DARK_GRAY));
                g.renderComponentTooltip(font, tip, mx, my);
            }
        }
    }
}
