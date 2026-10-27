package com.lodestar.core.recipe;

import net.minecraft.world.item.crafting.Ingredient;

/** An ingredient with a count. Count 0 marks a catalyst (a mold or die) that must be present but is not used up. */
public class CountedIngredient {
    public final Ingredient ingredient;
    public final int count;

    public CountedIngredient(Ingredient ingredient, int count) {
        this.ingredient = ingredient;
        this.count = Math.max(0, count);
    }

    public boolean isCatalyst() {
        return count == 0;
    }
}
