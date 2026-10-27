package com.lodestar.core.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
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

/**
 * An item that is opened (consumed) on right-click to roll a loot table into the player's inventory,
 * optionally granting experience. Used for supply crates, decoded data drives, treasure pouches...
 */
public class LootBagItem extends Item {
    private final ResourceLocation table;
    private final int xp;
    private final boolean foil;

    public LootBagItem(Properties props, ResourceLocation table, int xp, boolean foil) {
        super(props);
        this.table = table;
        this.xp = xp;
        this.foil = foil;
    }

    public LootBagItem(Properties props, ResourceLocation table) {
        this(props, table, 0, false);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return foil || super.isFoil(stack);
    }

    protected SoundEvent openSound() {
        return SoundEvents.BUNDLE_DROP_CONTENTS;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level instanceof ServerLevel) {
            ServerLevel sl = (ServerLevel) level;
            LootTable lt = sl.getServer().getLootData().getLootTable(table);
            LootParams params = new LootParams.Builder(sl)
                    .withParameter(LootContextParams.ORIGIN, player.position())
                    .withParameter(LootContextParams.THIS_ENTITY, player)
                    .withLuck(player.getLuck())
                    .create(LootContextParamSets.GIFT);
            int n = 0;
            for (ItemStack drop : lt.getRandomItems(params)) {
                n++;
                if (!player.getInventory().add(drop)) player.drop(drop, false);
            }
            if (xp > 0) player.giveExperiencePoints(xp);
            if (n == 0 && xp == 0) {
                player.displayClientMessage(Component.translatable("msg.ls_core.empty_bag").withStyle(ChatFormatting.GRAY), true);
            }
            level.playSound(null, player.blockPosition(), openSound(), SoundSource.PLAYERS, 1.0f, 0.9f + level.random.nextFloat() * 0.2f);
            if (!player.getAbilities().instabuild) stack.shrink(1);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }
}
