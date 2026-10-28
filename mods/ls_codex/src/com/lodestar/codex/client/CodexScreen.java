package com.lodestar.codex.client;

import com.lodestar.codex.CodexChapter;
import com.lodestar.core.client.GuiDraw;
import com.lodestar.core.client.LensBridge;
import com.lodestar.core.client.RecipeDraw;
import com.lodestar.core.recipe.display.RecipeView;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.ChatFormatting;
import net.minecraft.SharedConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

import javax.annotation.Nullable;
import java.io.InputStream;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.DoubleConsumer;
import java.util.function.IntConsumer;

/**
 * The Field Codex: a two-page book. The index (chapter list grouped by creative tab, search) and a chapter's contents
 * and pages; pages show word-wrapped text, the items they are about, an inline recipe or a turning 3D structure with
 * a layer slider and bill of materials. Reading progress and carried items come from {@link CodexProgress}.
 */
public class CodexScreen extends Screen {
    private static final ResourceLocation PAPER = new ResourceLocation("ls_codex", "textures/gui/paper.png");
    private static final ResourceLocation LEATHER = new ResourceLocation("ls_codex", "textures/gui/leather.png");
    static final int INK = 0xFF3B2A1A, HEAD = 0xFF6B2D0F, FAINT = 0xFF8A7556, LINK = 0xFF1F5FA0, MARK = 0xFFC0392B;
    static final int HOVER = 0x26704020, FRAME = 0xFFB59E78, FRAME_FILL = 0xFFE4D5B0, GOLD = 0xFFD9A93C;
    private static final int LINE = 10, GAP = 5;

    enum Mode { HOME, CHAPTER, PAGE }

    record State(Mode mode, @Nullable CodexBook.Chapter chapter, int page) {}

    private record Click(int x, int y, int w, int h, IntConsumer action) {}

    private record Scroll(int x, int y, int w, int h, DoubleConsumer action) {}

    /** A row of the index: a tab header, a chapter, or a search hit. */
    private record Row(int kind, String tab, @Nullable CodexBook.Chapter chapter, int page, String label, ItemStack icon) {}

    // where the reader was when the book was last closed, restored on the next open
    private static String lastMod;
    private static int lastPage = -1;
    private static Mode lastMode = Mode.HOME;
    private static String lastSearch = "";
    private static final Map<ResourceLocation, int[]> IMAGE_SIZES = new HashMap<>();

    @Nullable
    private final Screen parent;
    private State state;
    private final Deque<State> history = new ArrayDeque<>();
    private final RecipeFinder finder = new RecipeFinder();

    private int bx, by, bw, bh, lx, rx, pw, py, ph, footY;
    private EditBox search;
    private List<Row> rows = List.of();
    private int listScroll, contentsScroll;

    private List<FormattedCharSequence> lines = List.of();
    private boolean linesDirty = true;
    private int textScroll, textMaxScroll;
    private ItemStack focus = ItemStack.EMPTY;
    private int recipeIndex;
    @Nullable
    private StructurePreview structure;
    private float yaw = -35, pitch = 28, zoom = 1f;
    private int maxLayer = -1;
    private long lastTouch, lastFrame;
    private boolean dragging, sliding;
    private int sliderY, sliderH;

    private final List<Click> clicks = new ArrayList<>();
    private final List<Scroll> scrolls = new ArrayList<>();
    private ItemStack hoverStack = ItemStack.EMPTY;
    private final List<Component> hoverExtra = new ArrayList<>();
    @Nullable
    private RecipeDraw.Hit hoverTank;
    @Nullable
    private Component hoverText;
    private double clickX, clickY;

    public CodexScreen(@Nullable Screen parent, State initial) {
        super(Component.translatable("gui.ls_codex.title"));
        this.parent = parent;
        this.state = initial;
    }

    // ------------------------------------------------------------------ opening

    /** Opens the Codex where the reader left it. */
    public static void open(@Nullable Screen parent) {
        State s = new State(Mode.HOME, null, -1);
        CodexBook.Chapter c = lastMod == null ? null : CodexBook.chapter(lastMod);
        if (c != null && lastMode != Mode.HOME) {
            s = lastMode == Mode.PAGE && lastPage >= 0 && lastPage < c.pageCount() ? new State(Mode.PAGE, c, lastPage) : new State(Mode.CHAPTER, c, -1);
        }
        Minecraft.getInstance().setScreen(new CodexScreen(parent, s));
    }

    /** Opens the page that describes an item; false when no page lists it. */
    public static boolean openFor(Item item, @Nullable Screen parent) {
        CodexBook.Ref ref = CodexBook.find(item);
        if (ref == null) return false;
        State s = ref.page() < 0 ? new State(Mode.CHAPTER, ref.chapter(), -1) : new State(Mode.PAGE, ref.chapter(), ref.page());
        CodexScreen screen = new CodexScreen(parent, s);
        screen.focus = new ItemStack(item);
        screen.keepFocus = true;
        Minecraft.getInstance().setScreen(screen);
        return true;
    }

    private boolean keepFocus;

    @Override
    protected void init() {
        bw = Math.min(384, width - 16) & ~1;
        bh = Math.min(236, height - 30);
        bx = (width - bw) / 2;
        by = (height - bh) / 2 + 6;
        int inner = 12;
        pw = (bw - 2 * inner - 16) / 2;
        lx = bx + inner;
        rx = bx + bw - inner - pw;
        py = by + 10;
        footY = by + bh - 17;
        ph = footY - 4 - py;
        search = new EditBox(font, lx + 1, py + 26, pw - 2, 12, Component.translatable("gui.ls_codex.search"));
        search.setHint(Component.translatable("gui.ls_codex.search").withStyle(ChatFormatting.DARK_GRAY));
        search.setMaxLength(60);
        search.setValue(lastSearch);
        search.setResponder(s -> {
            lastSearch = s;
            listScroll = 0;
            rebuildRows();
        });
        addRenderableWidget(search);
        linesDirty = true;
        enter(state, false);
    }

    // ------------------------------------------------------------------ navigation

    private void go(State s) {
        if (state != null) history.push(state);
        enter(s, true);
    }

