package com.lodestar.datastore.client;

import com.lodestar.core.net.CoreNet;
import com.lodestar.datastore.ItemKey;
import com.lodestar.datastore.menu.TerminalMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** Client copy of the open terminal's network contents, plans and jobs, filled by server packets. */
public final class TermClient {
    private TermClient() {}

    public static int window = -1;
    public static int status;
    public static final Map<ItemKey, Long> items = new HashMap<>();
    public static final Set<ItemKey> craftable = new HashSet<>();
    public static int version;
    @Nullable
    public static CompoundTag plan;
    public static ListTag jobs = new ListTag();
    public static int jobsVersion;

    static void apply(CompoundTag t) {
        int w = t.getInt("W");
        if (t.getBoolean("Full") || w != window) {
            items.clear();
            craftable.clear();
            window = w;
        }
        status = t.getInt("St");
        for (Tag e : t.getList("E", Tag.TAG_COMPOUND)) {
            CompoundTag c = (CompoundTag) e;
            ItemKey k = ItemKey.load(c.getCompound("K"));
            if (k == null) continue;
            long n = c.getLong("N");
            if (n < 0) {
                items.remove(k);
                craftable.remove(k);
                continue;
            }
            items.put(k, n);
            if (c.getBoolean("C")) craftable.add(k);
            else craftable.remove(k);
        }
        version++;
    }

    static void plan(CompoundTag t) {
        plan = t;
    }

    static void jobs(CompoundTag t) {
        jobs = t.getList("Jobs", Tag.TAG_COMPOUND);
        jobsVersion++;
    }

    /** Sends a terminal action for the open terminal menu. */
    public static void send(int action, @Nullable ItemKey key, int button, long count) {
        if (!(Minecraft.getInstance().player != null && Minecraft.getInstance().player.containerMenu instanceof TerminalMenu m)) return;
        CompoundTag t = new CompoundTag();
        t.putInt("W", m.containerId);
        t.putInt("A", action);
        if (key != null) t.put("K", key.save());
        t.putInt("B", button);
        t.putLong("N", count);
        CoreNet.sendTag(TerminalMenu.ACTION, t);
    }

    public static void cancel(int job) {
        if (!(Minecraft.getInstance().player != null && Minecraft.getInstance().player.containerMenu instanceof TerminalMenu m)) return;
        CompoundTag t = new CompoundTag();
        t.putInt("W", m.containerId);
        t.putInt("A", TerminalMenu.A_CANCEL);
        t.putInt("J", job);
        CoreNet.sendTag(TerminalMenu.ACTION, t);
    }

    public static String fmt(long n) {
        if (n < 1000) return Long.toString(n);
        if (n < 10_000) return String.format("%.1fk", n / 1000.0).replace(".0k", "k");
        if (n < 1_000_000) return (n / 1000) + "k";
        if (n < 10_000_000) return String.format("%.1fM", n / 1_000_000.0).replace(".0M", "M");
        if (n < 1_000_000_000) return (n / 1_000_000) + "M";
        return (n / 1_000_000_000) + "G";
    }
}
