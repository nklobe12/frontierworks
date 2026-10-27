package com.lodestar.core.data;

import com.lodestar.core.net.CoreNet;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerEvent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Per-player data for every mod, kept under the player's persisted NBT so it survives death and dimension
 * changes. Each mod uses its own key ("ls_transmutation", "ls_quests"...). On the server {@link #get} returns
 * the live tag; after changing it call {@link #sync} to update that player's client copy. On the client
 * {@link #get} returns the last synced copy for the local player.
 */
public final class PlayerData {
    private static final String ROOT = "lodestar";
    private static final Map<String, CompoundTag> CLIENT = new HashMap<>();
    private static final Map<String, List<Consumer<CompoundTag>>> CLIENT_LISTENERS = new HashMap<>();
    /** keys that are synced automatically on login/respawn/dimension change */
    private static final List<String> AUTO_SYNC = new CopyOnWriteArrayList<>();

    private PlayerData() {}

    public static void init() {
        MinecraftForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedInEvent e) -> syncAll(e.getEntity()));
        MinecraftForge.EVENT_BUS.addListener((PlayerEvent.PlayerRespawnEvent e) -> syncAll(e.getEntity()));
        MinecraftForge.EVENT_BUS.addListener((PlayerEvent.PlayerChangedDimensionEvent e) -> syncAll(e.getEntity()));
    }

    /** Client setup: receive synced data. */
    public static void initClient() {
        CoreNet.registerClient("ls_core:pdata", tag -> {
            String key = tag.getString("k");
            CompoundTag data = tag.getCompound("d");
            CLIENT.put(key, data);
            List<Consumer<CompoundTag>> ls = CLIENT_LISTENERS.get(key);
            if (ls != null) ls.forEach(l -> l.accept(data));
        });
    }

    /** Marks a key to be sent to the client whenever the player joins, respawns or changes dimension. */
    public static void autoSync(String key) {
        if (!AUTO_SYNC.contains(key)) AUTO_SYNC.add(key);
    }

    /** Client: run a callback whenever data for this key arrives. */
    public static void onClientSync(String key, Consumer<CompoundTag> listener) {
        CLIENT_LISTENERS.computeIfAbsent(key, k -> new CopyOnWriteArrayList<>()).add(listener);
    }

    public static CompoundTag get(Player player, String key) {
        if (player.level().isClientSide) {
            return CLIENT.computeIfAbsent(key, k -> new CompoundTag());
        }
        CompoundTag persisted = player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
        CompoundTag root = persisted.getCompound(ROOT);
        CompoundTag data = root.getCompound(key);
        if (!root.contains(key)) root.put(key, data);
        if (!persisted.contains(ROOT)) persisted.put(ROOT, root);
        if (!player.getPersistentData().contains(Player.PERSISTED_NBT_TAG)) player.getPersistentData().put(Player.PERSISTED_NBT_TAG, persisted);
        return data;
    }

    public static void sync(ServerPlayer player, String key) {
        CompoundTag msg = new CompoundTag();
        msg.putString("k", key);
        msg.put("d", get(player, key).copy());
        CoreNet.toPlayer(player, "ls_core:pdata", msg);
    }

    private static void syncAll(Player p) {
        if (p instanceof ServerPlayer sp) {
            for (String k : AUTO_SYNC) sync(sp, k);
        }
    }

    /** Client: forget everything when leaving a world. */
    public static void clearClient() {
        CLIENT.clear();
    }
}
