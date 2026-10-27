package com.lodestar.core.recipe;

import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;

public class ChanceStack {
    public final ItemStack stack;
    public final float chance;

    public ChanceStack(ItemStack stack, float chance) {
        this.stack = stack;
        this.chance = chance;
    }

    public ItemStack roll(RandomSource random) {
        if (chance >= 1.0f || random.nextFloat() < chance) {
            return stack.copy();
        }
        return ItemStack.EMPTY;
    }
}
