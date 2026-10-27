package com.lodestar.core.item;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Block;

import javax.annotation.Nullable;

/** Item with a furnace burn time (coal coke, sawdust briquettes, ...). */
public class FuelItem extends Item {
    private final int burnTime;

    public FuelItem(Properties props, int burnTime) {
        super(props);
        this.burnTime = burnTime;
    }

    @Override
    public int getBurnTime(ItemStack stack, @Nullable RecipeType<?> recipeType) {
        return burnTime;
    }

    /** Block item with a burn time (coke blocks etc.). */
    public static class Block_ extends BlockItem {
        private final int burnTime;

        public Block_(Block block, Properties props, int burnTime) {
            super(block, props);
            this.burnTime = burnTime;
        }

        @Override
        public int getBurnTime(ItemStack stack, @Nullable RecipeType<?> recipeType) {
            return burnTime;
        }
    }
}
