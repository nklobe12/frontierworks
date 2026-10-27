package com.frontierworks.wraithking;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;

public class WraithBlade extends SwordItem {
    public WraithBlade(Properties props) {
        super(Tiers.NETHERITE, 6, -2.4f, props);
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        attacker.heal(2.0f);
        target.addEffect(new MobEffectInstance(MobEffects.WITHER, 60, 0), attacker);
        return super.hurtEnemy(stack, target, attacker);
    }
}
