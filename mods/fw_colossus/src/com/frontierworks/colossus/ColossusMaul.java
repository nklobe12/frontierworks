package com.frontierworks.colossus;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** Huge maul: right-click to slam the ground, hurling nearby enemies away. */
public class ColossusMaul extends SwordItem {
    public ColossusMaul(Properties props) {
        super(Tiers.NETHERITE, 10, -3.3f, props);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide) {
            for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(5.0, 1.5, 5.0), e -> e != player && !e.isAlliedTo(player))) {
                e.hurt(player.damageSources().playerAttack(player), 9.0f);
                Vec3 push = e.position().subtract(player.position()).normalize().scale(1.2);
                e.push(push.x, 0.6, push.z);
            }
            if (level instanceof ServerLevel sl) sl.sendParticles(ParticleTypes.EXPLOSION, player.getX(), player.getY() + 0.3, player.getZ(), 6, 2.0, 0.2, 2.0, 0.0);
            level.playSound(null, player.blockPosition(), SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 0.8f, 0.7f);
            stack.hurtAndBreak(3, player, p -> p.broadcastBreakEvent(hand));
        }
        player.getCooldowns().addCooldown(this, 100);
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }
}
