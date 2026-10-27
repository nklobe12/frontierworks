package com.lodestar.lens.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** The searchable list of every item (creative variants included), with cached search text. */
public final class ItemList {
    private record Entry(ItemStack stack, String name, String mod, String modName, List<String> tags) {}

    private static List<Entry> entries;
    private static String lastQuery;
    private static List<ItemStack> lastResult = List.of();

    private ItemList() {}

    public static void clear() {
        entries = null;
        lastQuery = null;
    }

    private static List<Entry> entries() {
        if (entries != null) return entries;
        Minecraft mc = Minecraft.getInstance();
        List<ItemStack> stacks = new ArrayList<>();
        Set<Item> seen = new HashSet<>();
        try {
            if (mc.player != null && mc.level != null) {
                CreativeModeTabs.tryRebuildTabContents(mc.player.connection.enabledFeatures(), mc.player.canUseGameMasterBlocks(), mc.level.registryAccess());
                for (ItemStack s : CreativeModeTabs.searchTab().getDisplayItems()) {
                    stacks.add(s.copyWithCount(1));
                    seen.add(s.getItem());
                }
            }
        } catch (Throwable ignored) {
        }
        for (Item item : ForgeRegistries.ITEMS) {
            if (item == Items.AIR || seen.contains(item)) continue;
            stacks.add(new ItemStack(item));
        }
        Map<String, String> modNames = new LinkedHashMap<>();
        List<Entry> l = new ArrayList<>();
        for (ItemStack s : stacks) {
            var key = ForgeRegistries.ITEMS.getKey(s.getItem());
            String mod = key == null ? "" : key.getNamespace();
            String modName = modNames.computeIfAbsent(mod, m -> ModList.get().getModContainerById(m)
                    .map(c -> c.getModInfo().getDisplayName().toLowerCase(Locale.ROOT)).orElse(m));
            String name;
            try {
                name = s.getHoverName().getString().toLowerCase(Locale.ROOT);
            } catch (Throwable t) {
                name = key == null ? "" : key.getPath();
            }
            List<String> tags = new ArrayList<>();
            s.getTags().forEach(t -> tags.add(t.location().toString()));
            l.add(new Entry(s, name, mod, modName, tags));
        }
        entries = l;
        return l;
    }

    /** Items matching a query: words must all appear in the name; "@word" matches the mod, "#word" a tag. */
    public static List<ItemStack> search(String query) {
        String q = query.trim().toLowerCase(Locale.ROOT);
        if (q.equals(lastQuery) && entries != null) return lastResult;
        String[] words = q.isEmpty() ? new String[0] : q.split("\\s+");
        List<ItemStack> out = new ArrayList<>();
        for (Entry e : entries()) {
            boolean ok = true;
            for (String w : words) {
                if (w.startsWith("@")) {
                    String m = w.substring(1);
                    if (!e.mod.contains(m) && !e.modName.contains(m)) ok = false;
                } else if (w.startsWith("#")) {
                    String t = w.substring(1);
                    boolean any = false;
                    for (String tag : e.tags) if (tag.contains(t)) any = true;
                    if (!any) ok = false;
                } else if (!e.name.contains(w)) ok = false;
                if (!ok) break;
            }
            if (ok) out.add(e.stack);
        }
        lastQuery = q;
        lastResult = out;
        return out;
    }
}
