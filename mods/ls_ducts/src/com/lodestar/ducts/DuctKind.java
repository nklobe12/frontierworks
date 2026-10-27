package com.lodestar.ducts;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;

/** What a duct carries. */
public enum DuctKind {
    ENERGY, FLUID, ITEM;

    public Capability<?> capability() {
        return switch (this) {
            case ENERGY -> ForgeCapabilities.ENERGY;
            case FLUID -> ForgeCapabilities.FLUID_HANDLER;
            case ITEM -> ForgeCapabilities.ITEM_HANDLER;
        };
    }

    /** Whether a non-duct block entity offers this kind of handler on the given face. */
    public boolean accepts(BlockEntity be, Direction face) {
        return be != null && !(be instanceof DuctBE) && be.getCapability(capability(), face).isPresent();
    }
}
