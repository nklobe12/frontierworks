package com.lodestar.codex.client;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.lodestar.codex.CodexChapter;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.slf4j.Logger;

import javax.annotation.Nullable;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Every Codex chapter, read through the client resource manager (so resource packs can rewrite pages), sorted by
 * creative tab and name, with an index from item to the page that describes it and a search over everything.
 * Rebuilt lazily after each resource reload (which also covers language changes).
 */
public final class CodexBook {
    private static final Logger LOG = LogUtils.getLogger();
    public static final List<String> TAB_ORDER = List.of("CORE", "MATERIALS", "INDUSTRY", "POWER", "LOGISTICS", "AUTOMATION", "TOOLS",
            "GEAR", "ALCHEMY", "EXPLORATION", "NATURE", "CREATURES", "DIMENSIONS", "SPACE", "FARMING", "BUILDING", "TRANSPORT");

    /** A chapter with its items resolved for display. */
    public static final class Chapter {
        public final CodexChapter data;
        public final List<List<ItemStack>> pageItems = new ArrayList<>();
        /** every distinct item of the chapter */
        public final List<ItemStack> items = new ArrayList<>();
        public final ItemStack icon;
        /** lower-case search text per page: title, text and item names */
        final List<String> pageSearch = new ArrayList<>();
        final List<String> pageTitles = new ArrayList<>();
        final String chapterSearch;

        Chapter(CodexChapter data) {
            this.data = data;
            Set<Item> seen = new LinkedHashSet<>();
            for (CodexChapter.Page p : data.pages) {
                List<ItemStack> stacks = new ArrayList<>();
                StringBuilder names = new StringBuilder();
                for (String id : p.items()) {
                    for (ItemStack s : resolve(id)) {
                        stacks.add(s);
                        names.append(' ').append(s.getHoverName().getString().toLowerCase(Locale.ROOT));
                        if (seen.add(s.getItem())) items.add(s);
                    }
                }
                pageItems.add(stacks);
                pageTitles.add(p.title().toLowerCase(Locale.ROOT));
                pageSearch.add((p.title() + " " + p.text()).toLowerCase(Locale.ROOT) + names);
            }
            icon = items.isEmpty() ? new ItemStack(Items.BOOK) : items.get(0);
            chapterSearch = (data.name + " " + data.desc + " " + data.intro).toLowerCase(Locale.ROOT);
        }

        public String name() {
            return data.name;
        }

        public int pageCount() {
            return data.pages.size();
        }

        public CodexChapter.Page page(int i) {
            return data.pages.get(i);
        }
    }

    /** Where an item is described: a chapter and a page index (-1 = the chapter's introduction). */
    public record Ref(Chapter chapter, int page) {}

    /** A search hit; page -1 is the chapter introduction. */
    public record Hit(Chapter chapter, int page, String label, ItemStack icon, int score) {}

    private static List<Chapter> chapters;
    private static Map<Item, Ref> index;

    private CodexBook() {}

    public static void invalidate() {
        chapters = null;
        index = null;
    }

    public static List<Chapter> chapters() {
        if (chapters == null) load();
        return chapters;
    }

    @Nullable
    public static Chapter chapter(String mod) {
        for (Chapter c : chapters()) if (c.data.mod.equals(mod)) return c;
        return null;
    }

    /** The page that describes an item: a page where it is the first item wins over one that merely lists it. */
    @Nullable
    public static Ref find(Item item) {
        if (index == null) load();
        return item == Items.AIR ? null : index.get(item);
    }

    static List<ItemStack> resolve(String id) {
        List<ItemStack> out = new ArrayList<>();
        try {
            if (id.startsWith("#")) {
                TagKey<Item> tag = TagKey.create(Registries.ITEM, new ResourceLocation(id.substring(1)));
                BuiltInRegistries.ITEM.getTag(tag).ifPresent(set -> {
                    if (set.size() > 0) out.add(new ItemStack(set.get(0).value()));
                });
            } else {
                Item item = BuiltInRegistries.ITEM.get(new ResourceLocation(id));
                if (item != Items.AIR) out.add(new ItemStack(item));
            }
        } catch (Exception e) {
            LOG.warn("Codex: bad item id {}", id);
        }
        return out;
    }

