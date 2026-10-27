package com.lodestar.lens.client;

import com.lodestar.core.client.ExtraAreas;
import com.lodestar.core.client.GuiDraw;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.recipebook.RecipeUpdateListener;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.fluids.FluidStack;
import org.lwjgl.glfw.GLFW;

import javax.annotation.Nullable;
import java.util.List;

/**
 * The item list beside every inventory screen (right), bookmarks (left) and the search box. Left-click an item for
 * its recipes, right-click for its uses; in creative, shift-click takes a stack (shift-right-click takes one).
 */
public final class Overlay {
    private Overlay() {}

    private static final int CELL = 18;
    static String query = "";
    static boolean hidden;
    private static int page, bpage;
    @Nullable
    private static AbstractContainerScreen<?> screen;
    @Nullable
    private static EditBox search;
    // item grid
    private static int gx, gy, cols, rows;
    // bookmarks
    private static int bx, by, bcols, brows;
    private static boolean bookmarksShown;

    private static Font font() {
        return Minecraft.getInstance().font;
    }

    public static void onInit(ScreenEvent.Init.Post e) {
        if (!(e.getScreen() instanceof AbstractContainerScreen<?> s)) {
            screen = null;
            search = null;
            return;
        }
        screen = s;
        layout(s);
        int sw = Math.max(60, cols * CELL);
        search = new EditBox(font(), gx + 1, s.height - 19, sw - 2, 14, Component.translatable("lens.ls_lens.search"));
        search.setMaxLength(64);
        search.setValue(query);
        search.setHint(Component.translatable("lens.ls_lens.search_hint").withStyle(ChatFormatting.DARK_GRAY));
        search.setResponder(v -> {
            query = v;
            page = 0;
        });
        search.visible = !hidden && cols >= 3;
        e.addListener(search);
    }

    private static void layout(AbstractContainerScreen<?> s) {
        int right = s.getGuiLeft() + s.getXSize();
        if (s instanceof ExtraAreas ea) for (Rect2i r : ea.extraAreas()) right = Math.max(right, r.getX() + r.getWidth());
        gx = right + 6;
        cols = Math.max(0, (s.width - gx - 4) / CELL);
        gx = s.width - 4 - cols * CELL;
        gy = 20;
        rows = Math.max(0, (s.height - gy - 24) / CELL);
        boolean book = s instanceof RecipeUpdateListener rul && rul.getRecipeBookComponent().isVisible();
        bx = 4;
        bcols = book ? 0 : Math.max(0, (s.getGuiLeft() - 10) / CELL);
        by = 20;
        brows = Math.max(0, (s.height - by - 8) / CELL);
        bookmarksShown = bcols >= 1 && !Bookmarks.list().isEmpty();
    }

    private static boolean active() {
        return screen != null && Minecraft.getInstance().screen == screen && !hidden && cols >= 3 && rows >= 1;
    }

    private static List<ItemStack> items() {
        return ItemList.search(query);
    }

    private static int pages() {
        int per = cols * rows;
        return per <= 0 ? 1 : Math.max(1, (items().size() + per - 1) / per);
    }

    private static int bpages() {
        int per = bcols * brows;
        return per <= 0 ? 1 : Math.max(1, (Bookmarks.list().size() + per - 1) / per);
    }

    /** Item under the mouse in the list or the bookmarks, or EMPTY. */
    static ItemStack itemAt(double mx, double my) {
        if (!active()) return ItemStack.EMPTY;
        if (mx >= gx && my >= gy && mx < gx + cols * CELL && my < gy + rows * CELL) {
            int c = (int) ((mx - gx) / CELL), r = (int) ((my - gy) / CELL);
            int i = page * cols * rows + r * cols + c;
            List<ItemStack> l = items();
            return i < l.size() ? l.get(i) : ItemStack.EMPTY;
        }
        if (bookmarksShown && mx >= bx && my >= by && mx < bx + bcols * CELL && my < by + brows * CELL) {
            int c = (int) ((mx - bx) / CELL), r = (int) ((my - by) / CELL);
            int i = bpage * bcols * brows + r * bcols + c;
            List<ItemStack> l = Bookmarks.list();
            return i < l.size() ? l.get(i) : ItemStack.EMPTY;
        }
        return ItemStack.EMPTY;
    }

