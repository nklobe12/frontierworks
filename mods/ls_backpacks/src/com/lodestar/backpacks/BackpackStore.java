package com.lodestar.backpacks;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Every backpack's contents, by backpack id, saved with the overworld. */
public final class BackpackStore extends SavedData {
    private static final String NAME = "ls_backpacks";
    private final Map<UUID, Contents> map = new HashMap<>();

    private BackpackStore() {}

    public static BackpackStore get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(BackpackStore::load, BackpackStore::new, NAME);
    }

    private static BackpackStore load(CompoundTag tag) {
        BackpackStore s = new BackpackStore();
        CompoundTag packs = tag.getCompound("Packs");
        for (String k : packs.getAllKeys()) {
            if (packs.get(k) == null || packs.get(k).getId() != Tag.TAG_COMPOUND) continue;
            s.map.put(UUID.fromString(k), Contents.load(packs.getCompound(k), s::setDirty));
        }
        return s;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        CompoundTag packs = new CompoundTag();
        map.forEach((k, v) -> packs.put(k.toString(), v.save()));
        tag.put("Packs", packs);
        return tag;
    }

    /** Contents for a backpack id, created or grown to hold {@code size} slots. */
    public Contents contents(UUID id, int size) {
        Contents c = map.get(id);
        if (c == null) {
            c = new Contents(size, this::setDirty);
            map.put(id, c);
            setDirty();
        } else if (c.items.getSlots() < size) {
            c = c.resized(size);
            map.put(id, c);
            setDirty();
        }
        return c;
    }

    public boolean exists(UUID id) {
        return map.containsKey(id);
    }
}