    private void enter(State s, boolean sound) {
        boolean pageChanged = state == null || state.mode != s.mode || state.chapter != s.chapter || state.page != s.page;
        state = s;
        if (pageChanged || rows.isEmpty()) {
            textScroll = 0;
            contentsScroll = 0;
            linesDirty = true;
            if (!keepFocus) focus = ItemStack.EMPTY;
            keepFocus = false;
            recipeIndex = 0;
            structure = null;
            maxLayer = -1;
            zoom = 1f;
            yaw = -35;
            pitch = 28;
            if (s.mode == Mode.PAGE && s.chapter != null) {
                CodexChapter.Page p = s.chapter.page(s.page);
                if (focus.isEmpty() && !s.chapter.pageItems.get(s.page).isEmpty()) focus = s.chapter.pageItems.get(s.page).get(0);
                if (p.multiblock() != null) {
                    structure = StructurePreview.of(p.multiblock());
                    if (structure != null) maxLayer = structure.layers() - 1;
                }
                CodexProgress.markRead(s.chapter, s.page);
            }
            if (sound && minecraft != null) minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.BOOK_PAGE_TURN, 1.0f));
        }
        lastMode = s.mode;
        lastMod = s.chapter == null ? null : s.chapter.data.mod;
        lastPage = s.page;
        if (search != null) {
            search.setVisible(s.mode == Mode.HOME);
            if (s.mode != Mode.HOME) search.setFocused(false);
        }
        rebuildRows();
    }

    private void back() {
        if (!history.isEmpty()) {
            enter(history.pop(), true);
        } else if (state.mode == Mode.PAGE) {
            enter(new State(Mode.CHAPTER, state.chapter, -1), true);
        } else if (state.mode == Mode.CHAPTER) {
            enter(new State(Mode.HOME, null, -1), true);
        }
    }

    private int chapterIndex(CodexBook.Chapter c) {
        return CodexBook.chapters().indexOf(c);
    }

    @Nullable
    private State prevState() {
        List<CodexBook.Chapter> all = CodexBook.chapters();
        if (state.mode == Mode.PAGE) {
            return state.page > 0 ? new State(Mode.PAGE, state.chapter, state.page - 1) : new State(Mode.CHAPTER, state.chapter, -1);
        }
        if (state.mode == Mode.CHAPTER) {
            int i = chapterIndex(state.chapter);
            if (i > 0) {
                CodexBook.Chapter p = all.get(i - 1);
                return new State(Mode.PAGE, p, p.pageCount() - 1);
            }
            return new State(Mode.HOME, null, -1);
        }
        return null;
    }

    @Nullable
    private State nextState() {
        List<CodexBook.Chapter> all = CodexBook.chapters();
        if (state.mode == Mode.PAGE) {
            if (state.page < state.chapter.pageCount() - 1) return new State(Mode.PAGE, state.chapter, state.page + 1);
            int i = chapterIndex(state.chapter);
            return i >= 0 && i < all.size() - 1 ? new State(Mode.CHAPTER, all.get(i + 1), -1) : null;
        }
        if (state.mode == Mode.CHAPTER) return new State(Mode.PAGE, state.chapter, 0);
        return null;
    }

    private void openPage(CodexBook.Chapter c, int page) {
        go(page < 0 ? new State(Mode.CHAPTER, c, -1) : new State(Mode.PAGE, c, page));
    }

    /** Follows an item link: the page describing it, unless that is the page already open. */
    private boolean followItem(ItemStack stack) {
        CodexBook.Ref ref = CodexBook.find(stack.getItem());
        if (ref == null || (ref.chapter() == state.chapter && ref.page() == state.page)) return false;
        keepFocus = true;
        focus = stack.copyWithCount(1);
        openPage(ref.chapter(), ref.page());
        return true;
    }

    private void rebuildRows() {
        List<Row> out = new ArrayList<>();
        String q = search == null ? lastSearch : search.getValue();
        if (!q.isBlank()) {
            for (CodexBook.Hit h : CodexBook.search(q)) out.add(new Row(2, h.chapter().data.tab, h.chapter(), h.page(), h.label(), h.icon()));
        } else {
            String tab = null;
            for (CodexBook.Chapter c : CodexBook.chapters()) {
                if (!c.data.tab.equals(tab)) {
                    tab = c.data.tab;
                    out.add(new Row(0, tab, null, -1, Component.translatable("gui.ls_codex.tab." + tab.toLowerCase(java.util.Locale.ROOT)).getString(), ItemStack.EMPTY));
                }
                out.add(new Row(1, tab, c, -1, c.name(), c.icon));
            }
        }
        rows = out;
    }

    // ------------------------------------------------------------------ input

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        boolean inSearch = search.isVisible() && GuiDraw.in((int) mx, (int) my, search.getX(), search.getY(), search.getWidth(), search.getHeight());
        if (!inSearch && search.isFocused()) {
            search.setFocused(false);
            setFocused(null);
        }
        if (inSearch && button == 1) {
            search.setValue("");
            return true;
        }
        if (super.mouseClicked(mx, my, button)) return true;
        clickX = mx;
        clickY = my;
        for (int i = clicks.size() - 1; i >= 0; i--) {
            Click c = clicks.get(i);
            if (GuiDraw.in((int) mx, (int) my, c.x, c.y, c.w, c.h)) {
                c.action.accept(button);
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (dragging) {
            yaw += (float) dx * 2.5f;
            pitch = Mth.clamp(pitch + (float) dy * 2.5f, -15f, 89f);
            lastTouch = System.currentTimeMillis();
            return true;
        }
        if (sliding) {
            slide(my);
            return true;
        }
        return super.mouseDragged(mx, my, button, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        dragging = false;
        sliding = false;
        return super.mouseReleased(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        for (int i = scrolls.size() - 1; i >= 0; i--) {
            Scroll s = scrolls.get(i);
            if (GuiDraw.in((int) mx, (int) my, s.x, s.y, s.w, s.h)) {
                s.action.accept(delta);
                return true;
            }
        }
        return super.mouseScrolled(mx, my, delta);
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (search.isFocused()) {
            if (key == GLFW.GLFW_KEY_ESCAPE) {
                search.setFocused(false);
                setFocused(null);
                return true;
            }
            return super.keyPressed(key, scan, mods);
        }
        switch (key) {
            case GLFW.GLFW_KEY_LEFT, GLFW.GLFW_KEY_PAGE_UP -> {
                State p = prevState();
                if (p != null) enter(p, true);
                return true;
            }
            case GLFW.GLFW_KEY_RIGHT, GLFW.GLFW_KEY_PAGE_DOWN -> {
                State n = nextState();
                if (n != null) enter(n, true);
                return true;
            }
            case GLFW.GLFW_KEY_BACKSPACE -> {
                back();
                return true;
            }
            default -> {
                return super.keyPressed(key, scan, mods);
            }
        }
    }

    @Override
    public boolean charTyped(char c, int mods) {
        if (state.mode == Mode.HOME && !search.isFocused() && SharedConstants.isAllowedChatCharacter(c) && c != ' ') {
            setFocused(search);
            search.setFocused(true);
            search.insertText(String.valueOf(c));
            return true;
        }
        return super.charTyped(c, mods);
    }

    @Override
    public void onClose() {
        CodexProgress.save();
        if (minecraft != null) minecraft.setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    // ------------------------------------------------------------------ rendering

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        clicks.clear();
        scrolls.clear();
        hoverStack = ItemStack.EMPTY;
        hoverExtra.clear();
        hoverTank = null;
        hoverText = null;
        renderBackground(g);
        drawBook(g, mx, my);
        switch (state.mode) {
            case HOME -> renderHome(g, mx, my);
            case CHAPTER -> renderChapter(g, mx, my);
            case PAGE -> renderPage(g, mx, my);
        }
        drawFooter(g, mx, my);
        super.render(g, mx, my, pt);
        if (!hoverStack.isEmpty()) {
            List<Component> tip = new ArrayList<>(getTooltipFromItem(minecraft, hoverStack));
            tip.addAll(hoverExtra);
            g.renderComponentTooltip(font, tip, mx, my);
        } else if (hoverTank != null) {
            RecipeDraw.tooltip(g, hoverTank, mx, my);
        } else if (hoverText != null) {
            g.renderTooltip(font, font.split(hoverText, 180), mx, my);
        }
    }

    private void tile(GuiGraphics g, ResourceLocation tex, int x, int y, int w, int h) {
        for (int ty = 0; ty < h; ty += 64) {
            for (int tx = 0; tx < w; tx += 64) {
                g.blit(tex, x + tx, y + ty, 0, 0, Math.min(64, w - tx), Math.min(64, h - ty), 64, 64);
            }
        }
    }

    private void drawBook(GuiGraphics g, int mx, int my) {
        // ribbon tabs behind the top edge: Index, and the open chapter's contents
        int tx = bx + 18;
        tx = ribbon(g, mx, my, tx, Component.translatable("gui.ls_codex.index"), 0xFF8E2F23, state.mode == Mode.HOME,
                () -> go(new State(Mode.HOME, null, -1)));
        if (state.chapter != null) {
            ribbon(g, mx, my, tx, Component.translatable("gui.ls_codex.contents"), 0xFF2F5D50, state.mode == Mode.CHAPTER,
                    () -> go(new State(Mode.CHAPTER, state.chapter, -1)));
        }
        // cover
        g.fill(bx - 7, by - 7, bx + bw + 7, by + bh + 7, 0xFF20140B);
        tile(g, LEATHER, bx - 6, by - 6, bw + 12, bh + 12);
        g.fill(bx - 6, by - 6, bx + bw + 6, by - 5, 0x40FFFFFF);
        // page block edges
        g.fill(bx + 1, by + 2, bx + bw - 1, by + bh + 1, 0xFFB8A57E);
        g.fill(bx + 2, by + 1, bx + bw - 2, by + bh, 0xFFD9C9A2);
        // the two pages
        int mid = bx + bw / 2;
        tile(g, PAPER, bx + 2, by, mid - bx - 3, bh - 2);
        tile(g, PAPER, mid + 1, by, bx + bw - mid - 3, bh - 2);
        // gutter shadow
        for (int i = 0; i < 8; i++) {
            int a = (int) (0x50 * (1 - i / 8f));
            g.fill(mid - 1 - i, by, mid - i, by + bh - 2, a << 24 | 0x3A2510);
            g.fill(mid + i, by, mid + 1 + i, by + bh - 2, a << 24 | 0x3A2510);
        }
        g.fill(mid - 1, by, mid + 1, by + bh - 2, 0xFF7A6544);
    }

    private int ribbon(GuiGraphics g, int mx, int my, int x, Component label, int color, boolean active, Runnable action) {
        int w = font.width(label) + 12;
        int top = by - (active ? 19 : 16);
        boolean hov = GuiDraw.in(mx, my, x, top, w, by - 6 - top);
        int c = hov ? brighten(color) : color;
        g.fill(x - 1, top - 1, x + w + 1, by, 0xFF20140B);
        g.fill(x, top, x + w, by, c);
        g.fill(x, top, x + w, top + 1, 0x40FFFFFF);
        g.drawString(font, label, x + 6, top + 3, 0xFFF3E6C8, false);
        clicks.add(new Click(x, top, w, by - 6 - top, b -> action.run()));
        return x + w + 4;
    }

    private static int brighten(int c) {
        int r = Math.min(255, ((c >> 16) & 0xFF) + 30), gg = Math.min(255, ((c >> 8) & 0xFF) + 30), b = Math.min(255, (c & 0xFF) + 30);
        return 0xFF000000 | r << 16 | gg << 8 | b;
    }

    private void drawFooter(GuiGraphics g, int mx, int my) {
        if (state.mode == Mode.HOME) return;
        State p = prevState(), n = nextState();
        if (p != null) arrow(g, mx, my, lx, footY, false, p);
        if (n != null) arrow(g, mx, my, rx + pw - 18, footY, true, n);
        if (state.mode == Mode.PAGE) {
            String s = Component.translatable("gui.ls_codex.page_of", state.page + 1, state.chapter.pageCount()).getString();
            g.drawString(font, s, lx + pw / 2 - font.width(s) / 2, footY + 2, FAINT, false);
        }
    }

    private void arrow(GuiGraphics g, int mx, int my, int x, int y, boolean right, State target) {
        boolean hov = GuiDraw.in(mx, my, x, y, 18, 11);
        int c = hov ? LINK : HEAD;
        for (int i = 0; i < 18; i++) {
            int xx = right ? x + i : x + 17 - i;
            int top, bot;
            if (i < 11) {
                top = y + 4;
                bot = y + 7;
            } else {
                int half = (17 - i) * 5 / 6;
                top = y + 5 - half;
                bot = y + 6 + half;
            }
            g.fill(xx, top, xx + 1, bot, c);
        }
        clicks.add(new Click(x, y - 2, 18, 15, b -> enter(target, true)));
    }

    // ------------------------------------------------------------------ text helpers

    private int heading(GuiGraphics g, String text, int x, int y, int w, int maxLines) {
        List<FormattedCharSequence> ls = font.split(Component.literal(text), w);
        int n = Math.min(maxLines, ls.size());
        for (int i = 0; i < n; i++) g.drawString(font, ls.get(i), x, y + i * LINE, HEAD, false);
        int h = n * LINE;
        g.fill(x, y + h, x + Math.min(w, Math.max(30, font.width(ls.get(0)) + 4)), y + h + 1, 0x806B2D0F);
        return h + 4;
    }

    private List<FormattedCharSequence> wrap(String text, int w) {
        List<FormattedCharSequence> out = new ArrayList<>();
        boolean first = true;
        for (String para : text.split("\n\n")) {
            if (!first) out.add(null);
            first = false;
            for (String ln : para.split("\n")) {
                if (ln.isEmpty()) out.add(FormattedCharSequence.EMPTY);
                else out.addAll(font.split(Component.literal(ln), w));
            }
        }
        return out;
    }

    /** Draws lines from index start while they fit above bottom; returns the index of the first line not drawn. */
    private int drawLines(GuiGraphics g, List<FormattedCharSequence> ls, int start, int x, int y, int bottom, int color) {
        int i = start;
        while (i < ls.size()) {
            int h = ls.get(i) == null ? GAP : LINE;
            if (y + h > bottom + 1) break;
            if (ls.get(i) != null) g.drawString(font, ls.get(i), x, y, color, false);
            y += h;
            i++;
        }
        return i;
    }

    private void icon(GuiGraphics g, ItemStack s, int x, int y, float scale, int mx, int my) {
        g.pose().pushPose();
        g.pose().translate(x, y, 0);
        g.pose().scale(scale, scale, 1f);
        g.renderItem(s, 0, 0);
        g.pose().popPose();
        int size = Math.round(16 * scale);
        if (GuiDraw.in(mx, my, x, y, size, size)) {
            hoverStack = s;
            hoverExtra.clear();
        }
    }

    private void frame(GuiGraphics g, int x, int y, boolean highlight) {
        g.fill(x - 1, y - 1, x + 17, y + 17, highlight ? GOLD : FRAME);
        g.fill(x, y, x + 16, y + 16, FRAME_FILL);
    }

    private void dot(GuiGraphics g, int x, int y) {
        g.fill(x, y + 1, x + 4, y + 3, MARK);
        g.fill(x + 1, y, x + 3, y + 4, MARK);
    }

    // ------------------------------------------------------------------ home

    private void renderHome(GuiGraphics g, int mx, int my) {
        List<CodexBook.Chapter> all = CodexBook.chapters();
        int y = py;
        String t = title.getString();
        g.drawString(font, t, lx + pw / 2 - font.width(t) / 2, y, HEAD, false);
        int pages = 0;
        for (CodexBook.Chapter c : all) pages += c.pageCount();
        String sum = Component.translatable("gui.ls_codex.summary", all.size(), pages).getString();
        g.drawString(font, sum, lx + pw / 2 - font.width(sum) / 2, y + 12, FAINT, false);
        // index list
        int top = py + 42, bottom = py + ph;
        int total = 0;
        for (Row r : rows) total += r.kind == 0 ? 13 : 14;
        int max = Math.max(0, total - (bottom - top));
        listScroll = Mth.clamp(listScroll, 0, max);
        if (rows.isEmpty() && !search.getValue().isBlank()) {
            GuiDraw.wrapped(g, font, Component.translatable("gui.ls_codex.no_results"), lx, top + 4, pw, FAINT);
        } else if (!search.getValue().isBlank()) {
            String n = Component.translatable("gui.ls_codex.results", rows.size()).getString();
            g.drawString(font, n, lx + pw - font.width(n), py + 12, FAINT, false);
        }
        g.enableScissor(lx - 2, top, lx + pw + 2, bottom);
        int ry = top - listScroll;
        for (Row r : rows) {
            int h = r.kind == 0 ? 13 : 14;
            if (ry + h >= top && ry < bottom) drawRow(g, r, ry, mx, my, top, bottom);
            ry += h;
        }
        g.disableScissor();
        if (max > 0) {
            g.fill(lx + pw + 3, top, lx + pw + 5, bottom, 0x30000000);
            int th = Math.max(10, (bottom - top) * (bottom - top) / total);
            int ty = top + (int) ((long) (bottom - top - th) * listScroll / max);
            g.fill(lx + pw + 3, ty, lx + pw + 5, ty + th, 0xA06B2D0F);
        }
        scrolls.add(new Scroll(lx, top, pw + 6, bottom - top, d -> listScroll = Mth.clamp(listScroll - (int) (d * 21), 0, max)));

        // right page: welcome and progress
        y = py;
        y += heading(g, Component.translatable("gui.ls_codex.welcome_title").getString(), rx, y, pw, 1) + 2;
        List<FormattedCharSequence> w = wrap(Component.translatable("gui.ls_codex.welcome").getString(), pw);
        int stop = drawLines(g, w, 0, rx, y, py + ph - 64, INK);
        for (int i = 0; i < stop; i++) y += w.get(i) == null ? GAP : LINE;
        y += 6;
        int read = 0, finished = 0, seen = 0, items = 0;
        for (CodexBook.Chapter c : all) {
            int un = CodexProgress.unread(c);
            read += c.pageCount() - un;
            if (un == 0) finished++;
            seen += CodexProgress.held(c);
            items += c.items.size();
        }
        y = stat(g, rx, y, Component.translatable("gui.ls_codex.stat_pages", read, pages), pages == 0 ? 0 : read / (float) pages, 0xFF4F7A3A);
        y = stat(g, rx, y, Component.translatable("gui.ls_codex.stat_chapters", finished, all.size()), all.isEmpty() ? 0 : finished / (float) all.size(), 0xFF3A5F7A);
        y = stat(g, rx, y, Component.translatable("gui.ls_codex.stat_items", seen, items), items == 0 ? 0 : seen / (float) items, 0xFF9A6A2A);
        Component key = CodexClient.LOOKUP.getTranslatedKeyMessage();
        List<FormattedCharSequence> hint = font.split(Component.translatable("gui.ls_codex.hint_lookup", key.getString()), pw);
        drawLines(g, hint, 0, rx, Math.max(y + 2, py + ph - hint.size() * LINE), py + ph, FAINT);
    }

    private int stat(GuiGraphics g, int x, int y, Component label, float frac, int color) {
        g.drawString(font, label, x, y, INK, false);
        g.fill(x, y + 10, x + pw, y + 13, 0x30000000);
        g.fill(x, y + 10, x + Math.round(pw * Math.min(1f, frac)), y + 13, color);
        return y + 17;
    }

    private void drawRow(GuiGraphics g, Row r, int y, int mx, int my, int top, int bottom) {
        boolean inList = my >= top && my < bottom;
        if (r.kind == 0) {
            g.drawString(font, r.label, lx, y + 3, HEAD, false);
            g.fill(lx + font.width(r.label) + 3, y + 7, lx + pw, y + 8, 0x506B2D0F);
            return;
        }
        boolean hov = inList && GuiDraw.in(mx, my, lx - 1, y, pw + 2, 14);
        if (hov) g.fill(lx - 1, y, lx + pw + 1, y + 14, HOVER);
        icon(g, r.icon, lx + 1, y + 1, 0.75f, inList ? mx : -1, my);
        int right = lx + pw;
        CodexBook.Chapter c = r.chapter;
        if (r.kind == 1) {
            String cnt = CodexProgress.held(c) + "/" + c.items.size();
            g.drawString(font, cnt, right - font.width(cnt), y + 3, FAINT, false);
            right -= font.width(cnt) + 3;
            if (CodexProgress.unread(c) > 0) {
                dot(g, right - 4, y + 5);
                right -= 7;
            }
        } else {
            if (r.page >= 0 && !CodexProgress.isRead(c, r.page)) {
                dot(g, right - 4, y + 5);
                right -= 7;
            }
            String ch = font.plainSubstrByWidth(c.name(), 60);
            if (r.page >= 0 && font.width(r.label) + font.width(ch) + 20 < right - lx) {
                g.drawString(font, ch, right - font.width(ch), y + 3, FAINT, false);
                right -= font.width(ch) + 4;
            }
        }
        String label = font.plainSubstrByWidth(r.label, right - lx - 16);
        g.drawString(font, label, lx + 15, y + 3, hov ? LINK : INK, false);
        if (inList) {
            if (hov && hoverStack.isEmpty() && r.kind == 1) hoverText = Component.literal(c.data.desc);
            clicks.add(new Click(lx - 1, Math.max(y, top), pw + 2, Math.min(y + 14, bottom) - Math.max(y, top), b -> openPage(c, r.page)));
        }
    }

    // ------------------------------------------------------------------ chapter

    private void renderChapter(GuiGraphics g, int mx, int my) {
        CodexBook.Chapter c = state.chapter;
        int y = py;
        icon(g, c.icon, lx, y, 1f, mx, my);
        List<FormattedCharSequence> name = font.split(Component.literal(c.name()), pw - 20);
        for (int i = 0; i < Math.min(2, name.size()); i++) g.drawString(font, name.get(i), lx + 20, y + (name.size() > 1 ? i * LINE : 4), HEAD, false);
        y += Math.max(18, Math.min(2, name.size()) * LINE) + 2;
        String tab = Component.translatable("gui.ls_codex.tab." + c.data.tab.toLowerCase(java.util.Locale.ROOT)).getString();
        g.drawString(font, tab, lx, y, FAINT, false);
        g.fill(lx, y + 10, lx + pw, y + 11, 0x806B2D0F);
        y += 14;
        // items grid at the bottom: carried items in colour, the rest faded
        int perRow = Math.max(1, (pw + 2) / 18);
        int gridRows = Math.min(2, (c.items.size() + perRow - 1) / perRow);
        int gridH = gridRows * 18;
        int bottomBlock = 16 + gridH;
        if (linesDirty) {
            lines = wrap(c.data.intro, pw);
            linesDirty = false;
        }
        drawLines(g, lines, 0, lx, y, py + ph - bottomBlock - 4, INK);
        int by0 = py + ph - bottomBlock;
        int held = CodexProgress.held(c);
        g.drawString(font, Component.translatable("gui.ls_codex.items_held", held, c.items.size()), lx, by0, INK, false);
        g.fill(lx, by0 + 10, lx + pw, by0 + 12, 0x30000000);
        if (!c.items.isEmpty()) g.fill(lx, by0 + 10, lx + pw * held / c.items.size(), by0 + 12, 0xFF9A6A2A);
        int gy = by0 + 16;
        int shown = Math.min(c.items.size(), gridRows * perRow);
        boolean more = c.items.size() > shown;
        if (more) shown--;
        for (int i = 0; i < shown; i++) {
            ItemStack s = c.items.get(i);
            int x = lx + (i % perRow) * 18, yy = gy + (i / perRow) * 18;
            boolean seen = CodexProgress.seen(s);
            g.renderItem(s, x, yy);
            if (!seen) {
                g.pose().pushPose();
                g.pose().translate(0, 0, 200);
                g.fill(x, yy, x + 16, yy + 16, 0xA8E9DCBB);
                g.pose().popPose();
            }
            if (GuiDraw.in(mx, my, x, yy, 16, 16)) {
                hoverStack = s;
                hoverExtra.clear();
                if (!seen) hoverExtra.add(Component.translatable("gui.ls_codex.not_carried").withStyle(ChatFormatting.DARK_GRAY));
                hoverExtra.add(Component.translatable("gui.ls_codex.click_page").withStyle(ChatFormatting.DARK_AQUA));
                clicks.add(new Click(x, yy, 16, 16, b -> {
                    for (int p = 0; p < c.pageCount(); p++) {
                        for (ItemStack ps : c.pageItems.get(p)) {
                            if (ItemStack.isSameItem(ps, s)) {
                                keepFocus = true;
                                focus = s;
                                openPage(c, p);
                                return;
                            }
                        }
                    }
                }));
            }
        }
        if (more) {
            String m = "+" + (c.items.size() - shown);
            int i = shown;
            g.drawString(font, m, lx + (i % perRow) * 18 + 8 - font.width(m) / 2, gy + (i / perRow) * 18 + 4, FAINT, false);
        }

        // right page: contents
        y = py;
        y += heading(g, Component.translatable("gui.ls_codex.contents").getString(), rx, y, pw, 1) + 2;
        int top = y, bottom = py + ph;
        int visible = (bottom - top) / 13;
        int maxScroll = Math.max(0, c.pageCount() - visible);
        contentsScroll = Mth.clamp(contentsScroll, 0, maxScroll);
        for (int i = contentsScroll; i < c.pageCount() && i < contentsScroll + visible; i++) {
            int ry = top + (i - contentsScroll) * 13;
            boolean hov = GuiDraw.in(mx, my, rx - 1, ry, pw + 2, 13);
            if (hov) g.fill(rx - 1, ry, rx + pw + 1, ry + 13, HOVER);
            String num = (i + 1) + ".";
            g.drawString(font, num, rx + 14 - font.width(num), ry + 2, FAINT, false);
            boolean unread = !CodexProgress.isRead(c, i);
            int right = rx + pw - (unread ? 8 : 0);
            String label = font.plainSubstrByWidth(c.page(i).title(), right - rx - 20);
            g.drawString(font, label, rx + 18, ry + 2, hov ? LINK : INK, false);
            if (unread) {
                dot(g, rx + pw - 5, ry + 4);
                if (GuiDraw.in(mx, my, rx + pw - 7, ry, 8, 13)) hoverText = Component.translatable("gui.ls_codex.unread");
            }
            if (c.page(i).multiblock() != null) g.fill(rx + 16, ry + 5, rx + 17, ry + 7, HEAD);
            final int page = i;
            clicks.add(new Click(rx - 1, ry, pw + 2, 13, b -> openPage(c, page)));
        }
        int ms = maxScroll;
        scrolls.add(new Scroll(rx, top, pw, bottom - top, d -> contentsScroll = Mth.clamp(contentsScroll - (int) Math.signum(d), 0, ms)));
    }

    // ------------------------------------------------------------------ page

    private void renderPage(GuiGraphics g, int mx, int my) {
        CodexBook.Chapter c = state.chapter;
        CodexChapter.Page page = c.page(state.page);
        List<ItemStack> items = c.pageItems.get(state.page);
        int y = py;
        y += heading(g, page.title(), lx, y, pw, 2);
        String sub = font.plainSubstrByWidth(c.name(), pw);
        g.drawString(font, sub, lx, y, FAINT, false);
        y += 13;
        // item row at the bottom of the left page
        int perRow = Math.max(1, (pw + 2) / 20);
        int itemRows = Math.min(2, (items.size() + perRow - 1) / perRow);
        int itemsH = itemRows == 0 ? 0 : itemRows * 20 + 2;
        int textBottom = py + ph - itemsH;
        if (linesDirty) {
            lines = wrap(page.text(), pw);
            linesDirty = false;
        }
        // what the right page shows decides whether text may continue there
        List<RecipeFinder.Found> recipes = focus.isEmpty() ? List.of() : finder.producing(focus);
        boolean rightBusy = page.image() != null || page.multiblock() != null || !recipes.isEmpty();
        int leftCap = capacity(lines, textScroll, y, textBottom);
        int end = textScroll + leftCap;
        int rightEnd = end;
        if (!rightBusy) rightEnd = end + capacity(lines, end, py, py + ph);
        // still too long for both pages: let the reader scroll
        textMaxScroll = textScroll + Math.max(0, lines.size() - rightEnd);
        drawLines(g, lines, textScroll, lx, y, textBottom, INK);
        if (textMaxScroll > 0) {
            if (textScroll > 0) g.drawString(font, "▲", lx + pw - 6, y - 12, FAINT, false);
            if (rightEnd < lines.size()) g.drawString(font, "▼", (rightBusy ? lx : rx) + pw - 6, (rightBusy ? textBottom : py + ph) - 8, FAINT, false);
            int maxS = textMaxScroll;
            scrolls.add(new Scroll(lx, py, pw, ph, d -> textScroll = Mth.clamp(textScroll - (int) Math.signum(d) * 2, 0, maxS)));
        }
        // items
        int iy = textBottom + 3;
        for (int i = 0; i < items.size() && i < itemRows * perRow; i++) {
            ItemStack s = items.get(i);
            int x = lx + 1 + (i % perRow) * 20, yy = iy + (i / perRow) * 20;
            boolean focused = !focus.isEmpty() && ItemStack.isSameItem(s, focus);
            frame(g, x, yy, focused);
            g.renderItem(s, x, yy);
            if (GuiDraw.in(mx, my, x, yy, 16, 16)) {
                hoverStack = s;
                hoverExtra.clear();
                if (LensBridge.present) {
                    hoverExtra.add(Component.translatable("gui.ls_codex.click_recipes").withStyle(ChatFormatting.DARK_AQUA));
                    hoverExtra.add(Component.translatable("gui.ls_codex.click_uses").withStyle(ChatFormatting.DARK_AQUA));
                    hoverExtra.add(Component.translatable("gui.ls_codex.shift_show").withStyle(ChatFormatting.DARK_GRAY));
                } else if (!focused) {
                    hoverExtra.add(Component.translatable("gui.ls_codex.click_show").withStyle(ChatFormatting.DARK_AQUA));
                }
                clicks.add(new Click(x, yy, 16, 16, b -> itemClicked(s, b)));
            }
        }
        // right page
        if (rightBusy) {
            int ry = py;
            if (page.image() != null) ry += drawImage(g, new ResourceLocation(page.image()), rx, ry, pw, page.multiblock() != null || !recipes.isEmpty() ? 70 : ph) + 4;
            if (page.multiblock() != null) {
                renderStructure(g, rx, ry, pw, py + ph - ry, mx, my);
            } else if (!recipes.isEmpty()) {
                renderRecipe(g, recipes, rx, ry, pw, py + ph - ry, mx, my);
            }
        } else if (end < lines.size()) {
            drawLines(g, lines, end, rx, py, py + ph, INK);
        } else if (!focus.isEmpty()) {
            int ry = py;
            ry += heading(g, focus.getHoverName().getString(), rx, ry, pw, 2);
            GuiDraw.wrapped(g, font, Component.translatable("gui.ls_codex.no_recipe"), rx, ry + 2, pw, FAINT);
        }
    }

    private int capacity(List<FormattedCharSequence> ls, int start, int y, int bottom) {
        int n = 0;
        for (int i = start; i < ls.size(); i++) {
            int h = ls.get(i) == null ? GAP : LINE;
            if (y + h > bottom + 1) break;
            y += h;
            n++;
        }
        return n;
    }

    private void itemClicked(ItemStack s, int button) {
        boolean show = !LensBridge.present || (button == 0 && hasShiftDown());
        if (show) {
            if (button == 0) {
                focus = s;
                recipeIndex = 0;
                minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK.value(), 1.4f, 0.4f));
            }
            return;
        }
        if (button == 0) LensBridge.recipes.accept(s.copy());
        else if (button == 1) LensBridge.uses.accept(s.copy());
    }

    private int drawImage(GuiGraphics g, ResourceLocation rl, int x, int y, int w, int maxH) {
        int[] size = IMAGE_SIZES.computeIfAbsent(rl, k -> {
            try (InputStream in = minecraft.getResourceManager().open(k); NativeImage img = NativeImage.read(in)) {
                return new int[]{img.getWidth(), img.getHeight()};
            } catch (Exception e) {
                return new int[]{0, 0};
            }
        });
        if (size[0] <= 0) return 0;
        float s = Math.min(w / (float) size[0], maxH / (float) size[1]);
        int dw = Math.round(size[0] * s), dh = Math.round(size[1] * s);
        int dx = x + (w - dw) / 2;
        g.fill(dx - 1, y - 1, dx + dw + 1, y + dh + 1, FRAME);
        g.blit(rl, dx, y, dw, dh, 0, 0, size[0], size[1], size[0], size[1]);
        return dh + 2;
    }

    private void renderRecipe(GuiGraphics g, List<RecipeFinder.Found> recipes, int x, int y, int w, int h, int mx, int my) {
        recipeIndex = Math.floorMod(recipeIndex, recipes.size());
        RecipeFinder.Found f = recipes.get(recipeIndex);
        y += heading(g, focus.getHoverName().getString(), x, y, w, 2);
        // where it is made, and paging between recipes
        if (!f.icon().isEmpty()) icon(g, f.icon(), x, y, 0.75f, mx, my);
        int right = x + w;
        if (recipes.size() > 1) {
            String n = (recipeIndex + 1) + "/" + recipes.size();
            int nx = right - 10 - font.width(n);
            g.drawString(font, n, nx, y + 2, FAINT, false);
            boolean hl = GuiDraw.in(mx, my, nx - 10, y, 9, 11), hr = GuiDraw.in(mx, my, right - 8, y, 9, 11);
            g.drawString(font, "◀", nx - 9, y + 2, hl ? LINK : HEAD, false);
            g.drawString(font, "▶", right - 7, y + 2, hr ? LINK : HEAD, false);
            clicks.add(new Click(nx - 10, y, 9, 11, b -> recipeIndex--));
            clicks.add(new Click(right - 8, y, 9, 11, b -> recipeIndex++));
            right = nx - 12;
        }
        g.drawString(font, font.plainSubstrByWidth(f.title().getString(), right - x - 16), x + 15, y + 2, INK, false);
        y += 16;
        // the recipe on a little card, scaled down if it is wider than the page
        RecipeView v = f.view();
        float s = Math.min(1f, Math.min((w - 10) / (float) v.width, (h - 26) / (float) v.height));
        int vw = Math.round(v.width * s), vh = Math.round(v.height * s);
        int vx = x + (w - vw) / 2, vy = y + 5;
        GuiDraw.panel(g, vx - 5, vy - 5, vw + 10, vh + 10);
        int lmx = (int) ((mx - vx) / s), lmy = (int) ((my - vy) / s);
        g.pose().pushPose();
        g.pose().translate(vx, vy, 0);
        g.pose().scale(s, s, 1f);
        RecipeDraw.draw(g, v, 0, 0, lmx, lmy);
        g.pose().popPose();
        RecipeDraw.Hit hit = GuiDraw.in(mx, my, vx, vy, vw, vh) ? RecipeDraw.hit(v, 0, 0, lmx, lmy) : null;
        if (hit != null) {
            if (hit.slot() != null && !hit.stack().isEmpty()) {
                ItemStack st = hit.stack();
                hoverStack = st;
                hoverExtra.clear();
                if (hit.slot().chance() < 1f) hoverExtra.add(Component.translatable("lens.ls_core.chance", Math.round(hit.slot().chance() * 100)).withStyle(ChatFormatting.GOLD));
                if (hit.slot().note() != null) hoverExtra.add(hit.slot().note().copy().withStyle(ChatFormatting.AQUA));
                CodexBook.Ref ref = CodexBook.find(st.getItem());
                boolean link = ref != null && !(ref.chapter() == state.chapter && ref.page() == state.page);
                if (link) hoverExtra.add(Component.translatable("gui.ls_codex.click_page").withStyle(ChatFormatting.DARK_AQUA));
                else if (LensBridge.present) hoverExtra.add(Component.translatable("gui.ls_codex.click_recipes").withStyle(ChatFormatting.DARK_AQUA));
                if (LensBridge.present) hoverExtra.add(Component.translatable("gui.ls_codex.click_uses").withStyle(ChatFormatting.DARK_AQUA));
                int sx = vx + Math.round(hit.slot().x() * s), sy = vy + Math.round(hit.slot().y() * s), ss = Math.max(1, Math.round(16 * s));
                clicks.add(new Click(sx, sy, ss, ss, b -> {
                    if (b == 0 && !followItem(st) && LensBridge.present) LensBridge.recipes.accept(st.copy());
                    if (b == 1 && LensBridge.present) LensBridge.uses.accept(st.copy());
                }));
            } else if (hit.tank() != null) {
                hoverTank = hit;
            }
        }
        int ty = vy + vh + 9;
        if (ty + 10 < y + h - 16 && recipes.size() > 1) {
            g.drawString(font, Component.translatable("gui.ls_codex.recipe_of", recipeIndex + 1, recipes.size()), x, ty, FAINT, false);
        }
    }

    private void renderStructure(GuiGraphics g, int x, int y, int w, int h, int mx, int my) {
        if (structure == null) {
            y += heading(g, Component.translatable("gui.ls_codex.structure").getString(), x, y, w, 1);
            GuiDraw.wrapped(g, font, Component.translatable("gui.ls_codex.missing_structure"), x, y + 2, w, FAINT);
            return;
        }
        var pat = structure.pattern;
        y += heading(g, Component.translatable("gui.ls_codex.structure").getString(), x, y, w, 1);
        String dims = pat.width + "×" + pat.height + "×" + pat.depth;
        g.drawString(font, dims, x + w - font.width(dims), y - 14, FAINT, false);
        // bill of materials below the preview
        List<ItemStack> mats = structure.materials;
        int perRow = Math.max(1, (w + 2) / 18);
        int matRows = Math.min(2, (mats.size() + perRow - 1) / perRow);
        int matsH = 12 + matRows * 18;
        int pvH = Math.max(50, h - matsH - 14);
        int pvW = w - 12;
        // preview frame
        g.fill(x, y, x + pvW, y + pvH, 0x22000000);
        g.fill(x, y, x + pvW, y + 1, 0x40000000);
        long now = System.currentTimeMillis();
        if (lastFrame == 0) lastFrame = now;
        float dt = Math.min(0.1f, (now - lastFrame) / 1000f);
        lastFrame = now;
        if (!dragging && now - lastTouch > 2500) yaw += dt * 18f;
        structure.render(g, x + 1, y + 1, pvW - 2, pvH - 2, yaw, pitch, zoom, maxLayer);
        clicks.add(new Click(x, y, pvW, pvH, b -> {
            if (b == 0) {
                dragging = true;
                lastTouch = System.currentTimeMillis();
            }
        }));
        scrolls.add(new Scroll(x, y, pvW, pvH, d -> {
            zoom = Mth.clamp(zoom * (float) Math.pow(1.12, d), 0.5f, 3f);
            lastTouch = System.currentTimeMillis();
        }));
        if (GuiDraw.in(mx, my, x, y, pvW, pvH) && !dragging) hoverText = Component.translatable("gui.ls_codex.drag");
        // layer slider: top = every layer, each step down hides the top layer
        int layers = structure.layers();
        int sx = x + w - 8;
        sliderY = y;
        sliderH = pvH;
        g.fill(sx + 2, y, sx + 4, y + pvH, 0x40000000);
        int th = 8;
        int k = layers - 1 - maxLayer;
        int thumbY = layers <= 1 ? y : y + (pvH - th) * k / (layers - 1);
        for (int i = 0; i < layers && layers > 1; i++) {
            int ty = y + (pvH - th) * i / (layers - 1) + th / 2;
            g.fill(sx, ty, sx + 6, ty + 1, 0x506B2D0F);
        }
        boolean hovS = GuiDraw.in(mx, my, sx - 1, y, 8, pvH);
        g.fill(sx - 1, thumbY, sx + 7, thumbY + th, hovS || sliding ? LINK : HEAD);
        g.fill(sx, thumbY + 1, sx + 6, thumbY + 2, 0x40FFFFFF);
        clicks.add(new Click(sx - 1, y, 8, pvH, b -> {
            sliding = true;
            slide(clickY);
        }));
        scrolls.add(new Scroll(sx - 1, y, 8, pvH, d -> maxLayer = Mth.clamp(maxLayer + (int) Math.signum(d), 0, layers - 1)));
        String layerLabel = maxLayer >= layers - 1 ? Component.translatable("gui.ls_codex.all_layers").getString()
                : Component.translatable("gui.ls_codex.layer", maxLayer + 1, layers).getString();
        int ly = y + pvH + 3;
        g.drawString(font, layerLabel, x, ly, FAINT, false);
        ly += 12;
        g.drawString(font, Component.translatable("gui.ls_codex.materials"), x, ly, INK, false);
        ly += 10;
        int shown = Math.min(mats.size(), matRows * perRow);
        for (int i = 0; i < shown; i++) {
            ItemStack s = mats.get(i);
            int ix = x + 1 + (i % perRow) * 18, iy = ly + (i / perRow) * 18;
            frame(g, ix, iy, false);
            g.renderItem(s, ix, iy);
            g.renderItemDecorations(font, s, ix, iy, String.valueOf(s.getCount()));
            if (GuiDraw.in(mx, my, ix, iy, 16, 16)) {
                hoverStack = s;
                hoverExtra.clear();
                hoverExtra.add(Component.literal("× " + s.getCount()).withStyle(ChatFormatting.GOLD));
                CodexBook.Ref ref = CodexBook.find(s.getItem());
                if (ref != null && !(ref.chapter() == state.chapter && ref.page() == state.page)) {
                    hoverExtra.add(Component.translatable("gui.ls_codex.click_page").withStyle(ChatFormatting.DARK_AQUA));
                    clicks.add(new Click(ix, iy, 16, 16, b -> followItem(s)));
                }
            }
        }
    }

    private void slide(double my) {
        if (structure == null) return;
        int layers = structure.layers();
        if (layers <= 1) return;
        float f = (float) ((my - sliderY) / Math.max(1, sliderH));
        int k = Mth.clamp(Math.round(f * (layers - 1)), 0, layers - 1);
        maxLayer = layers - 1 - k;
    }
}
