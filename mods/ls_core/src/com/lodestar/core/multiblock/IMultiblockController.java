package com.lodestar.core.multiblock;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;

import javax.annotation.Nullable;

/** A multiblock controller block entity. Port blocks delegate their capabilities here. */
public interface IMultiblockController {
    boolean isFormed();

    /** Capability for a port block at portPos accessed from side; empty when not offered. */
    <T> LazyOptional<T> getPortCapability(Capability<T> cap, BlockPos portPos, @Nullable Direction side);
}
