package com.frontierworks.explosives;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class ExplosiveItem extends Item {
    private final ThrownExplosive.Kind kind;

    public ExplosiveItem(Properties props, ThrownExplosive.Kind kind) {
        super(props);
        this.kind = kind;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), kind.bounce ? SoundEvents.TNT_PRIMED : SoundEvents.SNOWBALL_THROW,
                SoundSource.PLAYERS, 0.5f, 0.6f);
        if (!level.isClientSide) {
            ThrownExplosive e = new ThrownExplosive(Content.THROWN.get(), player, level, kind);
            e.setItem(stack.copyWithCount(1));
            e.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0f, kind.bounce ? 1.0f : 1.3f, 1.0f);
            level.addFreshEntity(e);
        }
        player.getCooldowns().addCooldown(this, 10);
        if (!player.getAbilities().instabuild) stack.shrink(1);
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }
}
