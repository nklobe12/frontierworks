package com.frontierworks.relics;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class RelicItem extends Item {
    public enum Kind { VIGOR, HASTE, SWIFT, FEATHER, EMBER, TIDAL, OWL, PHOENIX, LUCK, THORNS, REGEN, WARDING }

    public final Kind kind;

    public RelicItem(Properties props, Kind kind) {
        super(props);
        this.kind = kind;
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return kind == Kind.PHOENIX || kind == Kind.REGEN;
    }
}
