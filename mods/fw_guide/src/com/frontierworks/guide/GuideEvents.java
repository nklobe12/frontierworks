package com.frontierworks.guide;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerEvent;

/** Gives every player a Field Guide the first time they join. */
public final class GuideEvents {
    private GuideEvents() {}

    public static void init() {
        MinecraftForge.EVENT_BUS.addListener(GuideEvents::onLogin);
    }

    private static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        Player p = event.getEntity();
        CompoundTag root = p.getPersistentData();
        if (!root.contains(Player.PERSISTED_NBT_TAG)) root.put(Player.PERSISTED_NBT_TAG, new CompoundTag());
        CompoundTag data = root.getCompound(Player.PERSISTED_NBT_TAG);
        if (data.getBoolean("fw_guide_given")) return;
        data.putBoolean("fw_guide_given", true);
        ItemStack guide = new ItemStack(Content.FIELD_GUIDE.get());
        if (!p.getInventory().add(guide)) p.drop(guide, false);
    }
}
