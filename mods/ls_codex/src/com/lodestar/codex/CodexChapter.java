package com.lodestar.codex;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.util.GsonHelper;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * One mod's Codex chapter as written by the generator to {@code assets/<modid>/codex/chapter.json}:
 * {mod, name, tab, desc, intro, pages: [{title, text, items: [ids], multiblock?, image?}]}. Plain data, usable on both
 * sides (the client reads it through its resource manager, the self-test straight from the mod files).
 */
public final class CodexChapter {
    public final String mod, name, tab, desc, intro;
    public final List<Page> pages;

    public record Page(String title, String text, List<String> items, @Nullable String multiblock, @Nullable String image) {
        /** Stable key of this page for reading progress ("ls_ducts#Energy"). */
        public String key(CodexChapter chapter) {
            return chapter.mod + "#" + title;
        }
    }

    private CodexChapter(String mod, String name, String tab, String desc, String intro, List<Page> pages) {
        this.mod = mod;
        this.name = name;
        this.tab = tab;
        this.desc = desc;
        this.intro = intro;
        this.pages = Collections.unmodifiableList(pages);
    }

    public static CodexChapter parse(JsonObject o, String fallbackMod) {
        String mod = GsonHelper.getAsString(o, "mod", fallbackMod);
        String name = GsonHelper.getAsString(o, "name", mod);
        String tab = GsonHelper.getAsString(o, "tab", "CORE").toUpperCase(java.util.Locale.ROOT);
        String desc = GsonHelper.getAsString(o, "desc", "");
        String intro = GsonHelper.getAsString(o, "intro", desc);
        List<Page> pages = new ArrayList<>();
        JsonArray arr = GsonHelper.getAsJsonArray(o, "pages", new JsonArray());
        for (JsonElement el : arr) {
            if (!el.isJsonObject()) continue;
            JsonObject p = el.getAsJsonObject();
            List<String> items = new ArrayList<>();
            for (JsonElement i : GsonHelper.getAsJsonArray(p, "items", new JsonArray())) items.add(i.getAsString());
            pages.add(new Page(GsonHelper.getAsString(p, "title", "?"), GsonHelper.getAsString(p, "text", ""), items,
                    p.has("multiblock") ? GsonHelper.getAsString(p, "multiblock") : null,
                    p.has("image") ? GsonHelper.getAsString(p, "image") : null));
        }
        return new CodexChapter(mod, name, tab, desc, intro, pages);
    }

    /** Every item id mentioned on any page, in page order without repeats. */
    public List<String> allItems() {
        List<String> out = new ArrayList<>();
        for (Page p : pages) for (String i : p.items()) if (!out.contains(i)) out.add(i);
        return out;
    }
}
