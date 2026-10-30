package com.lodestar.datastore;

import com.lodestar.core.grid.Grid;
import com.lodestar.core.grid.GridNodeBE;
import com.lodestar.datastore.craft.Pattern;
import com.lodestar.datastore.storage.IStorage;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A datastore network: every cable and device joined together. The single core pays the upkeep of every device each
 * tick; while it can, the network is online and its storages (disks in drive bays, inventories behind links) act as one
 * store, sorted by priority. Also collects the recipe cards of every fabricator for autocrafting.
 */
public class DataGrid extends Grid {
    public static final int ONLINE = 0, NO_CORE = 1, MANY_CORES = 2, NO_POWER = 3;

    @Nullable
    public CoreBE core;
    private int cores;
    private int status = NO_CORE;
    private int upkeep;
    public final List<DataNodeBE> devices = new ArrayList<>();
    public final List<DriveBayBE> drives = new ArrayList<>();
    public final List<LinkBE> links = new ArrayList<>();
    public final List<FabricatorBE> fabricators = new ArrayList<>();

    private List<IStorage> sources = List.of();
    private boolean sourcesDirty = true;
    private Map<ItemKey, Long> snapshot;
    private long snapshotAt = Long.MIN_VALUE;
    private Map<ItemKey, List<Pattern>> patterns;

    public DataGrid(Level level) {
        super(level);
    }

    @Override
    protected void onBuilt() {
        for (GridNodeBE<?> n : nodeEntities) {
            if (!(n instanceof DataNodeBE d)) continue;
            if (!(d instanceof CableBE)) devices.add(d);
            if (d instanceof CoreBE c) {
                cores++;
                core = c;
            }
            if (d instanceof DriveBayBE b) drives.add(b);
            if (d instanceof LinkBE l) links.add(l);
            if (d instanceof FabricatorBE f) fabricators.add(f);
        }
        status = cores == 0 ? NO_CORE : cores > 1 ? MANY_CORES : core.energy.getEnergyStored() > 0 ? ONLINE : NO_POWER;
    }

    @Override
    protected void tick() {
        upkeep = 0;
        for (DataNodeBE d : devices) upkeep += d.upkeep();
        if (cores == 1 && core != null && !core.isRemoved()) {
            int got = core.energy.consume(upkeep);
            status = got >= upkeep ? ONLINE : NO_POWER;
            if (status == ONLINE) core.tickJobs(this);
        } else {
            status = cores == 0 ? NO_CORE : MANY_CORES;
        }
        if (level.getGameTime() % 20 == 0) sourcesDirty = true;
    }

    public boolean online() {
        return status == ONLINE;
    }

    public int status() {
        return status;
    }

    public int upkeep() {
        return upkeep;
    }

    // ------------------------------------------------------------------ storage

    public void markSourcesDirty() {
        sourcesDirty = true;
        snapshot = null;
    }

    public void markPatternsDirty() {
        patterns = null;
    }

    public List<IStorage> sources() {
        if (sourcesDirty) {
            sourcesDirty = false;
            List<IStorage> l = new ArrayList<>();
            for (DriveBayBE d : drives) if (!d.isRemoved()) d.storages(l);
            for (LinkBE k : links) {
                IStorage s = k.isRemoved() ? null : k.storage();
                if (s != null) l.add(s);
            }
            l.sort(Comparator.comparingInt(IStorage::priority).reversed());
            sources = l;
        }
        return sources;
    }

    /** Stores up to n; returns how many were (or would be) accepted. Higher priority first; within a priority, storages that already hold the item first. */
    public long insert(ItemKey k, long n, boolean simulate) {
        if (!online() || n <= 0) return 0;
        List<IStorage> src = sources();
        long left = n;
        int i = 0;
        while (i < src.size() && left > 0) {
            int p = src.get(i).priority();
            int j = i;
            while (j < src.size() && src.get(j).priority() == p) j++;
            for (int pass = 0; pass < 2 && left > 0; pass++) {
                for (int s = i; s < j && left > 0; s++) {
                    IStorage st = src.get(s);
                    if ((st.count(k) > 0) == (pass == 0)) left -= st.insert(k, left, simulate);
                }
            }
            i = j;
        }
        if (!simulate && left != n) snapshot = null;
        return n - left;
    }

    /** Removes up to n; returns how many were (or would be) removed. Lowest priority storage gives first. */
    public long extract(ItemKey k, long n, boolean simulate) {
        if (!online() || n <= 0) return 0;
        List<IStorage> src = sources();
        long got = 0;
        for (int i = src.size() - 1; i >= 0 && got < n; i--) got += src.get(i).extract(k, n - got, simulate);
        if (!simulate && got > 0) snapshot = null;
        return got;
    }

    /** Inserts a stack; returns what did not fit. */
    public ItemStack insert(ItemStack s, boolean simulate) {
        if (s.isEmpty()) return ItemStack.EMPTY;
        long done = insert(ItemKey.of(s), s.getCount(), simulate);
        return done >= s.getCount() ? ItemStack.EMPTY : s.copyWithCount(s.getCount() - (int) done);
    }

    public ItemStack extractStack(ItemKey k, int n, boolean simulate) {
        long got = extract(k, Math.min(n, k.maxStack()), simulate);
        return got <= 0 ? ItemStack.EMPTY : k.stack(got);
    }

    /** Everything stored, recomputed at most once per tick (or after a change). Do not modify. */
    public Map<ItemKey, Long> snapshot() {
        long now = level.getGameTime();
        if (snapshot == null || snapshotAt != now) {
            Map<ItemKey, Long> m = new HashMap<>();
            if (online()) for (IStorage s : sources()) s.collect(m);
            snapshot = m;
            snapshotAt = now;
        }
        return snapshot;
    }

    public long count(ItemKey k) {
        return snapshot().getOrDefault(k, 0L);
    }

    // ------------------------------------------------------------------ autocrafting

    /** Every recipe card on the network by the item it makes. */
    public Map<ItemKey, List<Pattern>> patterns() {
        if (patterns == null) {
            Map<ItemKey, List<Pattern>> m = new LinkedHashMap<>();
            for (FabricatorBE f : fabricators) {
                if (f.isRemoved()) continue;
                for (Pattern p : f.patterns()) {
                    List<Pattern> l = m.computeIfAbsent(p.output, k -> new ArrayList<>());
                    boolean dup = false;
                    for (Pattern q : l) dup |= q.cardKey.equals(p.cardKey);
                    if (!dup) l.add(p);
                }
            }
            patterns = m;
        }
        return patterns;
    }

    public Set<ItemKey> craftable() {
        return online() ? patterns().keySet() : Collections.emptySet();
    }

    // ------------------------------------------------------------------ status

    public long[] diskStats() {
        long used = 0, total = 0, tu = 0, tt = 0, disks = 0;
        for (IStorage s : sources()) {
            if (s.bytesTotal() <= 0) continue;
            disks++;
            used += s.bytesUsed();
            total += s.bytesTotal();
            tu += s.typesUsed();
            tt += s.typesTotal();
        }
        return new long[]{used, total, tu, tt, disks};
    }
}
