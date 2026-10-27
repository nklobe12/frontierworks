package com.lodestar.lens.client;

import com.lodestar.core.client.GuiDraw;
import com.lodestar.core.client.RecipeDraw;
import com.lodestar.core.net.CoreNet;
import com.lodestar.core.recipe.display.RecipeView;
import com.lodestar.lens.VanillaCategories;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.CraftingScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;
import org.lwjgl.glfw.GLFW;

import javax.annotation.Nullable;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/** Recipes producing (or using) an item or fluid, one tab per category, with history and grid transfer. */
public class LensScreen extends Screen {
    private record State(RecipeIndex.Mode mode, ItemStack item, FluidStack fluid, @Nullable ResourceLocation category, int tab, int page) {}

    private static final int TAB = 24, GAP = 6;
    @Nullable
    private final Screen parent;
    private final Deque<State> history = new ArrayDeque<>();
    private State state;
    private List<RecipeIndex.Tab> tabs = List.of();
    private int tab, page, tabScroll;
    /** first view index of each page of the current tab */
    private final List<Integer> pageStarts = new ArrayList<>();
    private int left, top, w, h;

    private LensScreen(@Nullable Screen parent, State s) {
        super(Component.translatable("lens.ls_lens.title"));
        this.parent = parent;
        this.state = s;
    }

    /** Opens the viewer (or moves an open one); returns false when nothing matches. */
    public static boolean open(RecipeIndex.Mode mode, ItemStack item, FluidStack fluid, @Nullable ResourceLocation category) {
        Minecraft mc = Minecraft.getInstance();
        State s = new State(mode, item.isEmpty() ? ItemStack.EMPTY : item.copyWithCount(1), fluid.isEmpty() ? FluidStack.EMPTY : fluid.copy(), category, 0, 0);
        if (RecipeIndex.lookup(mode, s.item, s.fluid, category).isEmpty()) {
            if (mc.player != null) mc.player.playSound(SoundEvents.UI_BUTTON_CLICK.value(), 0.3f, 0.6f);
            return false;
        }
        if (mc.screen instanceof LensScreen ls) {
            ls.history.push(ls.snapshot());
            ls.state = s;
            ls.load();
        } else {
            mc.setScreen(new LensScreen(mc.screen, s));
        }
        return true;
    }

    private State snapshot() {
        return new State(state.mode, state.item, state.fluid, state.category, tab, page);
    }

    private void load() {
        tabs = RecipeIndex.lookup(state.mode, state.item, state.fluid, state.category);
        tab = Math.min(state.tab, Math.max(0, tabs.size() - 1));
        page = state.page;
        tabScroll = Math.max(0, tab - visibleTabs() + 1);
        layout();
    }

    @Override
    protected void init() {
        load();
    }

    private int visibleTabs() {
        return Math.max(1, (Math.min(width - 40, 260) - 8) / TAB);
    }

    private void layout() {
        int maxW = 120;
        for (RecipeIndex.Tab t : tabs) for (RecipeView v : t.views()) maxW = Math.max(maxW, v.width);
        w = Math.max(196, Math.min(width - 60, maxW + 32));
        h = Math.min(height - 36, 300);
        left = (width - w) / 2;
        top = (height - h) / 2 + 12;
        pageStarts.clear();
        if (tabs.isEmpty()) return;
        List<RecipeView> vs = tabs.get(tab).views();
        int avail = h - 40;
        int used = 0;
        pageStarts.add(0);
        for (int i = 0; i < vs.size(); i++) {
            int vh = vs.get(i).height + GAP;
            if (used > 0 && used + vh > avail) {
                pageStarts.add(i);
                used = 0;
            }
            used += vh;
        }
        page = Math.max(0, Math.min(page, pageStarts.size() - 1));
    }

    private boolean canTransfer(RecipeView v) {
        if (v.recipeId == null || v.gridWidth <= 0 || !v.category.equals(VanillaCategories.CRAFTING)) return false;
        if (parent instanceof CraftingScreen) return true;
        return parent instanceof InventoryScreen && v.gridWidth <= 2;
    }

