package com.frontierworks.ruins;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Ancient tablet: deciphering it (right-click) grants experience. */
public class TabletItem extends Item {
    public TabletItem(Properties props) {
        super(props);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide) {
            int xp = 12 + level.random.nextInt(24);
            player.giveExperiencePoints(xp);
            level.playSound(null, player.blockPosition(), SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 1.0f, 0.8f);
            player.displayClientMessage(Component.translatable("msg.fw_ruins.deciphered", xp).withStyle(ChatFormatting.GOLD), true);
            if (!player.getAbilities().instabuild) stack.shrink(1);
            player.getCooldowns().addCooldown(this, 20);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }
}
