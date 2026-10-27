package com.frontierworks.hearts;

import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class CursedHeartItem extends Item {
    public CursedHeartItem(Properties props) {
        super(props);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) return InteractionResultHolder.success(stack);
        int n = HeartEvents.extra(player);
        if (n <= 0) {
            player.displayClientMessage(Component.translatable("msg.fw_hearts.none"), true);
            return InteractionResultHolder.fail(stack);
        }
        HeartEvents.setExtra(player, n - 1);
        player.displayClientMessage(Component.translatable("msg.fw_hearts.removed", n - 1), true);
        ItemStack back = new ItemStack(Content.HEART_CONTAINER.get());
        if (!player.getInventory().add(back)) player.drop(back, false);
        if (!player.getAbilities().instabuild) stack.shrink(1);
        return InteractionResultHolder.consume(stack);
    }
}