    // ------------------------------------------------------------------ drawing

    private String focusName() {
        if (!state.item.isEmpty()) return state.item.getHoverName().getString();
        if (!state.fluid.isEmpty()) return state.fluid.getDisplayName().getString();
        return "";
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        renderBackground(g);
        if (tabs.isEmpty()) {
            super.render(g, mx, my, pt);
            return;
        }
        // category tabs
        int vt = visibleTabs();
        int tx0 = left + 4;
        for (int i = tabScroll; i < tabs.size() && i < tabScroll + vt; i++) {
            int tx = tx0 + (i - tabScroll) * TAB;
            boolean sel = i == tab;
            GuiDraw.tab(g, tx, top - TAB + (sel ? 2 : 4), TAB - 1, TAB, sel ? 0xFFFFD86B : 0xFF8899AA);
            g.renderItem(tabs.get(i).category().icon(), tx + 4, top - TAB + (sel ? 7 : 9));
        }
        if (tabScroll > 0) arrowButton(g, left - 14, top - 20, "<", mx, my);
        if (tabScroll + vt < tabs.size()) arrowButton(g, left + w + 2, top - 20, ">", mx, my);
        GuiDraw.panel(g, left, top, w, h);
        RecipeIndex.Tab t = tabs.get(tab);
        // header
        g.drawString(font, font.plainSubstrByWidth(t.category().title.getString(), w - 70), left + 8, top + 7, GuiDraw.TEXT, false);
        String sub = switch (state.mode) {
            case RECIPES -> Component.translatable("lens.ls_lens.recipes_for", focusName()).getString();
            case USES -> Component.translatable("lens.ls_lens.uses_of", focusName()).getString();
            case CATEGORY -> Component.translatable("lens.ls_lens.all_recipes", t.views().size()).getString();
        };
        g.drawString(font, font.plainSubstrByWidth(sub, w - 16), left + 8, top + 19, 0xFF707070, false);
        String pg = (page + 1) + "/" + pageStarts.size();
        g.drawString(font, pg, left + w - 30 - font.width(pg) / 2, top + 7, GuiDraw.TEXT, false);
        if (pageStarts.size() > 1) {
            arrowButton(g, left + w - 52, top + 5, "<", mx, my);
            arrowButton(g, left + w - 18, top + 5, ">", mx, my);
        }
        // catalysts
        List<ItemStack> cats = t.category().catalysts();
        if (!cats.isEmpty()) {
            int ch = Math.min(cats.size(), 8) * 18 + 8;
            GuiDraw.panel(g, left - 26, top + 4, 26, ch);
            for (int i = 0; i < cats.size() && i < 8; i++) {
                GuiDraw.slot(g, left - 21, top + 9 + i * 18);
                g.renderItem(cats.get(i), left - 21, top + 9 + i * 18);
            }
        }
        // recipes
        RecipeDraw.Hit hover = null;
        ItemStack hoverCat = ItemStack.EMPTY;
        int start = pageStarts.get(page), end = page + 1 < pageStarts.size() ? pageStarts.get(page + 1) : t.views().size();
        int y = top + 34;
        for (int i = start; i < end; i++) {
            RecipeView v = t.views().get(i);
            int x = left + (w - v.width) / 2;
            RecipeDraw.draw(g, v, x, y, mx, my);
            RecipeDraw.Hit hh = RecipeDraw.hit(v, x, y, mx, my);
            if (hh != null) hover = hh;
            if (canTransfer(v)) {
                int bx = x + v.width - 12, by = y + v.height - 12;
                boolean on = GuiDraw.in(mx, my, bx, by, 12, 12);
                GuiDraw.button(g, bx, by, 12, 12, on, true);
                g.drawString(font, "+", bx + 3, by + 2, GuiDraw.TEXT, false);
                if (on) g.renderTooltip(font, List.of(Component.translatable("lens.ls_lens.transfer").getVisualOrderText(),
                        Component.translatable("lens.ls_lens.transfer_max").withStyle(ChatFormatting.GRAY).getVisualOrderText()), mx, my);
            }
            y += v.height + GAP;
        }
        for (int i = 0; i < cats.size() && i < 8; i++) if (GuiDraw.in(mx, my, left - 21, top + 9 + i * 18, 16, 16)) hoverCat = cats.get(i);
        super.render(g, mx, my, pt);
        if (hover != null) RecipeDraw.tooltip(g, hover, mx, my);
        else if (!hoverCat.isEmpty()) g.renderTooltip(font, hoverCat, mx, my);
        else {
            for (int i = tabScroll; i < tabs.size() && i < tabScroll + vt; i++) {
                if (GuiDraw.in(mx, my, tx0 + (i - tabScroll) * TAB, top - TAB + 2, TAB - 1, TAB - 2)) {
                    RecipeIndex.Tab tt = tabs.get(i);
                    g.renderTooltip(font, List.of(tt.category().title.getVisualOrderText(),
                            Component.translatable("lens.ls_lens.count", tt.views().size()).withStyle(ChatFormatting.GRAY).getVisualOrderText()), mx, my);
                }
            }
        }
    }

