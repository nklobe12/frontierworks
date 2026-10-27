package com.frontierworks.parts;

import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** Reusable crafting tool (hammer, wire cutters, mortar): survives crafting, losing one durability. */
public class CraftingToolItem extends Item {
    private static final RandomSource RANDOM = RandomSource.create();

    public CraftingToolItem(Properties props, int durability) {
        super(props.durability(durability));
    }

    @Override
    public boolean hasCraftingRemainingItem(ItemStack stack) {
        return true;
    }

    @Override
    public ItemStack getCraftingRemainingItem(ItemStack stack) {
        ItemStack copy = stack.copy();
        if (copy.hurt(1, RANDOM, null)) {
            return ItemStack.EMPTY;
        }
        return copy;
    }

    @Override
    public boolean isEnchantable(ItemStack stack) {
        return false;
    }
}
