package com.frontierworks.skyislands;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingFallEvent;

/** Feather charm: negates fall damage while anywhere in the inventory, costing one durability per saved fall. */
public class CharmItem extends Item {
    public CharmItem(Properties props) {
        super(props);
    }

    public static void init() {
        MinecraftForge.EVENT_BUS.addListener(CharmItem::onFall);
    }

    private static void onFall(LivingFallEvent event) {
        if (!(event.getEntity() instanceof Player) || event.getDistance() <= 3.0f) return;
        Player player = (Player) event.getEntity();
        for (ItemStack stack : player.getInventory().items) {
            if (stack.getItem() instanceof CharmItem) {
                event.setCanceled(true);
                if (!player.level().isClientSide && !player.getAbilities().instabuild) {
                    stack.hurtAndBreak(1, player, p -> {});
                }
                return;
            }
        }
        ItemStack off = player.getOffhandItem();
        if (off.getItem() instanceof CharmItem) {
            event.setCanceled(true);
            if (!player.level().isClientSide && !player.getAbilities().instabuild) off.hurtAndBreak(1, player, p -> {});
        }
    }
}
