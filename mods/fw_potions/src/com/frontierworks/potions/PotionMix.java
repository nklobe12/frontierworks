package com.frontierworks.potions;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraftforge.common.brewing.IBrewingRecipe;

import java.util.function.Supplier;

/** Brewing stand recipe: (potion of `from`) + ingredient -> potion of `to`, preserving bottle type. */
public class PotionMix implements IBrewingRecipe {
    private final Supplier<Potion> from;
    private final Item ingredient;
    private final Supplier<Potion> to;

    public PotionMix(Supplier<Potion> from, Item ingredient, Supplier<Potion> to) {
        this.from = from;
        this.ingredient = ingredient;
        this.to = to;
    }

    @Override
    public boolean isInput(ItemStack input) {
        Item i = input.getItem();
        return (i == Items.POTION || i == Items.SPLASH_POTION || i == Items.LINGERING_POTION) && PotionUtils.getPotion(input) == from.get();
    }

    @Override
    public boolean isIngredient(ItemStack stack) {
        return ingredient != Items.AIR && stack.is(ingredient);
    }

    @Override
    public ItemStack getOutput(ItemStack input, ItemStack stack) {
        if (!isInput(input) || !isIngredient(stack)) return ItemStack.EMPTY;
        return PotionUtils.setPotion(new ItemStack(input.getItem()), to.get());
    }
}