    private void arrowButton(GuiGraphics g, int x, int y, String s, int mx, int my) {
        GuiDraw.button(g, x, y, 12, 12, GuiDraw.in(mx, my, x, y, 12, 12), true);
        g.drawString(font, s, x + 3, y + 2, GuiDraw.TEXT, false);
    }

    // ------------------------------------------------------------------ input

    @Nullable
    private RecipeDraw.Hit hitAt(double mx, double my) {
        if (tabs.isEmpty()) return null;
        RecipeIndex.Tab t = tabs.get(tab);
        int start = pageStarts.get(page), end = page + 1 < pageStarts.size() ? pageStarts.get(page + 1) : t.views().size();
        int y = top + 34;
        for (int i = start; i < end; i++) {
            RecipeView v = t.views().get(i);
            RecipeDraw.Hit hh = RecipeDraw.hit(v, left + (w - v.width) / 2, y, (int) mx, (int) my);
            if (hh != null) return hh;
            y += v.height + GAP;
        }
        return null;
    }

    /** Item or fluid under the mouse (recipe slots, tanks, catalysts). */
    ItemStack hoveredItem(double mx, double my) {
        RecipeDraw.Hit h = hitAt(mx, my);
        if (h != null) return h.stack();
        if (!tabs.isEmpty()) {
            List<ItemStack> cats = tabs.get(tab).category().catalysts();
            for (int i = 0; i < cats.size() && i < 8; i++) if (GuiDraw.in((int) mx, (int) my, left - 21, top + 9 + i * 18, 16, 16)) return cats.get(i);
        }
        return ItemStack.EMPTY;
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (tabs.isEmpty()) return super.mouseClicked(mx, my, button);
        int imx = (int) mx, imy = (int) my;
        int vt = visibleTabs();
        for (int i = tabScroll; i < tabs.size() && i < tabScroll + vt; i++) {
            if (GuiDraw.in(imx, imy, left + 4 + (i - tabScroll) * TAB, top - TAB + 2, TAB - 1, TAB - 2)) {
                tab = i;
                page = 0;
                layout();
                return true;
            }
        }
        if (tabScroll > 0 && GuiDraw.in(imx, imy, left - 14, top - 20, 12, 12)) {
            tabScroll--;
            return true;
        }
        if (tabScroll + vt < tabs.size() && GuiDraw.in(imx, imy, left + w + 2, top - 20, 12, 12)) {
            tabScroll++;
            return true;
        }
        if (pageStarts.size() > 1 && GuiDraw.in(imx, imy, left + w - 52, top + 5, 12, 12)) {
            turn(-1);
            return true;
        }
        if (pageStarts.size() > 1 && GuiDraw.in(imx, imy, left + w - 18, top + 5, 12, 12)) {
            turn(1);
            return true;
        }
        // transfer buttons
        RecipeIndex.Tab t = tabs.get(tab);
        int start = pageStarts.get(page), end = page + 1 < pageStarts.size() ? pageStarts.get(page + 1) : t.views().size();
        int y = top + 34;
        for (int i = start; i < end; i++) {
            RecipeView v = t.views().get(i);
            int x = left + (w - v.width) / 2;
            if (canTransfer(v) && GuiDraw.in(imx, imy, x + v.width - 12, y + v.height - 12, 12, 12)) {
                CompoundTag tag = new CompoundTag();
                tag.putString("Recipe", v.recipeId.toString());
                tag.putBoolean("Max", hasShiftDown());
                CoreNet.sendTag("ls_lens:transfer", tag);
                onClose();
                return true;
            }
            y += v.height + GAP;
        }
        RecipeDraw.Hit h = hitAt(mx, my);
        if (h != null) {
            RecipeIndex.Mode m = button == 1 ? RecipeIndex.Mode.USES : RecipeIndex.Mode.RECIPES;
            open(m, h.stack(), h.fluid(), null);
            return true;
        }
        ItemStack cat = hoveredItem(mx, my);
        if (!cat.isEmpty()) {
            open(button == 1 ? RecipeIndex.Mode.USES : RecipeIndex.Mode.RECIPES, cat, FluidStack.EMPTY, null);
            return true;
        }
        if (button == 1 && !history.isEmpty()) {
            back();
            return true;
        }
        return super.mouseClicked(mx, my, button);
    }

