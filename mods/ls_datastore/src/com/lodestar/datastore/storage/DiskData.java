package com.lodestar.datastore.storage;

import com.lodestar.datastore.ItemKey;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * The contents of every data disk in the world, keyed by the disk's UUID. Disk items only carry their id (and a small
 * summary for tooltips), so a full disk never makes the item, the drive bay or a packet heavy.
 */
public final class DiskData extends SavedData {
    private static final String NAME = "ls_datastore_disks";
    private final Map<UUID, DiskInv> disks = new HashMap<>();

    public static DiskData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(DiskData::load, DiskData::new, NAME);
    }

    public DiskInv inv(UUID id) {
        return disks.computeIfAbsent(id, k -> new DiskInv(this));
    }

    public boolean has(UUID id) {
        return disks.containsKey(id);
    }

    /** One disk's items. Counts are longs; byte and type accounting is done by {@link DiskStorage} with the disk's tier. */
    public static final class DiskInv {
        final DiskData owner;
        public final LinkedHashMap<ItemKey, Long> items = new LinkedHashMap<>();
        long total;

        DiskInv(DiskData owner) {
            this.owner = owner;
        }

        public long total() {
            return total;
        }

        public int types() {
            return items.size();
        }

        public long count(ItemKey k) {
            Long v = items.get(k);
            return v == null ? 0 : v;
        }

        void add(ItemKey k, long n) {
            if (n <= 0) return;
            items.merge(k, n, Long::sum);
            total += n;
            owner.setDirty();
        }

        long remove(ItemKey k, long n) {
            long have = count(k);
            long take = Math.min(have, n);
            if (take <= 0) return 0;
            if (take == have) items.remove(k);
            else items.put(k, have - take);
            total -= take;
            owner.setDirty();
            return take;
        }
    }

    private static DiskData load(CompoundTag tag) {
        DiskData d = new DiskData();
        for (Tag t : tag.getList("Disks", Tag.TAG_COMPOUND)) {
            CompoundTag dt = (CompoundTag) t;
            if (!dt.hasUUID("Id")) continue;
            DiskInv inv = new DiskInv(d);
            for (Tag it : dt.getList("Items", Tag.TAG_COMPOUND)) {
                CompoundTag e = (CompoundTag) it;
                ItemKey k = ItemKey.load(e.getCompound("K"));
                long n = e.getLong("N");
                if (k != null && n > 0) {
                    inv.items.merge(k, n, Long::sum);
                    inv.total += n;
                }
            }
            d.disks.put(dt.getUUID("Id"), inv);
        }
        return d;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag list = new ListTag();
        for (Map.Entry<UUID, DiskInv> e : disks.entrySet()) {
            if (e.getValue().items.isEmpty()) continue;
            CompoundTag dt = new CompoundTag();
            dt.putUUID("Id", e.getKey());
            ListTag items = new ListTag();
            for (Map.Entry<ItemKey, Long> it : e.getValue().items.entrySet()) {
                CompoundTag c = new CompoundTag();
                c.put("K", it.getKey().save());
                c.putLong("N", it.getValue());
                items.add(c);
            }
            dt.put("Items", items);
            list.add(dt);
        }
        tag.put("Disks", list);
        return tag;
    }
}
