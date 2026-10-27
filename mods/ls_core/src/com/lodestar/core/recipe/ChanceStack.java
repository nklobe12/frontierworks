package com.lodestar.core.recipe;

import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;

/** An output stack with a chance (1.0 = always). Chances above 1 give extra items (1.5 = one plus 50% for another). */
public class ChanceStack {
    public final ItemStack stack;
    public final float chance;

    public ChanceStack(ItemStack stack, float chance) {
        this.stack = stack;
        this.chance = chance;
    }

    public ItemStack roll(RandomSource random) {
        return roll(random, 0f);
    }

    /** Rolls the output; bonus (from output augments) raises chances below 1 by that fraction of the chance. */
    public ItemStack roll(RandomSource random, float bonus) {
        if (chance >= 1.0f && chance == Math.floor(chance)) {
            ItemStack out = stack.copy();
            out.setCount(stack.getCount() * (int) chance);
            return out;
        }
        float c = chance < 1f ? Math.min(1f, chance * (1f + bonus)) : chance;
        int whole = (int) Math.floor(c);
        float frac = c - whole;
        int n = whole + (random.nextFloat() < frac ? 1 : 0);
        if (n <= 0) return ItemStack.EMPTY;
        ItemStack out = stack.copy();
        out.setCount(Math.min(out.getMaxStackSize(), stack.getCount() * n));
        return out;
    }

    public boolean isGuaranteed() {
        return chance >= 1f;
    }
}
