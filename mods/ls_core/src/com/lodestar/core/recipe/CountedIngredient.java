package com.lodestar.core.recipe;

import net.minecraft.world.item.crafting.Ingredient;

public class CountedIngredient {
    public final Ingredient ingredient;
    public final int count;

    public CountedIngredient(Ingredient ingredient, int count) {
        this.ingredient = ingredient;
        this.count = Math.max(1, count);
    }
}
