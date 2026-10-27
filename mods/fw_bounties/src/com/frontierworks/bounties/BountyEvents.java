package com.frontierworks.bounties;

import com.frontierworks.core.util.FW;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;

public final class BountyEvents {
    /** target id, min, max, tier */
    private static final Object[][] TARGETS = {
            {"minecraft:zombie", 10, 20, 1}, {"minecraft:skeleton", 8, 16, 1}, {"minecraft:spider", 8, 14, 1}, {"minecraft:creeper", 5, 9, 1},
            {"minecraft:drowned", 6, 10, 1}, {"minecraft:husk", 6, 12, 1}, {"minecraft:stray", 6, 10, 1}, {"minecraft:slime", 8, 14, 1},
            {"minecraft:enderman", 3, 6, 2}, {"minecraft:witch", 2, 4, 2}, {"minecraft:pillager", 5, 9, 2}, {"minecraft:phantom", 3, 6, 2},
            {"minecraft:blaze", 4, 8, 2}, {"minecraft:ghast", 2, 3, 2}, {"minecraft:vindicator", 2, 4, 3}, {"minecraft:wither_skeleton", 3, 6, 3},
            {"minecraft:ravager", 1, 1, 3}, {"minecraft:evoker", 1, 2, 3},
            {"fw_mobs:rust_beetle", 6, 12, 1}, {"fw_mobs:cave_crawler", 6, 12, 1}, {"fw_mobs:bog_lurker", 5, 10, 1}, {"fw_mobs:sand_mummy", 5, 10, 1},
            {"fw_mobs:scrap_drone", 4, 8, 2}, {"fw_mobs:frost_wraith", 3, 6, 2}, {"fw_mobs:ember_imp", 4, 8, 2},
            {"fw_colossus:iron_colossus", 1, 1, 3}, {"fw_wraithking:wraith_king", 1, 1, 3}, {"fw_wyvern:sky_wyvern", 1, 1, 3},
    };

    private BountyEvents() {}

    public static void init() {
        MinecraftForge.EVENT_BUS.addListener(BountyEvents::onDeath);
    }

    private static List<ItemStack> contracts(Player p) {
        List<ItemStack> out = new ArrayList<>();
        for (ItemStack s : p.getInventory().items) if (s.getItem() instanceof ContractItem) out.add(s);
        for (ItemStack s : p.getInventory().offhand) if (s.getItem() instanceof ContractItem) out.add(s);
        return out;
    }

    static void useBoard(Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        if (held.getItem() instanceof ContractItem && ContractItem.complete(held)) {
            pay(player, ContractItem.tier(held));
            held.shrink(1);
            player.displayClientMessage(Component.translatable("msg.fw_bounties.paid"), true);
            player.level().playSound(null, player.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.8f, 1.2f);
            return;
        }
        for (ItemStack s : contracts(player)) {
            if (!ContractItem.complete(s)) {
                player.displayClientMessage(Component.translatable("msg.fw_bounties.have"), true);
                return;
            }
        }
        RandomSource r = player.getRandom();
        Object[] pick = null;
        for (int tries = 0; tries < 20 && pick == null; tries++) {
            Object[] t = TARGETS[r.nextInt(TARGETS.length)];
            if (ForgeRegistries.ENTITY_TYPES.containsKey(new ResourceLocation((String) t[0]))) pick = t;
        }
        if (pick == null) pick = TARGETS[0];
        int min = (Integer) pick[1], max = (Integer) pick[2];
        int n = min + (max > min ? r.nextInt(max - min + 1) : 0);
        ItemStack c = ContractItem.create((String) pick[0], n, (Integer) pick[3]);
        if (!player.getInventory().add(c)) player.drop(c, false);
        player.displayClientMessage(Component.translatable("msg.fw_bounties.new", n, ContractItem.targetName((String) pick[0])), false);
        player.level().playSound(null, player.blockPosition(), SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 1.0f, 1.0f);
    }

    private static void pay(Player player, int tier) {
        String coin = tier >= 3 ? "fw_coins:gold_coin" : "fw_coins:silver_coin";
        int count = tier == 1 ? 3 + player.getRandom().nextInt(4) : tier == 2 ? 6 + player.getRandom().nextInt(6) : 2 + player.getRandom().nextInt(3);
        ItemStack coins = FW.stack(coin, count);
        if (coins.isEmpty()) coins = new ItemStack(Items.EMERALD, count);
        give(player, coins);
        if (player.level() instanceof ServerLevel sl) {
            LootTable table = sl.getServer().getLootData().getLootTable(new ResourceLocation("fw_bounties", "rewards/tier" + Math.min(3, tier)));
            LootParams params = new LootParams.Builder(sl).withParameter(LootContextParams.ORIGIN, player.position())
                    .withParameter(LootContextParams.THIS_ENTITY, player).withLuck(player.getLuck()).create(LootContextParamSets.CHEST);
            for (ItemStack s : table.getRandomItems(params)) give(player, s);
        }
    }

    private static void give(Player p, ItemStack s) {
        if (!p.getInventory().add(s)) p.drop(s, false);
    }

    private static void onDeath(LivingDeathEvent event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer player)) return;
        ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(event.getEntity().getType());
        if (id == null) return;
        for (ItemStack s : contracts(player)) {
            if (ContractItem.complete(s) || !ContractItem.target(s).equals(id.toString())) continue;
            int p = ContractItem.progress(s) + 1;
            s.getOrCreateTag().putInt("Progress", p);
            if (p >= ContractItem.needed(s)) {
                player.displayClientMessage(Component.translatable("msg.fw_bounties.complete"), false);
                player.level().playSound(null, player.blockPosition(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 0.6f, 1.0f);
            } else {
                player.displayClientMessage(Component.translatable("msg.fw_bounties.progress", p, ContractItem.needed(s)), true);
            }
            return;
        }
    }
}
