package com.frontierworks.guide.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;

import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Guide contents, generated at build time into assets/fw_guide/guide/guide.json. */
public final class GuideData {
    public record ModPage(String id, String name, String desc, List<String> items) {}

    public record Category(String id, String title, String icon, String intro, List<ModPage> mods) {}

    public final List<Category> categories = new ArrayList<>();

    public static GuideData load() {
        GuideData d = new GuideData();
        try {
            Optional<Resource> res = Minecraft.getInstance().getResourceManager().getResource(new ResourceLocation("fw_guide", "guide/guide.json"));
            if (res.isEmpty()) return d;
            try (Reader r = res.get().openAsReader()) {
                JsonObject root = JsonParser.parseReader(r).getAsJsonObject();
                for (JsonElement ce : root.getAsJsonArray("categories")) {
                    JsonObject c = ce.getAsJsonObject();
                    List<ModPage> mods = new ArrayList<>();
                    for (JsonElement me : c.getAsJsonArray("mods")) {
                        JsonObject mo = me.getAsJsonObject();
                        List<String> items = new ArrayList<>();
                        JsonArray arr = mo.getAsJsonArray("items");
                        for (JsonElement ie : arr) items.add(ie.getAsString());
                        mods.add(new ModPage(mo.get("id").getAsString(), mo.get("name").getAsString(), mo.get("desc").getAsString(), items));
                    }
                    d.categories.add(new Category(c.get("id").getAsString(), c.get("title").getAsString(), c.get("icon").getAsString(),
                            c.get("intro").getAsString(), mods));
                }
            }
        } catch (Exception e) {
            LogUtils.getLogger().error("Failed to load Frontierworks guide", e);
        }
        return d;
    }
}
