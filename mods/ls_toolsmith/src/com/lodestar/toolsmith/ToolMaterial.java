package com.lodestar.toolsmith;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

import javax.annotation.Nullable;

/**
 * A tool material. Heads give durability, speed, attack and harvest tier; handles multiply durability; bindings
 * add durability. Every part made of a material carries its trait. Materials come from items in the part
 * builder, or from molten metal poured over casts.
 */
public record ToolMaterial(String id, int color, int tier, int headDurability, float headSpeed, float headAttack, float handleMultiplier,
                           int bindingDurability, String trait, String source, String fluid) {

    public boolean castable() {
        return !fluid.isEmpty();
    }

    public boolean craftable() {
        return !source.isEmpty();
    }

    public MutableComponent displayName() {
        return Component.translatable("material.ls_toolsmith." + id);
    }

    /** The item that supplies this material in the part builder and for repairs (null for casting-only metals). */
    @Nullable
    public Ingredient ingredient() {
        if (source.isEmpty()) return null;
        if (source.startsWith("#")) return Ingredient.of(ItemTags.create(new ResourceLocation(source.substring(1))));
        Item it = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(new ResourceLocation(source));
        return Ingredient.of(it);
    }

    /** Ingots of a castable metal also repair tools made from it. */
    @Nullable
    public Ingredient repairIngredient() {
        if (!source.isEmpty()) return ingredient();
        if (id.equals("netherite")) return Ingredient.of(net.minecraft.world.item.Items.NETHERITE_INGOT);
        return Ingredient.of(ItemTags.create(new ResourceLocation("forge", "ingots/" + id)));
    }

    public static int rgb(String hex) {
        return Integer.parseInt(hex.startsWith("#") ? hex.substring(1) : hex, 16);
    }

    public boolean matches(ItemStack stack) {
        Ingredient ing = repairIngredient();
        return ing != null && ing.test(stack);
    }
}
