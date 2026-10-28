package com.lodestar.kinetics;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A spur gear on a short axle: passes rotation along its axis like an axle and meshes with spur gears beside it on
 * the same axis, which turn the other way.
 */
public class SpurGearBE extends KineticBE {
    public SpurGearBE(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public double faceFactor(Direction d) {
        return d.getAxis() == axis() ? 1 : 0;
    }

    @Override
    public double meshFactor(Direction d, KineticBE other) {
        return other instanceof SpurGearBE g && g.axis() == axis() && d.getAxis() != axis() ? -1 : 0;
    }

    /** Half a tooth of offset on alternate positions so neighbouring gears interlock on screen. */
    public float toothOffset() {
        BlockPos p = worldPosition;
        int parity = switch (axis()) {
            case X -> p.getY() + p.getZ();
            case Y -> p.getX() + p.getZ();
            case Z -> p.getX() + p.getY();
        };
        return (parity & 1) == 0 ? 0f : 22.5f;
    }
}
