package com.frontierworks.coins;

import com.frontierworks.core.util.FW;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.npc.VillagerTrades;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.village.WandererTradesEvent;

public final class CoinEvents {
    private CoinEvents() {}

    public static void init() {
        MinecraftForge.EVENT_BUS.addListener(CoinEvents::onDrops);
        MinecraftForge.EVENT_BUS.addListener(CoinEvents::onWanderer);
    }

    private static void drop(LivingEntity e, LivingDropsEvent event, ItemStack stack) {
        if (!stack.isEmpty()) event.getDrops().add(new ItemEntity(e.level(), e.getX(), e.getY() + 0.3, e.getZ(), stack));
    }

    private static void onDrops(LivingDropsEvent event) {
        LivingEntity e = event.getEntity();
        if (e.level().isClientSide || !(event.getSource().getEntity() instanceof Player)) return;
        if (e.getType().getCategory() != MobCategory.MONSTER) return;
        float hp = e.getMaxHealth();
        var r = e.getRandom();
        if (hp >= 150) {
            drop(e, event, new ItemStack(Content.GOLD_COIN.get(), 3 + r.nextInt(6)));
            drop(e, event, new ItemStack(Content.PLATINUM_COIN.get(), 1 + r.nextInt(2)));
            return;
        }
        if (r.nextFloat() < 0.35f) drop(e, event, new ItemStack(Content.COPPER_COIN.get(), 1 + r.nextInt(2 + event.getLootingLevel())));
        if (r.nextFloat() < 0.04f + 0.02f * event.getLootingLevel()) drop(e, event, new ItemStack(Content.SILVER_COIN.get()));
        if (hp >= 40 && r.nextFloat() < 0.25f) drop(e, event, new ItemStack(Content.GOLD_COIN.get()));
    }

    private static VillagerTrades.ItemListing sell(ItemStack price, String item, int count, int uses) {
        return (trader, rand) -> {
            ItemStack out = FW.stack(item, count);
            return out.isEmpty() ? null : new MerchantOffer(price.copy(), out, uses, 5, 0.05f);
        };
    }

    private static void onWanderer(WandererTradesEvent event) {
        ItemStack silver4 = new ItemStack(Content.SILVER_COIN.get(), 4);
        ItemStack gold = new ItemStack(Content.GOLD_COIN.get(), 1);
        ItemStack gold3 = new ItemStack(Content.GOLD_COIN.get(), 3);
        event.getGenericTrades().add(sell(new ItemStack(Content.COPPER_COIN.get(), 12), "minecraft:emerald", 1, 16));
        event.getGenericTrades().add(sell(silver4, "fw_treasure:explorers_map", 1, 3));
        event.getGenericTrades().add(sell(silver4, "fw_food:tomato_seeds", 4, 8));
        event.getGenericTrades().add(sell(gold, "minecraft:diamond", 2, 6));
        event.getGenericTrades().add(sell(gold, "fw_hearts:heart_fragment", 1, 2));
        event.getRareTrades().add(sell(gold3, "minecraft:totem_of_undying", 1, 1));
        event.getRareTrades().add(sell(new ItemStack(Content.PLATINUM_COIN.get(), 2), "fw_hearts:heart_container", 1, 1));
        event.getRareTrades().add(sell(gold3, "fw_relics:lucky_coin", 1, 1));
    }

    @SuppressWarnings("unused")
    private static boolean unused() {
        return Items.AIR == null;
    }
}