    private static void load() {
        List<Chapter> list = new ArrayList<>();
        Map<ResourceLocation, Resource> res;
        try {
            res = Minecraft.getInstance().getResourceManager().listResources("codex", rl -> rl.getPath().endsWith("/chapter.json"));
        } catch (Exception e) {
            LOG.error("Codex: could not list chapters", e);
            res = Map.of();
        }
        for (Map.Entry<ResourceLocation, Resource> e : res.entrySet()) {
            try (Reader r = e.getValue().openAsReader()) {
                JsonElement el = JsonParser.parseReader(r);
                CodexChapter c = CodexChapter.parse(el.getAsJsonObject(), e.getKey().getNamespace());
                if (c.pages.isEmpty()) continue; // engine-only chapters with nothing to read
                list.add(new Chapter(c));
            } catch (Exception ex) {
                LOG.error("Codex: could not read {}", e.getKey(), ex);
            }
        }
        list.sort((a, b) -> {
            int ta = tabIndex(a.data.tab), tb = tabIndex(b.data.tab);
            if (ta != tb) return Integer.compare(ta, tb);
            // the Codex's own chapter opens its tab
            boolean ca = a.data.mod.equals("ls_codex"), cb = b.data.mod.equals("ls_codex");
            if (ca != cb) return ca ? -1 : 1;
            return a.data.name.compareToIgnoreCase(b.data.name);
        });
        Map<Item, Ref> idx = new HashMap<>();
        for (Chapter c : list) {
            for (int p = 0; p < c.pageItems.size(); p++) {
                List<ItemStack> st = c.pageItems.get(p);
                if (!st.isEmpty()) idx.putIfAbsent(st.get(0).getItem(), new Ref(c, p));
            }
        }
        for (Chapter c : list) {
            for (int p = 0; p < c.pageItems.size(); p++) {
                for (ItemStack s : c.pageItems.get(p)) idx.putIfAbsent(s.getItem(), new Ref(c, p));
            }
        }
        chapters = list;
        index = idx;
        LOG.info("Codex: loaded {} chapters, {} indexed items", list.size(), idx.size());
    }

    public static int tabIndex(String tab) {
        int i = TAB_ORDER.indexOf(tab);
        return i < 0 ? TAB_ORDER.size() : i;
    }

    /** Searches chapter names, page titles, page text and item names; every word of the query must match. */
    public static List<Hit> search(String query) {
        String q = query.trim().toLowerCase(Locale.ROOT);
        List<Hit> out = new ArrayList<>();
        if (q.isEmpty()) return out;
        String[] words = q.split("\\s+");
        for (Chapter c : chapters()) {
            if (all(c.chapterSearch, words)) {
                int score = c.data.name.toLowerCase(Locale.ROOT).contains(q) ? 0 : 3;
                out.add(new Hit(c, -1, c.data.name, c.icon, score));
            }
            for (int p = 0; p < c.pageCount(); p++) {
                if (!all(c.pageSearch.get(p), words)) continue;
                int score = c.pageTitles.get(p).contains(q) ? 1 : 2;
                // name the item that matched, if any, so "steel ingot" shows the ingot
                ItemStack icon = c.pageItems.get(p).isEmpty() ? c.icon : c.pageItems.get(p).get(0);
                for (ItemStack s : c.pageItems.get(p)) {
                    if (s.getHoverName().getString().toLowerCase(Locale.ROOT).contains(q)) {
                        icon = s;
                        score = Math.min(score, 1);
                        break;
                    }
                }
                out.add(new Hit(c, p, c.page(p).title(), icon, score));
            }
        }
        out.sort((a, b) -> Integer.compare(a.score, b.score));
        return out.size() > 200 ? out.subList(0, 200) : out;
    }

    private static boolean all(String text, String[] words) {
        for (String w : words) if (!text.contains(w)) return false;
        return true;
    }
}
