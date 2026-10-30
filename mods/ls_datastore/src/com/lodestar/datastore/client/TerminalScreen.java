package com.lodestar.datastore.client;

import com.lodestar.core.client.GuiDraw;
import com.lodestar.datastore.DataGrid;
import com.lodestar.datastore.ItemKey;
import com.lodestar.datastore.menu.TerminalMenu;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.fml.ModList;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * The terminal: a searchable, sortable, scrollable grid of everything in the network, drawn from {@link TermClient},
 * with the player's inventory (and a crafting grid on the crafting terminal) below.
 */
public class TerminalScreen extends AbstractContainerScreen<TerminalMenu> {
    private static final int COLS = 9, GX = 8, GY = 18;
    private static String search = "";
    private static int sort, dir;

    private record Entry(ItemKey key, long count, boolean craftable, String name) {}

    private final int rows;
    private EditBox searchBox;
    private List<Entry> view = new ArrayList<>();
    private int builtVersion = -1;
    private String builtSearch = null;
    private int builtSort = -1, builtDir = -1;
    private int scroll;
    private boolean dragging;
    private final Map<ItemKey, String> names = new HashMap<>();

    public TerminalScreen(TerminalMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.rows = TerminalMenu.rows(menu.crafting);
        imageWidth = 195;
        imageHeight = TerminalMenu.invY(menu.crafting) + 82;
        inventoryLabelY = TerminalMenu.invY(menu.crafting) - 11;
    }

    @Override
    protected void init() {
        super.init();
        searchBox = new EditBox(font, leftPos + 96, topPos + 5, 74, 10, Component.translatable("gui.ls_datastore.search"));
        searchBox.setBordered(false);
        searchBox.setMaxLength(64);
        searchBox.setTextColor(0xFFFFFF);
        searchBox.setValue(search);
        searchBox.setHint(Component.translatable("gui.ls_datastore.search").withStyle(ChatFormatting.DARK_GRAY));
        searchBox.setResponder(s -> {
            search = s;
            scroll = 0;
        });
        addRenderableWidget(searchBox);
    }

    // ------------------------------------------------------------------ the item view

    private void rebuild() {
        if (builtVersion == TermClient.version && search.equals(builtSearch) && sort == builtSort && dir == builtDir) return;
        builtVersion = TermClient.version;
        builtSearch = search;
        builtSort = sort;
        builtDir = dir;
        String q = search.trim().toLowerCase(Locale.ROOT);
        List<Entry> l = new ArrayList<>();
        for (Map.Entry<ItemKey, Long> e : TermClient.items.entrySet()) {
            ItemKey k = e.getKey();
            String name = names.computeIfAbsent(k, kk -> kk.proto().getHoverName().getString());
            if (!q.isEmpty() && !matches(k, name, q)) continue;
            l.add(new Entry(k, e.getValue(), TermClient.craftable.contains(k), name));
        }
        Comparator<Entry> byName = Comparator.comparing(e -> e.name.toLowerCase(Locale.ROOT));
        Comparator<Entry> c = switch (sort) {
            case 1 -> Comparator.comparingLong((Entry e) -> e.count).reversed().thenComparing(byName);
            case 2 -> Comparator.comparing((Entry e) -> ns(e.key)).thenComparing(byName);
            default -> byName;
        };
        if (dir == 1) c = c.reversed();
        l.sort(c);
        view = l;
        clampScroll();
    }

    private static String ns(ItemKey k) {
        var id = net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(k.item);
        return id == null ? "" : id.getNamespace();
    }

    private static boolean matches(ItemKey k, String name, String q) {
        if (q.startsWith("@")) {
            String m = q.substring(1);
            String n = ns(k);
            if (n.contains(m)) return true;
            return ModList.get().getModContainerById(n).map(mc -> mc.getModInfo().getDisplayName().toLowerCase(Locale.ROOT).contains(m)).orElse(false);
        }
        if (q.startsWith("#")) {
            String t = q.substring(1);
            return k.proto().getTags().anyMatch(tag -> tag.location().toString().contains(t));
        }
        return name.toLowerCase(Locale.ROOT).contains(q);
    }

    private int maxScroll() {
        return Math.max(0, (view.size() + COLS - 1) / COLS - rows);
    }

    private void clampScroll() {
        scroll = Math.max(0, Math.min(scroll, maxScroll()));
    }

    @Nullable
    private Entry entryAt(double mx, double my) {
        int x = (int) mx - leftPos - GX, y = (int) my - topPos - GY;
        if (x < 0 || y < 0 || x >= COLS * 18 || y >= rows * 18) return null;
        int i = (scroll + y / 18) * COLS + x / 18;
        return i >= 0 && i < view.size() ? view.get(i) : null;
    }