    private void turn(int d) {
        int n = pageStarts.size();
        page = ((page + d) % n + n) % n;
    }

    private void back() {
        if (history.isEmpty()) return;
        state = history.pop();
        load();
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        if (my < top && !tabs.isEmpty()) {
            tab = Math.max(0, Math.min(tabs.size() - 1, tab - (int) Math.signum(delta)));
            page = 0;
            tabScroll = Math.max(0, Math.min(tab, Math.max(tabScroll, tab - visibleTabs() + 1)));
            layout();
        } else if (pageStarts.size() > 1) turn(delta > 0 ? -1 : 1);
        return true;
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (key == GLFW.GLFW_KEY_BACKSPACE) {
            back();
            return true;
        }
        Minecraft mc = Minecraft.getInstance();
        double mx = mc.mouseHandler.xpos() * width / mc.getWindow().getScreenWidth();
        double my = mc.mouseHandler.ypos() * height / mc.getWindow().getScreenHeight();
        RecipeDraw.Hit h = hitAt(mx, my);
        ItemStack item = h != null ? h.stack() : hoveredItem(mx, my);
        FluidStack fluid = h != null ? h.fluid() : FluidStack.EMPTY;
        if (LensClient.is(LensClient.RECIPES, key, scan) && (!item.isEmpty() || !fluid.isEmpty())) {
            open(RecipeIndex.Mode.RECIPES, item, fluid, null);
            return true;
        }
        if (LensClient.is(LensClient.USES, key, scan) && (!item.isEmpty() || !fluid.isEmpty())) {
            open(RecipeIndex.Mode.USES, item, fluid, null);
            return true;
        }
        if (LensClient.is(LensClient.BOOKMARK, key, scan) && !item.isEmpty()) {
            Bookmarks.toggle(item);
            return true;
        }
        if (mc.options.keyInventory.matches(key, scan)) {
            onClose();
            return true;
        }
        if (key == GLFW.GLFW_KEY_LEFT || key == GLFW.GLFW_KEY_PAGE_UP) {
            turn(-1);
            return true;
        }
        if (key == GLFW.GLFW_KEY_RIGHT || key == GLFW.GLFW_KEY_PAGE_DOWN) {
            turn(1);
            return true;
        }
        return super.keyPressed(key, scan, mods);
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
