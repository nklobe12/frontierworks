package com.lodestar.core.data;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * World-wide data for every mod (waystone networks, mail, world events...), saved with the overworld.
 * {@link #get} returns the live tag for a key; call {@link #dirty} after changing it.
 */
public final class WorldData extends SavedData {
    private static final String NAME = "lodestar";
    private final CompoundTag root;

    private WorldData(CompoundTag root) {
        this.root = root;
    }

    private static WorldData of(MinecraftServer server) {
        ServerLevel ow = server.overworld();
        return ow.getDataStorage().computeIfAbsent(WorldData::new, () -> new WorldData(new CompoundTag()), NAME);
    }

    public static CompoundTag get(MinecraftServer server, String key) {
        WorldData d = of(server);
        if (!d.root.contains(key)) d.root.put(key, new CompoundTag());
        return d.root.getCompound(key);
    }

    public static void dirty(MinecraftServer server) {
        of(server).setDirty();
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        return root.copy();
    }
}