    private boolean inGrid(double mx, double my) {
        return GuiDraw.in((int) mx, (int) my, leftPos + GX, topPos + GY, COLS * 18, rows * 18);
    }

    // ------------------------------------------------------------------ drawing

    private int sbX() {
        return leftPos + 175;
    }

    @Override
    protected void renderBg(GuiGraphics g, float pt, int mx, int my) {
        rebuild();
        GuiDraw.panel(g, leftPos, topPos, imageWidth, imageHeight);
        GuiDraw.inset(g, leftPos + 94, topPos + 3, 78, 12);
        g.fill(leftPos + GX - 1, topPos + GY - 1, leftPos + GX + COLS * 18 - 1, topPos + GY + rows * 18 - 1, 0xFF8B8B8B);
        for (int r = 0; r < rows; r++)
            for (int c = 0; c < COLS; c++) GuiDraw.slot(g, leftPos + GX + c * 18, topPos + GY + r * 18);
        GuiDraw.scrollbar(g, sbX(), topPos + GY, 12, rows * 18 - 2, maxScroll() == 0 ? 0 : (float) scroll / maxScroll(),
                view.isEmpty() ? 1f : Math.min(1f, (float) rows * COLS / view.size()));
        for (Slot s : menu.slots) GuiDraw.slot(g, leftPos + s.x, topPos + s.y);
        if (menu.crafting) {
            int gy = topPos + TerminalMenu.gridY(true);
            GuiDraw.arrow(g, leftPos + 88, gy + 18, 0);
            GuiDraw.bigSlot(g, leftPos + 120, gy + 18);
            boolean hov = GuiDraw.in(mx, my, leftPos + 84, gy - 1, 11, 11);
            GuiDraw.button(g, leftPos + 84, gy - 1, 11, 11, hov, true);
            g.drawString(font, "x", leftPos + 87, gy, 0xFF404040, false);
        }
        // side tabs: sort mode, direction, jobs
        for (int i = 0; i < 3; i++) {
            int x = leftPos - 22, y = topPos + 4 + i * 22;
            GuiDraw.tab(g, x, y, 22, 20, 0xFF3FD0C9);
            boolean hov = GuiDraw.in(mx, my, x, y, 22, 20);
            if (hov) g.fill(x + 3, y + 5, x + 19, y + 17, 0x40FFFFFF);
        }
        String sortLabel = sort == 0 ? "Az" : sort == 1 ? "#" : "@";
        g.drawString(font, sortLabel, leftPos - 11 - font.width(sortLabel) / 2, topPos + 10, 0xFF303030, false);
        String dirLabel = dir == 0 ? "▲" : "▼";
        g.drawString(font, dirLabel, leftPos - 11 - font.width(dirLabel) / 2, topPos + 32, 0xFF303030, false);
        g.renderItem(new ItemStack(Items.CRAFTING_TABLE), leftPos - 19, topPos + 51);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mx, int my) {
        g.drawString(font, font.plainSubstrByWidth(title.getString(), 84), 8, 6, GuiDraw.TEXT, false);
        g.drawString(font, playerInventoryTitle, 8, inventoryLabelY, GuiDraw.TEXT, false);
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        renderBackground(g);
        super.render(g, mx, my, pt);
        int start = scroll * COLS;
        for (int i = 0; i < rows * COLS && start + i < view.size(); i++) {
            Entry e = view.get(start + i);
            int x = leftPos + GX + (i % COLS) * 18, y = topPos + GY + (i / COLS) * 18;
            g.renderItem(e.key.proto(), x, y);
            g.pose().pushPose();
            g.pose().translate(0, 0, 200);
            g.pose().scale(0.5f, 0.5f, 1f);
            String s = e.count > 0 ? TermClient.fmt(e.count) : Component.translatable("gui.ls_datastore.craft").getString();
            int color = e.count > 0 ? 0xFFFFFF : 0xFFE8C860;
            g.drawString(font, s, (x + 16) * 2 - font.width(s), (y + 12) * 2, color, true);
            g.pose().popPose();
        }
        if (TermClient.status != DataGrid.ONLINE) {
            int x0 = leftPos + GX, y0 = topPos + GY, w = COLS * 18 - 2, h = rows * 18 - 2;
            g.pose().pushPose();
            g.pose().translate(0, 0, 250);
            g.fill(x0, y0, x0 + w, y0 + h, 0xC0101418);
            GuiDraw.wrapped(g, font, Component.translatable("gui.ls_datastore.status." + TermClient.status), x0 + 8, y0 + h / 2 - 8, w - 16, 0xFFE06060);
            g.pose().popPose();
        }
        Entry hov = TermClient.status == DataGrid.ONLINE ? entryAt(mx, my) : null;
        if (hov != null) {
            int i = view.indexOf(hov) - start;
            int x = leftPos + GX + (i % COLS) * 18, y = topPos + GY + (i / COLS) * 18;
            renderSlotHighlight(g, x, y, 0);
            if (menu.getCarried().isEmpty()) {
                List<Component> tip = new ArrayList<>(Screen.getTooltipFromItem(minecraft, hov.key.proto()));
                tip.add(Component.translatable("gui.ls_datastore.stored", String.format("%,d", hov.count)).withStyle(ChatFormatting.AQUA));
                if (hov.craftable) tip.add(Component.translatable("gui.ls_datastore.craftable").withStyle(ChatFormatting.GOLD));
                g.renderComponentTooltip(font, tip, mx, my);
            }
        } else {
            for (int i = 0; i < 3; i++) {
                if (!GuiDraw.in(mx, my, leftPos - 22, topPos + 4 + i * 22, 22, 20)) continue;
                Component c = i == 0 ? Component.translatable("gui.ls_datastore.sort." + sort) : i == 1 ? Component.translatable("gui.ls_datastore.dir." + dir)
                        : Component.translatable("gui.ls_datastore.show_jobs");
                g.renderTooltip(font, c, mx, my);
            }
            if (menu.crafting && GuiDraw.in(mx, my, leftPos + 84, topPos + TerminalMenu.gridY(true) - 1, 11, 11)) {
                g.renderTooltip(font, Component.translatable("gui.ls_datastore.clear"), mx, my);
            }
            renderTooltip(g, mx, my);
        }
    }