    public static void onRender(ScreenEvent.Render.Post e) {
        if (e.getScreen() != screen) return;
        if (search != null) search.visible = !hidden && cols >= 3;
        if (!active()) return;
        layout(screen);
        GuiGraphics g = e.getGuiGraphics();
        int mx = e.getMouseX(), my = e.getMouseY();
        page = Math.min(page, pages() - 1);
        // page bar
        String pg = (page + 1) + " / " + pages();
        int cx = gx + cols * CELL / 2;
        g.drawString(font(), pg, cx - font().width(pg) / 2, 6, 0xFFE0E0E0, true);
        nav(g, gx, 4, "<", mx, my);
        nav(g, gx + cols * CELL - 12, 4, ">", mx, my);
        List<ItemStack> l = items();
        int start = page * cols * rows;
        for (int i = 0; i < cols * rows && start + i < l.size(); i++) {
            int x = gx + (i % cols) * CELL + 1, y = gy + (i / cols) * CELL + 1;
            g.renderItem(l.get(start + i), x, y);
        }
        if (bookmarksShown) {
            bpage = Math.min(bpage, bpages() - 1);
            List<ItemStack> b = Bookmarks.list();
            int bs = bpage * bcols * brows;
            for (int i = 0; i < bcols * brows && bs + i < b.size(); i++) {
                int x = bx + (i % bcols) * CELL + 1, y = by + (i / bcols) * CELL + 1;
                g.renderItem(b.get(bs + i), x, y);
            }
            if (bpages() > 1) {
                String bp = (bpage + 1) + "/" + bpages();
                g.drawString(font(), bp, bx, 6, 0xFFE0E0E0, true);
            }
        }
        ItemStack hov = itemAt(mx, my);
        if (!hov.isEmpty()) {
            int x = mx >= gx ? gx + (int) ((mx - gx) / CELL) * CELL : bx + (int) ((mx - bx) / CELL) * CELL;
            int y = mx >= gx ? gy + (int) ((my - gy) / CELL) * CELL : by + (int) ((my - by) / CELL) * CELL;
            g.fill(x + 1, y + 1, x + 17, y + 17, 0x80FFFFFF);
            List<Component> tip = Screen.getTooltipFromItem(Minecraft.getInstance(), hov);
            tip.add(Component.translatable("lens.ls_lens.hint").withStyle(ChatFormatting.DARK_GRAY));
            g.renderComponentTooltip(font(), tip, mx, my);
        } else if (GuiDraw.in(mx, my, gx, 4, 12, 12) || GuiDraw.in(mx, my, gx + cols * CELL - 12, 4, 12, 12)) {
            g.renderTooltip(font(), Component.translatable("lens.ls_lens.pages"), mx, my);
        }
    }

    private static void nav(GuiGraphics g, int x, int y, String s, int mx, int my) {
        GuiDraw.button(g, x, y, 12, 12, GuiDraw.in(mx, my, x, y, 12, 12), true);
        g.drawString(font(), s, x + 3, y + 2, GuiDraw.TEXT, false);
    }

