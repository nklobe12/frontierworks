package com.frontierworks.depths;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class AbyssalLensItem extends Item {
    public AbyssalLensItem(Properties props) {
        super(props);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }
}
