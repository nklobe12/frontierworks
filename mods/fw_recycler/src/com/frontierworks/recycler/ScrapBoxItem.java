package com.frontierworks.recycler;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

/** Right-click to open: rolls the fw_recycler:scrap_box loot table. Sneak to open the whole stack. */
public class ScrapBoxItem extends Item {
    private static final ResourceLocation TABLE = new ResourceLocation("fw_recycler", "scrap_box");

    public ScrapBoxItem(Properties props) {
        super(props);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level instanceof ServerLevel) {
            ServerLevel sl = (ServerLevel) level;
            LootTable table = sl.getServer().getLootData().getLootTable(TABLE);
            int count = player.isShiftKeyDown() ? stack.getCount() : 1;
            for (int i = 0; i < count; i++) {
                LootParams params = new LootParams.Builder(sl).withParameter(LootContextParams.ORIGIN, player.position())
                        .withParameter(LootContextParams.THIS_ENTITY, player).withLuck(player.getLuck()).create(LootContextParamSets.GIFT);
                for (ItemStack out : table.getRandomItems(params)) {
                    if (!player.getInventory().add(out)) {
                        player.drop(out, false);
                    }
                }
            }
            if (!player.getAbilities().instabuild) stack.shrink(count);
            level.playSound(null, player.blockPosition(), SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.6f, 0.8f);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }
}
