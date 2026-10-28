package com.lodestar.codex.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.lodestar.codex.CodexChapter;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.LevelResource;
import org.slf4j.Logger;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * The reader's progress in the current world, kept on the client in {@code lodestar/codex/<world>.json} under the game
 * directory: which pages have been opened and which items have ever been in the player's inventory.
 */
public final class CodexProgress {
    private static final Logger LOG = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static String world;
    private static final Set<String> READ = new LinkedHashSet<>();
    private static final Set<String> SEEN = new LinkedHashSet<>();
    private static boolean dirty;
    /** the client level the progress was last checked against (re-checked when it changes) */
    private static Object lastLevel;

    private CodexProgress() {}

    /** A file-name-safe key for the world being played: the save folder in single player, the address in multiplayer. */
    private static String worldKey() {
        Minecraft mc = Minecraft.getInstance();
        IntegratedServer sp = mc.getSingleplayerServer();
        String key;
        if (sp != null) {
            Path root = sp.getWorldPath(LevelResource.ROOT).toAbsolutePath().normalize();
            key = "sp_" + (root.getFileName() == null ? "world" : root.getFileName().toString());
        } else {
            ServerData sd = mc.getCurrentServer();
            key = "mp_" + (sd == null ? "unknown" : sd.ip);
        }
        return key.replaceAll("[^A-Za-z0-9_.-]", "_");
    }

    private static Path file(String key) {
        return Minecraft.getInstance().gameDirectory.toPath().resolve("lodestar").resolve("codex").resolve(key + ".json");
    }

    /** Loads progress for the current world if it is not loaded yet. */
    private static void ensure() {
        Object level = Minecraft.getInstance().level;
        if (level == null || (level == lastLevel && world != null)) return;
        lastLevel = level;
        String key = worldKey();
        if (key.equals(world)) return;
        save();
        world = key;
        READ.clear();
        SEEN.clear();
        Path f = file(key);
        if (!Files.exists(f)) return;
        try (Reader r = Files.newBufferedReader(f)) {
            JsonElement el = JsonParser.parseReader(r);
            JsonObject o = el.getAsJsonObject();
            for (JsonElement e : o.getAsJsonArray("read")) READ.add(e.getAsString());
            for (JsonElement e : o.getAsJsonArray("seen")) SEEN.add(e.getAsString());
        } catch (Exception e) {
            LOG.warn("Codex: could not read progress file {}", f, e);
        }
    }

    public static void save() {
        if (world == null || !dirty) return;
        dirty = false;
        Path f = file(world);
        try {
            Files.createDirectories(f.getParent());
            JsonObject o = new JsonObject();
            JsonArray r = new JsonArray();
            READ.forEach(r::add);
            JsonArray s = new JsonArray();
            SEEN.forEach(s::add);
            o.add("read", r);
            o.add("seen", s);
            try (Writer w = Files.newBufferedWriter(f)) {
                GSON.toJson(o, w);
            }
        } catch (Exception e) {
            LOG.warn("Codex: could not save progress file {}", f, e);
        }
    }

    /** Leaving a world: write what is pending and forget it. */
    public static void unload() {
        save();
        world = null;
        lastLevel = null;
        READ.clear();
        SEEN.clear();
    }

    public static boolean isRead(CodexBook.Chapter c, int page) {
        ensure();
        return READ.contains(c.page(page).key(c.data));
    }

    public static void markRead(CodexBook.Chapter c, int page) {
        ensure();
        if (world != null && READ.add(c.page(page).key(c.data))) dirty = true;
    }

    public static int unread(CodexBook.Chapter c) {
        ensure();
        int n = 0;
        for (CodexChapter.Page p : c.data.pages) if (!READ.contains(p.key(c.data))) n++;
        return n;
    }

    public static boolean seen(ItemStack s) {
        ensure();
        return SEEN.contains(BuiltInRegistries.ITEM.getKey(s.getItem()).toString());
    }

    public static int held(CodexBook.Chapter c) {
        int n = 0;
        for (ItemStack s : c.items) if (seen(s)) n++;
        return n;
    }

    /** Notes every item currently in the player's inventory (called about once a second). */
    public static void scan(Player player) {
        ensure();
        if (world == null) return;
        Inventory inv = player.getInventory();
        for (List<ItemStack> part : List.of(inv.items, inv.armor, inv.offhand)) {
            for (ItemStack s : part) {
                if (s.isEmpty()) continue;
                if (SEEN.add(BuiltInRegistries.ITEM.getKey(s.getItem()).toString())) dirty = true;
            }
        }
    }
}
