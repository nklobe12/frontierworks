package com.lodestar.codex;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** The Field Codex book. Using it opens the Codex screen on the client. */
public class CodexItem extends Item {
    /** Set by the client setup; opens the Codex where the reader left off. */
    public static Runnable openClient = () -> {};

    public CodexItem(Properties props) {
        super(props);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) {
            openClient.run();
        } else {
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 0.8f, 1.0f);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }
}