    public static void onMouse(ScreenEvent.MouseButtonPressed.Pre e) {
        if (e.getScreen() != screen || screen == null) return;
        double mx = e.getMouseX(), my = e.getMouseY();
        if (search != null && search.visible) {
            boolean inBox = mx >= search.getX() && my >= search.getY() && mx < search.getX() + search.getWidth() && my < search.getY() + search.getHeight();
            if (inBox && e.getButton() == 1) {
                search.setValue("");
                screen.setFocused(search);
                search.setFocused(true);
                e.setCanceled(true);
                return;
            }
            if (!inBox && search.isFocused()) {
                search.setFocused(false);
                if (screen.getFocused() == search) screen.setFocused(null);
            }
        }
        if (!active()) return;
        if (GuiDraw.in((int) mx, (int) my, gx, 4, 12, 12)) {
            page = (page - 1 + pages()) % pages();
            e.setCanceled(true);
            return;
        }
        if (GuiDraw.in((int) mx, (int) my, gx + cols * CELL - 12, 4, 12, 12)) {
            page = (page + 1) % pages();
            e.setCanceled(true);
            return;
        }
        ItemStack s = itemAt(mx, my);
        if (s.isEmpty()) return;
        e.setCanceled(true);
        Minecraft mc = Minecraft.getInstance();
        if (Screen.hasShiftDown() && mc.player != null && mc.player.isCreative()) {
            give(s.copyWithCount(e.getButton() == 1 ? 1 : s.getMaxStackSize()));
            return;
        }
        LensScreen.open(e.getButton() == 1 ? RecipeIndex.Mode.USES : RecipeIndex.Mode.RECIPES, s, FluidStack.EMPTY, null);
    }

    /** Creative only: put a stack into the first free inventory slot (the server accepts creative slot packets). */
    private static void give(ItemStack stack) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.gameMode == null) return;
        var inv = mc.player.getInventory();
        int free = inv.getFreeSlot();
        if (free < 0) return;
        inv.setItem(free, stack);
        int menuSlot = free < 9 ? free + InventoryMenu.USE_ROW_SLOT_START : free;
        mc.gameMode.handleCreativeModeItemAdd(stack, menuSlot);
    }

    public static void onScroll(ScreenEvent.MouseScrolled.Pre e) {
        if (e.getScreen() != screen || !active()) return;
        double mx = e.getMouseX(), my = e.getMouseY();
        if (mx >= gx && my >= gy - 16 && mx < gx + cols * CELL && my < gy + rows * CELL) {
            int n = pages();
            page = ((page - (int) Math.signum(e.getScrollDelta())) % n + n) % n;
            e.setCanceled(true);
        } else if (bookmarksShown && mx >= bx && mx < bx + bcols * CELL && my >= by) {
            int n = bpages();
            bpage = ((bpage - (int) Math.signum(e.getScrollDelta())) % n + n) % n;
            e.setCanceled(true);
        }
    }

    public static void onKey(ScreenEvent.KeyPressed.Pre e) {
        if (e.getScreen() != screen || screen == null) return;
        int key = e.getKeyCode(), scan = e.getScanCode();
        if (search != null && search.isFocused() && search.visible) {
            if (key == GLFW.GLFW_KEY_ESCAPE || key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) {
                search.setFocused(false);
                screen.setFocused(null);
            } else {
                search.keyPressed(key, scan, e.getModifiers());
            }
            e.setCanceled(true);
            return;
        }
        if (screen.getFocused() instanceof EditBox) return; // another text field (creative search) is being typed in
        if (LensClient.is(LensClient.TOGGLE, key, scan)) {
            hidden = !hidden;
            e.setCanceled(true);
            return;
        }
        if (LensClient.is(LensClient.FOCUS, key, scan) && search != null && !hidden) {
            screen.setFocused(search);
            search.setFocused(true);
            e.setCanceled(true);
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        double mx = mc.mouseHandler.xpos() * screen.width / mc.getWindow().getScreenWidth();
        double my = mc.mouseHandler.ypos() * screen.height / mc.getWindow().getScreenHeight();
        ItemStack s = itemAt(mx, my);
        if (s.isEmpty()) {
            Slot slot = screen.getSlotUnderMouse();
            if (slot != null) s = slot.getItem();
        }
        if (s.isEmpty()) return;
        if (LensClient.is(LensClient.RECIPES, key, scan)) {
            LensScreen.open(RecipeIndex.Mode.RECIPES, s, FluidStack.EMPTY, null);
            e.setCanceled(true);
        } else if (LensClient.is(LensClient.USES, key, scan)) {
            LensScreen.open(RecipeIndex.Mode.USES, s, FluidStack.EMPTY, null);
            e.setCanceled(true);
        } else if (LensClient.is(LensClient.BOOKMARK, key, scan)) {
            Bookmarks.toggle(s);
            e.setCanceled(true);
        }
    }
}
