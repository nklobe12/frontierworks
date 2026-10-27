package com.frontierworks.potions;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/** Plain effect; behaviours that need ticking (Purity, Magnetism) live in {@link AlchemyHooks}. */
public class AlchemyEffect extends MobEffect {
    public AlchemyEffect(MobEffectCategory category, int color) {
        super(category, color);
    }
}
