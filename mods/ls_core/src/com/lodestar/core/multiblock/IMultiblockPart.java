package com.lodestar.core.multiblock;

import net.minecraft.core.BlockPos;

import javax.annotation.Nullable;

/** Block entities that belong to a multiblock learn their controller when it forms (null when it breaks). */
public interface IMultiblockPart {
    void setController(@Nullable BlockPos controller);

    @Nullable
    BlockPos getController();
}
