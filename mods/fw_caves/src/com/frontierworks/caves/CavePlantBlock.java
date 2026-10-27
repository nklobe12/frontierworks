package com.frontierworks.caves;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** A small plant that grows on any solid floor (stone included), in the dark. */
public class CavePlantBlock extends BushBlock {
    private static final VoxelShape SHAPE = BushBlock.box(3, 0, 3, 13, 10, 13);

    public CavePlantBlock(Properties props) {
        super(props);
    }

    @Override
    protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
        return state.isFaceSturdy(level, pos, Direction.UP);
    }

    @Override
    @SuppressWarnings("deprecation")
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        net.minecraft.world.phys.Vec3 off = state.getOffset(level, pos);
        return SHAPE.move(off.x, off.y, off.z);
    }
}
