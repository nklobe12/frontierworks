package com.lodestar.datastore.storage;

import com.lodestar.datastore.ItemKey;

import java.util.Map;

/** One place the network can put items: a disk in a drive bay, or an inventory behind an inventory link. */
public interface IStorage {
    /** Returns how many of n were (or would be) accepted. */
    long insert(ItemKey k, long n, boolean simulate);

    /** Returns how many of n were (or would be) removed. */
    long extract(ItemKey k, long n, boolean simulate);

    long count(ItemKey k);

    /** Adds everything this storage holds to the map. */
    void collect(Map<ItemKey, Long> into);

    int priority();

    /** Byte accounting for status screens (0 for storages that do not count bytes). */
    default long bytesUsed() {
        return 0;
    }

    default long bytesTotal() {
        return 0;
    }

    default int typesUsed() {
        return 0;
    }

    default int typesTotal() {
        return 0;
    }
}
