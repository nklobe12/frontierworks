package com.lodestar.datastore;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/** A data cable: joins devices, costs nothing and does not tick. */
public class CableBE extends DataNodeBE {
    public CableBE(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public int upkeep() {
        return 0;
    }

    @Override
    protected boolean isTicking() {
        return false;
    }
}
