package com.lodestar.datastore.storage;

import com.lodestar.datastore.DiskItem;
import com.lodestar.datastore.ItemKey;

import java.util.Map;

/**
 * A disk in a drive bay. Each stored item costs one byte and each item type costs {@link DiskItem#bytesPerType()} bytes
 * and one type slot.
 */
public final class DiskStorage implements IStorage {
    private final DiskData.DiskInv inv;
    private final DiskItem tier;
    private final int priority;
    private final Runnable onChange;

    public DiskStorage(DiskData.DiskInv inv, DiskItem tier, int priority, Runnable onChange) {
        this.inv = inv;
        this.tier = tier;
        this.priority = priority;
        this.onChange = onChange;
    }

    public static long used(DiskData.DiskInv inv, DiskItem tier) {
        return inv.total() + (long) inv.types() * tier.bytesPerType();
    }

    @Override
    public long bytesUsed() {
        return used(inv, tier);
    }

    @Override
    public long bytesTotal() {
        return tier.bytes;
    }

    @Override
    public int typesUsed() {
        return inv.types();
    }

    @Override
    public int typesTotal() {
        return tier.types;
    }

    @Override
    public long insert(ItemKey k, long n, boolean simulate) {
        if (n <= 0) return 0;
        long free = tier.bytes - bytesUsed();
        if (inv.count(k) == 0) {
            if (inv.types() >= tier.types) return 0;
            free -= tier.bytesPerType();
        }
        long take = Math.max(0, Math.min(n, free));
        if (take > 0 && !simulate) {
            inv.add(k, take);
            onChange.run();
        }
        return take;
    }

    @Override
    public long extract(ItemKey k, long n, boolean simulate) {
        if (simulate) return Math.min(n, inv.count(k));
        long r = inv.remove(k, n);
        if (r > 0) onChange.run();
        return r;
    }

    @Override
    public long count(ItemKey k) {
        return inv.count(k);
    }

    @Override
    public void collect(Map<ItemKey, Long> into) {
        for (Map.Entry<ItemKey, Long> e : inv.items.entrySet()) into.merge(e.getKey(), e.getValue(), Long::sum);
    }

    @Override
    public int priority() {
        return priority;
    }
}
