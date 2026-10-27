package com.lodestar.dynamos;

import com.lodestar.core.block.LSEntityBlock;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.function.Supplier;

/** A dynamo's coil points into the block it was placed against, so it powers whatever it sits on. */
public class DynamoBlock extends LSEntityBlock {
    public DynamoBlock(Properties props, Supplier<? extends BlockEntityType<?>> type) {
        super(props, type, Facing.ALL, true);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState().setValue(FACING_ALL, ctx.getClickedFace().getOpposite());
    }
}
