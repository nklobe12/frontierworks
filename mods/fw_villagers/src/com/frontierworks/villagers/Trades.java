package com.frontierworks.villagers;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.VillagerTrades;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.village.VillagerTradesEvent;
import net.minecraftforge.event.village.WandererTradesEvent;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Trades for the Frontierworks professions (and a few for the wandering trader). Items are looked up by
 * id when the offer is created, so a trade whose item's mod is missing is simply skipped.
 */
public final class Trades {
    private Trades() {}

    private static final Map<String, List<Object[]>> TABLE = new HashMap<>();

    /** villager pays emeralds for items */
    private static Object[] buy(int level, String item, int count, int emeralds, int uses, int xp) {
        return new Object[]{level, item, count, "minecraft:emerald", emeralds, uses, xp};
    }

    /** villager sells items for emeralds */
    private static Object[] sell(int level, int emeralds, String item, int count, int uses, int xp) {
        return new Object[]{level, "minecraft:emerald", emeralds, item, count, uses, xp};
    }

    private static void add(String prof, Object[]... rows) {
        TABLE.computeIfAbsent(prof, k -> new ArrayList<>()).addAll(List.of(rows));
    }

    static {
        add("engineer",
                buy(1, "minecraft:copper_ingot", 10, 1, 16, 2), buy(1, "minecraft:redstone", 20, 1, 16, 2), sell(1, 2, "fw_parts:copper_wire", 6, 12, 1),
                sell(1, 3, "fw_core:wrench", 1, 4, 1), buy(2, "fw_metals:tin_ingot", 8, 1, 16, 5), sell(2, 4, "fw_circuits:basic_circuit", 2, 12, 5),
                sell(2, 5, "fw_parts:machine_frame", 1, 8, 5), buy(3, "fw_alloys:steel_ingot", 4, 1, 16, 10), sell(3, 6, "fw_batteries:battery_basic", 1, 8, 10),
                sell(3, 8, "fw_parts:motor", 1, 8, 10), sell(4, 12, "fw_circuits:advanced_circuit", 1, 8, 15), sell(4, 10, "fw_cables:electrum_cable", 8, 8, 15),
                sell(5, 24, "fw_circuits:elite_circuit", 1, 4, 30), sell(5, 20, "fw_batteries:battery_advanced", 1, 4, 30));
        add("surveyor",
                buy(1, "minecraft:paper", 24, 1, 16, 2), sell(1, 3, "fw_rope:rope", 16, 12, 1), sell(1, 2, "fw_flares:flare", 4, 12, 1),
                buy(2, "minecraft:compass", 1, 1, 12, 5), sell(2, 8, "fw_treasure:stash_map", 1, 6, 5), sell(2, 6, "fw_treasure:explorers_map", 1, 6, 5),
                buy(3, "fw_ruins:ancient_coin", 4, 1, 16, 10), buy(3, "fw_camps:bandit_mask", 2, 1, 16, 10), sell(3, 12, "fw_treasure:ruins_map", 1, 4, 10),
                sell(4, 14, "fw_treasure:dungeon_map", 1, 4, 15), sell(4, 12, "fw_treasure:tech_map", 1, 4, 15), buy(4, "fw_temples:sun_idol", 1, 12, 4, 15),
                buy(5, "fw_temples:jade_idol", 1, 12, 4, 30), buy(5, "fw_temples:frost_idol", 1, 12, 4, 30), sell(5, 16, "fw_treasure:treasure_detector", 1, 3, 30));
        add("miner",
                buy(1, "minecraft:coal", 16, 1, 16, 2), buy(1, "minecraft:raw_iron", 8, 1, 16, 2), sell(1, 1, "minecraft:torch", 16, 16, 1),
                buy(2, "fw_metals:raw_tin", 8, 1, 16, 5), buy(2, "fw_metals:raw_lead", 8, 1, 16, 5), sell(2, 6, "fw_hammers:iron_hammer", 1, 6, 5),
                buy(3, "fw_metals:raw_silver", 6, 1, 16, 10), sell(3, 8, "fw_explosives:dynamite", 4, 8, 10), sell(3, 10, "fw_scanner:ore_scanner", 1, 4, 10),
                buy(4, "fw_gems:ruby", 2, 1, 12, 15), buy(4, "fw_gems:sapphire", 2, 1, 12, 15), sell(4, 16, "fw_drills:electric_drill", 1, 3, 15),
                sell(5, 20, "minecraft:diamond_pickaxe", 1, 3, 30), buy(5, "fw_meteors:raw_meteoric_iron", 2, 1, 12, 30));
        add("botanist",
                buy(1, "minecraft:wheat_seeds", 24, 1, 16, 2), sell(1, 1, "fw_flora:blueberries", 6, 12, 1), sell(1, 2, "fw_flora:maple_sapling", 1, 8, 1),
                sell(2, 2, "fw_flora:aspen_sapling", 1, 8, 5), sell(2, 2, "fw_flora:baobab_sapling", 1, 8, 5), buy(2, "minecraft:bone_meal", 12, 1, 16, 5),
                sell(3, 3, "fw_rubber:rubber_sapling", 1, 8, 10), sell(3, 4, "fw_chemistry:fertilizer", 8, 12, 10), buy(3, "fw_caves:glowcap", 6, 1, 16, 10),
                sell(4, 6, "fw_caves:glowcap_stew", 1, 8, 15), sell(4, 4, "fw_skyislands:cloud_bloom", 2, 8, 15), sell(5, 12, "fw_farming:crop_harvester", 1, 3, 30),
                sell(5, 3, "fw_flora:maple_syrup", 2, 12, 30));
    }

