package com.frontierworks.hearts;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerEvent;

import java.util.UUID;

/** Extra hearts live in the player's persisted NBT and are applied as a max-health modifier. */
public final class HeartEvents {
    public static final int MAX_EXTRA = 10;
    private static final UUID ID = UUID.fromString("a3f4b2c1-9d8e-4f7a-b6c5-d4e3f2a1b0c9");

    private HeartEvents() {}

    public static void init() {
        MinecraftForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedInEvent e) -> apply(e.getEntity()));
        MinecraftForge.EVENT_BUS.addListener((PlayerEvent.PlayerRespawnEvent e) -> apply(e.getEntity()));
        MinecraftForge.EVENT_BUS.addListener((PlayerEvent.PlayerChangedDimensionEvent e) -> apply(e.getEntity()));
    }

    private static CompoundTag data(Player p) {
        CompoundTag root = p.getPersistentData();
        if (!root.contains(Player.PERSISTED_NBT_TAG)) root.put(Player.PERSISTED_NBT_TAG, new CompoundTag());
        return root.getCompound(Player.PERSISTED_NBT_TAG);
    }

    public static int extra(Player p) {
        return data(p).getInt("fw_hearts");
    }

    public static void setExtra(Player p, int n) {
        data(p).putInt("fw_hearts", Math.max(0, Math.min(MAX_EXTRA, n)));
        apply(p);
    }

    public static void apply(Player p) {
        AttributeInstance inst = p.getAttribute(Attributes.MAX_HEALTH);
        if (inst == null) return;
        inst.removeModifier(ID);
        int n = extra(p);
        if (n > 0) inst.addPermanentModifier(new AttributeModifier(ID, "Heart containers", n * 2.0, AttributeModifier.Operation.ADDITION));
        if (p.getHealth() > p.getMaxHealth()) p.setHealth(p.getMaxHealth());
    }
}
