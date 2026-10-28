package com.lodestar.kinetics;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A crown of bevel gears in a timber case: a shaft on every face. Every shaft turns the same way seen from outside the
 * case, so shafts leaving opposite faces counter-rotate, and rotation can turn any corner.
 */
public class BevelJunctionBE extends KineticBE {
    public BevelJunctionBE(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public double faceFactor(Direction d) {
        return d.getAxisDirection().getStep();
    }
}