    public static void init() {
        MinecraftForge.EVENT_BUS.addListener(Trades::onVillager);
        MinecraftForge.EVENT_BUS.addListener(Trades::onWanderer);
    }

    private static void onVillager(VillagerTradesEvent event) {
        ResourceLocation id = ForgeRegistries.VILLAGER_PROFESSIONS.getKey(event.getType());
        if (id == null || !"fw_villagers".equals(id.getNamespace())) return;
        List<Object[]> rows = TABLE.get(id.getPath());
        if (rows == null) return;
        for (Object[] r : rows) {
            int level = (Integer) r[0];
            List<VillagerTrades.ItemListing> list = event.getTrades().get(level);
            if (list != null) list.add(new Offer((String) r[1], (Integer) r[2], (String) r[3], (Integer) r[4], (Integer) r[5], (Integer) r[6]));
        }
    }

    private static void onWanderer(WandererTradesEvent event) {
        event.getGenericTrades().add(new Offer("minecraft:emerald", 4, "fw_treasure:stash_map", 1, 4, 1));
        event.getGenericTrades().add(new Offer("minecraft:emerald", 2, "fw_flora:raspberries", 8, 8, 1));
        event.getGenericTrades().add(new Offer("minecraft:emerald", 3, "fw_skyislands:aether_crystal", 1, 6, 1));
        event.getRareTrades().add(new Offer("minecraft:emerald", 10, "fw_treasure:dungeon_map", 1, 2, 1));
        event.getRareTrades().add(new Offer("minecraft:emerald", 16, "fw_meteors:star_fragment", 1, 2, 1));
    }

    @Nullable
    private static Item item(String id) {
        Item i = ForgeRegistries.ITEMS.getValue(new ResourceLocation(id));
        return i == null || i == Items.AIR ? null : i;
    }

    private static final class Offer implements VillagerTrades.ItemListing {
        private final String cost, result;
        private final int costCount, resultCount, uses, xp;

        Offer(String cost, int costCount, String result, int resultCount, int uses, int xp) {
            this.cost = cost;
            this.costCount = costCount;
            this.result = result;
            this.resultCount = resultCount;
            this.uses = uses;
            this.xp = xp;
        }

        @Nullable
        @Override
        public MerchantOffer getOffer(Entity trader, RandomSource random) {
            Item c = item(cost), r = item(result);
            if (c == null || r == null) return null;
            return new MerchantOffer(new ItemStack(c, costCount), new ItemStack(r, resultCount), uses, xp, 0.05f);
        }
    }
}
