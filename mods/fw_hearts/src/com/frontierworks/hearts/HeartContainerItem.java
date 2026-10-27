package com.frontierworks.hearts;

import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class HeartContainerItem extends Item {
    public HeartContainerItem(Properties props) {
        super(props);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) return InteractionResultHolder.success(stack);
        int n = HeartEvents.extra(player);
        if (n >= HeartEvents.MAX_EXTRA) {
            player.displayClientMessage(Component.translatable("msg.fw_hearts.max"), true);
            return InteractionResultHolder.fail(stack);
        }
        HeartEvents.setExtra(player, n + 1);
        player.heal(2.0f);
        level.playSound(null, player.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1.0f, 1.4f);
        player.displayClientMessage(Component.translatable("msg.fw_hearts.gained", n + 1), true);
        if (!player.getAbilities().instabuild) stack.shrink(1);
        return InteractionResultHolder.consume(stack);
    }
}
