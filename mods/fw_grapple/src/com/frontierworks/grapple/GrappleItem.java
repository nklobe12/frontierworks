package com.frontierworks.grapple;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Fires a grappling hook. Keep holding the item to be reeled in; sneak to let go. */
public class GrappleItem extends Item {
    private final double speed;
    private final double pull;

    public GrappleItem(Properties props, double speed, double pull) {
        super(props);
        this.speed = speed;
        this.pull = pull;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide) {
            for (GrappleHookEntity old : level.getEntitiesOfClass(GrappleHookEntity.class, player.getBoundingBox().inflate(80), e -> e.getOwner() == player)) {
                old.discard();
            }
            GrappleHookEntity hook = new GrappleHookEntity(Content.GRAPPLE_HOOK_ENTITY.get(), player, level, pull);
            hook.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0f, (float) speed, 0.5f);
            level.addFreshEntity(hook);
            level.playSound(null, player.blockPosition(), SoundEvents.CROSSBOW_SHOOT, SoundSource.PLAYERS, 0.8f, 1.4f);
            stack.hurtAndBreak(1, player, p -> p.broadcastBreakEvent(hand));
        }
        player.getCooldowns().addCooldown(this, 10);
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }
}