    // ------------------------------------------------------------------ input

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (searchBox.isMouseOver(mx, my) && button == 1) {
            searchBox.setValue("");
            return true;
        }
        for (int i = 0; i < 3; i++) {
            if (!GuiDraw.in((int) mx, (int) my, leftPos - 22, topPos + 4 + i * 22, 22, 20)) continue;
            if (i == 0) sort = (sort + 1) % 3;
            else if (i == 1) dir = 1 - dir;
            else minecraft.setScreen(new JobsScreen(this));
            return true;
        }
        if (menu.crafting && GuiDraw.in((int) mx, (int) my, leftPos + 84, topPos + TerminalMenu.gridY(true) - 1, 11, 11)) {
            TermClient.send(TerminalMenu.A_CLEAR, null, 0, 0);
            return true;
        }
        if (GuiDraw.in((int) mx, (int) my, sbX(), topPos + GY, 12, rows * 18)) {
            dragging = true;
            dragTo(my);
            return true;
        }
        if (inGrid(mx, my) && TermClient.status == DataGrid.ONLINE) {
            if (!menu.getCarried().isEmpty()) {
                TermClient.send(TerminalMenu.A_INSERT, null, button == 1 ? 1 : 0, 0);
                return true;
            }
            Entry e = entryAt(mx, my);
            if (e == null) return true;
            if (e.craftable && (hasControlDown() || button == 2 || e.count == 0)) {
                minecraft.setScreen(new AmountScreen(this, e.key));
            } else if (hasShiftDown()) {
                TermClient.send(TerminalMenu.A_TAKE_SHIFT, e.key, 0, 0);
            } else {
                TermClient.send(TerminalMenu.A_TAKE, e.key, button == 1 ? 1 : 0, 0);
            }
            return true;
        }
        return super.mouseClicked(mx, my, button);
    }

    private void dragTo(double my) {
        float f = (float) (my - topPos - GY - 6) / (rows * 18 - 14);
        scroll = Math.round(Math.max(0, Math.min(1, f)) * maxScroll());
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (dragging) {
            dragTo(my);
            return true;
        }
        return super.mouseDragged(mx, my, button, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        dragging = false;
        return super.mouseReleased(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        if (inGrid(mx, my) || GuiDraw.in((int) mx, (int) my, sbX(), topPos + GY, 12, rows * 18)) {
            scroll -= (int) Math.signum(delta);
            clampScroll();
            return true;
        }
        return super.mouseScrolled(mx, my, delta);
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (key == 256) return super.keyPressed(key, scan, mods);
        if (searchBox.isFocused()) {
            searchBox.keyPressed(key, scan, mods);
            return true;
        }
        return super.keyPressed(key, scan, mods);
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        searchBox.tick();
    }
}
