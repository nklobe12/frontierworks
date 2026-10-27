package com.lodestar.core.energy;

import net.minecraft.world.item.ItemStack;

/** Implemented by items that store Forge Energy in their NBT (see {@link ItemEnergy}). */
public interface IEnergyItem {
    int getEnergyCapacity(ItemStack stack);

    int getEnergyTransfer(ItemStack stack);

    default boolean canExtractEnergy(ItemStack stack) {
        return false;
    }
}
