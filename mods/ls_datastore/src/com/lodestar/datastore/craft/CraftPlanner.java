package com.lodestar.datastore.craft;

import com.lodestar.datastore.ItemKey;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Works out how to craft count of an item from what the network stores and the recipe cards it has: what to take from
 * storage, which crafts to run (sub-crafts first) and what is missing. Leftovers of one craft feed later ones; recipe
 * loops are cut off and reported as missing.
 */
public final class CraftPlanner {
    public static final int MAX_DEPTH = 24;
    public static final long MAX_CRAFTS = 100_000;

    public record Step(Pattern pattern, int crafts) {}

    public static final class Plan {
        public final ItemKey target;
        public final long count;
        public final Map<ItemKey, Long> used = new LinkedHashMap<>();
        public final Map<ItemKey, Long> crafted = new LinkedHashMap<>();
        public final Map<ItemKey, Long> missing = new LinkedHashMap<>();
        public final List<Step> steps = new ArrayList<>();
        long crafts;

        Plan(ItemKey target, long count) {
            this.target = target;
            this.count = count;
        }

        public boolean ok() {
            return missing.isEmpty() && !steps.isEmpty() && crafts <= MAX_CRAFTS;
        }

        public long totalCrafts() {
            return crafts;
        }

        /** Summary for the terminal's plan screen. */
        public CompoundTag toTag() {
            CompoundTag t = new CompoundTag();
            t.put("T", target.save());
            t.putLong("N", count);
            t.putBoolean("Ok", ok());
            t.putLong("Crafts", crafts);
            t.putInt("Steps", steps.size());
            Set<ItemKey> keys = new LinkedHashSet<>(crafted.keySet());
            keys.addAll(used.keySet());
            keys.addAll(missing.keySet());
            ListTag rows = new ListTag();
            for (ItemKey k : keys) {
                CompoundTag r = new CompoundTag();
                r.put("K", k.save());
                r.putLong("U", used.getOrDefault(k, 0L));
                r.putLong("C", crafted.getOrDefault(k, 0L));
                r.putLong("M", missing.getOrDefault(k, 0L));
                rows.add(r);
            }
            t.put("Rows", rows);
            return t;
        }
    }

    private final Map<ItemKey, Long> stock;
    private final Map<ItemKey, List<Pattern>> patterns;
    private final Map<ItemKey, Long> leftovers = new HashMap<>();
    private final Deque<ItemKey> chain = new ArrayDeque<>();
    private final Plan plan;

    private CraftPlanner(Map<ItemKey, Long> stock, Map<ItemKey, List<Pattern>> patterns, ItemKey target, long count) {
        this.stock = new HashMap<>(stock);
        this.patterns = patterns;
        this.plan = new Plan(target, count);
    }

    /** Plans crafting count of target (always crafted, never taken from storage). */
    public static Plan plan(Map<ItemKey, Long> stock, Map<ItemKey, List<Pattern>> patterns, ItemKey target, long count) {
        CraftPlanner p = new CraftPlanner(stock, patterns, target, count);
        if (patterns.containsKey(target)) p.craft(target, count);
        else p.plan.missing.put(target, count);
        return p.plan;
    }

    private void need(List<ItemKey> alts, long n) {
        for (ItemKey k : alts) {
            if (n <= 0) return;
            long have = leftovers.getOrDefault(k, 0L);
            long take = Math.min(have, n);
            if (take > 0) {
                leftovers.put(k, have - take);
                n -= take;
            }
        }
        for (ItemKey k : alts) {
            if (n <= 0) return;
            long have = stock.getOrDefault(k, 0L);
            long take = Math.min(have, n);
            if (take > 0) {
                stock.put(k, have - take);
                plan.used.merge(k, take, Long::sum);
                n -= take;
            }
        }
        if (n <= 0) return;
        if (chain.size() < MAX_DEPTH && plan.crafts <= MAX_CRAFTS) {
            for (ItemKey k : alts) {
                if (patterns.containsKey(k) && !chain.contains(k)) {
                    craft(k, n);
                    return;
                }
            }
        }
        plan.missing.merge(alts.get(0), n, Long::sum);
    }

    private void craft(ItemKey k, long n) {
        Pattern p = patterns.get(k).get(0);
        long crafts = (n + p.outCount - 1) / p.outCount;
        plan.crafts += crafts;
        if (plan.crafts > MAX_CRAFTS) {
            plan.missing.merge(k, n, Long::sum);
            return;
        }
        chain.push(k);
        for (Pattern.Req r : p.reqs) need(r.alts(), r.count() * crafts);
        chain.pop();
        plan.steps.add(new Step(p, (int) crafts));
        long made = crafts * p.outCount;
        plan.crafted.merge(k, made, Long::sum);
        if (made > n) leftovers.merge(k, made - n, Long::sum);
    }
}
