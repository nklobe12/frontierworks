package com.lodestar.transmutation;

import com.lodestar.core.data.PlayerData;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** A player's transmutation knowledge (items they have learned) and stored aether, kept in their player data. */
public final class Knowledge {
    public static final String KEY = "ls_transmutation";

    private Knowledge() {}

    private static CompoundTag data(Player p) {
        return PlayerData.get(p, KEY);
    }

    public static long aether(Player p) {
        return data(p).getLong("Aether");
    }

    public static void setAether(Player p, long v) {
        data(p).putLong("Aether", Math.max(0, v));
    }

    public static List<Item> known(Player p) {
        List<Item> out = new ArrayList<>();
        for (Tag t : data(p).getList("Known", Tag.TAG_STRING)) {
            Item it = BuiltInRegistries.ITEM.get(new ResourceLocation(t.getAsString()));
            if (it != Items.AIR) out.add(it);
        }
        return out;
    }

    public static boolean knows(Player p, Item item) {
        String id = BuiltInRegistries.ITEM.getKey(item).toString();
        for (Tag t : data(p).getList("Known", Tag.TAG_STRING)) if (t.getAsString().equals(id)) return true;
        return false;
    }

    /** Learns an item; returns true if it was new. */
    public static boolean learn(Player p, Item item) {
        if (knows(p, item)) return false;
        ListTag l = data(p).getList("Known", Tag.TAG_STRING);
        l.add(StringTag.valueOf(BuiltInRegistries.ITEM.getKey(item).toString()));
        data(p).put("Known", l);
        return true;
    }

    public static void forget(Player p, Item item) {
        String id = BuiltInRegistries.ITEM.getKey(item).toString();
        ListTag l = data(p).getList("Known", Tag.TAG_STRING);
        l.removeIf(t -> t.getAsString().equals(id));
        data(p).put("Known", l);
    }

    public static Set<String> knownIds(Player p) {
        Set<String> s = new LinkedHashSet<>();
        for (Tag t : data(p).getList("Known", Tag.TAG_STRING)) s.add(t.getAsString());
        return s;
    }

    public static void sync(ServerPlayer p) {
        PlayerData.sync(p, KEY);
    }
}
