package com.frontierworks.waystones;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.List;

/** Per-player list of activated waystones, kept in persisted player data so it survives death. */
public final class WaystoneData {
    private static final String KEY = "fw_waystones";

    private WaystoneData() {}

    public static class Entry {
        public final BlockPos pos;
        public final String dim;
        public final String name;

        public Entry(BlockPos pos, String dim, String name) {
            this.pos = pos;
            this.dim = dim;
            this.name = name;
        }
    }

    private static CompoundTag root(Player p) {
        CompoundTag data = p.getPersistentData();
        if (!data.contains(Player.PERSISTED_NBT_TAG)) data.put(Player.PERSISTED_NBT_TAG, new CompoundTag());
        return data.getCompound(Player.PERSISTED_NBT_TAG);
    }

    public static List<Entry> list(Player p) {
        List<Entry> out = new ArrayList<>();
        ListTag l = root(p).getList(KEY, Tag.TAG_COMPOUND);
        for (int i = 0; i < l.size(); i++) {
            CompoundTag t = l.getCompound(i);
            out.add(new Entry(BlockPos.of(t.getLong("Pos")), t.getString("Dim"), t.getString("Name")));
        }
        return out;
    }

    public static boolean add(Player p, BlockPos pos, String dim, String name) {
        for (Entry e : list(p)) {
            if (e.pos.equals(pos) && e.dim.equals(dim)) return false;
        }
        CompoundTag root = root(p);
        ListTag l = root.getList(KEY, Tag.TAG_COMPOUND);
        CompoundTag t = new CompoundTag();
        t.putLong("Pos", pos.asLong());
        t.putString("Dim", dim);
        t.putString("Name", name);
        l.add(t);
        root.put(KEY, l);
        p.getPersistentData().put(Player.PERSISTED_NBT_TAG, root);
        return true;
    }

    public static void remove(Player p, BlockPos pos, String dim) {
        CompoundTag root = root(p);
        ListTag l = root.getList(KEY, Tag.TAG_COMPOUND);
        for (int i = l.size() - 1; i >= 0; i--) {
            CompoundTag t = l.getCompound(i);
            if (t.getLong("Pos") == pos.asLong() && t.getString("Dim").equals(dim)) l.remove(i);
        }
        root.put(KEY, l);
        p.getPersistentData().put(Player.PERSISTED_NBT_TAG, root);
    }
}
